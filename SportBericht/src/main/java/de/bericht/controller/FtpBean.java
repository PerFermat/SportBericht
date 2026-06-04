package de.bericht.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import de.bericht.provider.KiProvider;
import de.bericht.provider.KiProviderFactory;
import de.bericht.service.DatabaseService;
import de.bericht.service.EmailService;
import de.bericht.service.FtpManuellerTagEintrag;
import de.bericht.service.FtpTerminEintrag;
import de.bericht.service.HallenPdfParser;
import de.bericht.service.Heimspiele;
import de.bericht.service.ParserAusgabeFormatter;
import de.bericht.service.TerminMitStatus;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.LoginCookieDaten;
import de.bericht.util.enums.FtpUploadThema;
import de.bericht.util.enums.TerminStatus;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

@Named
@ViewScoped
public class FtpBean implements Serializable {

	private enum TerminHtmlZweck {
		MAIL, HTML_DATEI
	}

	private record SftpZiel(String host, String user, int port, String passwort, String zielPfad, String backupPfad) {
	}

	private static final Set<String> ICS_TERMIN_STATUS = Set.of(TerminStatus.TRAININGSAUSFALLJA.getLabel(),
			TerminStatus.TRAININGSAUSFALLJ.getLabel(), TerminStatus.TRAININGSAUSFALLA.getLabel(),
			TerminStatus.TRAINING.getLabel(), TerminStatus.TERMINNEU.getLabel());

	private static final long serialVersionUID = 1L;
	private static final int SFTP_CONNECT_TIMEOUT_MS = 15000;
	private Boolean freigabe;
	private boolean adminFreigabe;
	private boolean userFreigabe;
	private String ausgabeText;
	private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static final Pattern MONATS_PATTERN = Pattern.compile(
			"\\b(januar|februar|märz|maerz|april|mai|juni|juli|august|september|oktober|november|dezember)\\b",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private String kiAusgabeText;
	private byte[] originalPdfBytes;
	private String emailFreitext;
	private List<FtpTerminEintrag> terminEintraege = new ArrayList<>();
	private List<FtpManuellerTagEintrag> manuelleTage = new ArrayList<>();
	private List<Heimspiele> spieleDaheim = new ArrayList<>();
	private List<String> heim;
	private List<TerminMitStatus> parserTerminStatusListe = new ArrayList<>();
	private List<String> ueberschrift = new ArrayList<>();
	private final DatabaseService dbService = new DatabaseService();
	private HallenPdfParser pdfParcer;
	private String vereinnr;
	private String passwort;
	private String ruecksprung;
	private String ausgewaehltesThema = FtpUploadThema.HALLENBELEGUNGN.key;
	private Part uploadedDatei;
	private byte[] ausgewaehlteDateiBytes;
	private String ausgewaehlterDateiname;
	private YearMonth pdfMonat;
	private boolean analyseDurchgefuehrt;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null) {
			vereinnr = request.getParameter("vereinnr");
		}
		ruecksprung = request.getParameter("ruecksprung");
		passwort = request.getParameter("p");
		lesenCookieParameter();
		String adminPasswort = ConfigManager.getAdminPasswort(vereinnr);
		String userPasswort = ConfigManager.getUserPasswort(vereinnr);
		adminFreigabe = passwort != null && passwort.equals(adminPasswort);
		userFreigabe = passwort != null && passwort.equals(userPasswort);
		this.freigabe = adminFreigabe || userFreigabe;
		if (!freigabe) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Falsches Passwort eingegeben",
					"Bitte User- oder Admin-Passwort verwenden");
		}

	}

	private void lesenCookieParameter() {
		LoginCookieDaten logging = new LoginCookieDaten();
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = logging.getVereinnr();
			if (passwort == null) {
				passwort = logging.getPasswort();
			}
		} else {
			if (passwort == null && vereinnr.equals(logging.getVereinnr())) {
				passwort = logging.getPasswort();
			}
		}
	}

	public boolean isHallenThemaAusgewaehlt() {
		return FtpUploadThema.fromKey(ausgewaehltesThema).isHalleParcer();
	}

	public boolean isAdminFreigabe() {
		return adminFreigabe;
	}

	public void termineEintragen() {
		if (!adminFreigabe) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Keine Berechtigung",
					"Das Eintragen der Termine per FTP ist nur mit Admin-Passwort möglich.");
			return;
		}
		if (!isHallenThemaAusgewaehlt() || !isHallenAnalyseBereit()) {
			addMessage(FacesMessage.SEVERITY_WARN, "Analyse fehlt", "Bitte zuerst eine PDF auswählen und analysieren.");
			return;
		}

		byte[] icsBytes = erstelleTerminIcsBytes();
		if (icsBytes.length == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine Termine ausgewählt",
					"Bitte mindestens einen passenden Terminstatus für den Kalendereintrag auswählen.");
			return;
		}

		SftpZiel ziel;
		try {
			ziel = ermittleSftpZiel("ics");
		} catch (IllegalArgumentException e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "SFTP-Konfiguration unvollständig", e.getMessage());
			return;
		}

		Session session = null;
		ChannelSftp sftp = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(ziel.user(), ziel.host(), ziel.port());
			session.setPassword(ziel.passwort());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(SFTP_CONNECT_TIMEOUT_MS);

			sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect(SFTP_CONNECT_TIMEOUT_MS);
			uploadMitBackup(sftp, icsBytes, ziel.zielPfad(), ziel.backupPfad());
			addMessage(FacesMessage.SEVERITY_INFO, "Termine eingetragen",
					"ICS-Datei wurde per SFTP übertragen: " + ziel.zielPfad());
		} catch (JSchException | SftpException | IOException e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Termine eintragen fehlgeschlagen", e.getMessage());
		} finally {
			if (sftp != null && sftp.isConnected()) {
				sftp.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	public void hochladen() {
		if (!adminFreigabe) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Keine Berechtigung",
					"Upload per FTP ist nur mit Admin-Passwort möglich.");

			return;
		}
		if (ausgewaehlteDateiBytes == null || ausgewaehlteDateiBytes.length == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine Datei ausgewählt", "Bitte zuerst eine Datei auswählen.");
			return;
		}
		if (isHallenThemaAusgewaehlt() && !isHallenAnalyseBereit()) {
			addMessage(FacesMessage.SEVERITY_WARN, "Analyse fehlt", "Bitte zuerst eine PDF auswählen und analysieren.");
			return;
		}

		if (vereinnr == null || vereinnr.isBlank()) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Fehlende Vereinsnummer",
					"Die Vereinsnummer fehlt. Bitte Seite über den regulären Einstieg öffnen.");
			return;
		}

		String host = ConfigManager.getSftpUrl(vereinnr);
		String user = ConfigManager.getSftpUser(vereinnr);
		String portRaw = ConfigManager.getSftpPort(vereinnr);
		String passwort = ConfigManager.getSftpPasswort(vereinnr);

		if (isBlank(host) || isBlank(user) || isBlank(portRaw) || isBlank(passwort)) {
			addMessage(FacesMessage.SEVERITY_ERROR, "SFTP-Konfiguration unvollständig",
					"Bitte sftp.url, sftp.user, sftp.port und sftp.passwort in der Config prüfen.");
			return;
		}

		int port;
		try {
			port = Integer.parseInt(portRaw.trim());
		} catch (NumberFormatException e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Ungültiger SFTP-Port", "sftp.port ist keine Zahl: " + portRaw);
			return;
		}

		FtpUploadThema thema = FtpUploadThema.fromKey(ausgewaehltesThema);
		String originalDateiname = sanitizeDateiname(ausgewaehlterDateiname);
		String zielDateiname = thema.neuerDateiname != null ? thema.neuerDateiname : originalDateiname;
		String zielVerzeichnis = ConfigManager.getConfigValue(vereinnr, "sftp.verzeichnis.pdf") + thema.verzeichnis;
		String zielPfad = buildRemotePath(zielVerzeichnis, zielDateiname);
		String backupPfad = thema.alterDateiname != null ? buildRemotePath(zielVerzeichnis, thema.alterDateiname)
				: null;
		String zielHtmlDateiname = ersetzePdfEndungDurchHtml(zielDateiname);
		String zielHtmlPfad = buildRemotePath(zielVerzeichnis, zielHtmlDateiname);
		String backupHtmlPfad = thema.alterDateiname != null
				? buildRemotePath(zielVerzeichnis, ersetzePdfEndungDurchHtml(thema.alterDateiname))

				: null;

		Session session = null;
		ChannelSftp sftp = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(user, host, port);
			session.setPassword(passwort);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(SFTP_CONNECT_TIMEOUT_MS);

			sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect(SFTP_CONNECT_TIMEOUT_MS);

			byte[] uploadedPdf = ausgewaehlteDateiBytes;
			uploadMitBackup(sftp, uploadedPdf, zielPfad, backupPfad);

			if (thema.isHalleParcer() && originalPdfBytes != null && originalPdfBytes.length > 0 && pdfParcer != null) {
				byte[] htmlBytes = erstelleTerminHtml(TerminHtmlZweck.HTML_DATEI).getBytes(StandardCharsets.UTF_8);
				uploadMitBackup(sftp, htmlBytes, zielHtmlPfad, backupHtmlPfad);
			}

			addMessage(FacesMessage.SEVERITY_INFO, "Upload erfolgreich",
					"Datei wurde per SFTP übertragen: " + zielPfad);
		} catch (JSchException | SftpException | IOException e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Upload fehlgeschlagen", e.getMessage());
		} finally {
			if (sftp != null && sftp.isConnected()) {
				sftp.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}

	}

	public void analysierePdf() {
		if (!freigabe) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Keine Berechtigung",
					"Analyse ist nur mit User- oder Admin-Passwort möglich.");
			return;
		}
		if (ausgewaehlteDateiBytes == null || ausgewaehlteDateiBytes.length == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine Datei ausgewählt", "Bitte zuerst eine Datei auswählen.");
			return;
		}
		analyseDurchgefuehrt = false;
		FtpUploadThema thema = FtpUploadThema.fromKey(ausgewaehltesThema);
		if (!thema.isHalleParcer()) {
			addMessage(FacesMessage.SEVERITY_WARN, "Analyse nicht verfügbar",
					"Analyse ist nur für Hallen-PDF vorgesehen.");
			return;
		}

		try {
			byte[] uploadedPdf = ausgewaehlteDateiBytes;
			pdfMonat = ermittlePdfMonat(uploadedPdf);
			if (pdfMonat == null) {
				addMessage(FacesMessage.SEVERITY_WARN, "Analyse nicht möglich",
						"Analyse ist nur für Hallen-PDF vorgesehen.");
				return;
			}

			pdfParcer = new HallenPdfParser(vereinnr, new java.io.ByteArrayInputStream(uploadedPdf), pdfMonat);
			ueberschrift = pdfParcer.getUeberschrift();
			ausgabeText = pdfParcer.getHtmlText();
			heim = ladeHeimspiele(pdfMonat);

			terminEintraege = extrahiereTermine(ausgabeText);
			parserTerminStatusListe = new ArrayList<>(pdfParcer.getParserBloecke().stream()
					.map(block -> new TerminMitStatus(block.getTag(),
							ParserAusgabeFormatter.formatBlock(block.getTag() + " " + block.getTitel() + " [status] ",
									block.getText(), block.getWochentag()),
							block.getWochentag(),
							ParserAusgabeFormatter.statusVarianten(
									block.getTag() + " " + block.getTitel() + " [status] ", block.getText(),
									block.getWochentag())))
					.toList());
			manuelleTage = new ArrayList<>();
			emailFreitext = "";

			originalPdfBytes = uploadedPdf;

			for (int i = 0; i < 31; i++) {
				String spiele = "";
				Heimspiele heim = null;
				for (Heimspiele b : spieleDaheim) {
					if (i == b.getTag()) {
						spiele = spiele + b.getLiga() + ": Heimspiel gegen " + " " + b.getGast() + " um " + b.getZeit()
								+ " \n";
						heim = b;
					}
				}

				if (!spiele.isBlank()) {
					TerminMitStatus termin = new TerminMitStatus(heim, spiele, pdfParcer.getParserAlle());
					for (int j = 0; j < parserTerminStatusListe.size(); j++) {
						TerminMitStatus b = parserTerminStatusListe.get(j);
						if (b.getTag() == termin.getTag()) {
							parserTerminStatusListe.remove(j);
						}
					}
					parserTerminStatusListe.add(termin);
				}
			}
			ladeGespeicherteHallenbelegung();
			Collections.sort(parserTerminStatusListe);
			analyseDurchgefuehrt = true;
			addMessage(FacesMessage.SEVERITY_INFO, "Analyse erfolgreich", "PDF wurde analysiert.");
		} catch (IOException e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Analyse fehlgeschlagen", e.getMessage());
		}
	}

	public void dateiAusgewaehlt(FileUploadEvent event) {
		UploadedFile file = event.getFile();
		if (file == null || file.getContent() == null || file.getContent().length == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine Datei ausgewählt", "Bitte zuerst eine Datei auswählen.");
			return;
		}
		ausgewaehlteDateiBytes = file.getContent();
		ausgewaehlterDateiname = file.getFileName();
		uploadedDatei = null;
		originalPdfBytes = ausgewaehlteDateiBytes;
		pdfParcer = null;
		pdfMonat = null;
		analyseDurchgefuehrt = false;
		parserTerminStatusListe.clear();
		manuelleTage.clear();

	}

	public void kiAnalyse() {
		if (!isHallenAnalyseBereit()) {
			addMessage(FacesMessage.SEVERITY_WARN, "Analyse fehlt", "Bitte zuerst eine PDF auswählen und analysieren.");
			return;
		}

		kiAusgabeText = erstelleKiAnalyse(pdfParcer.getPlainText(), originalPdfBytes);
	}

	public boolean isDateiAusgewaehlt() {
		return ausgewaehlteDateiBytes != null && ausgewaehlteDateiBytes.length > 0;
	}

	public boolean isHallenAnalyseBereit() {
		return isDateiAusgewaehlt() && analyseDurchgefuehrt && pdfMonat != null && pdfParcer != null;
	}

	public void speichereHallenbelegung(TerminMitStatus termin) {
		LocalDate datum = datumFuerTermin(termin);
		if (datum == null) {
			return;
		}
		System.out.println("Speichern " + termin.getStatus() + " - " + termin.getTerminFreitext());
		dbService.speichereHallenbelegung(datum, termin.getStatus(), termin.getTerminFreitext());
	}

	private void ladeGespeicherteHallenbelegung() {
		Map<LocalDate, String> gespeicherteEintraege = dbService.ladeHallenbelegung(pdfMonat);
		if (gespeicherteEintraege.isEmpty()) {
			return;
		}

		List<LocalDate> hinzugefuegteDaten = new ArrayList<>();
		for (TerminMitStatus termin : parserTerminStatusListe) {
			LocalDate datum = datumFuerTermin(termin);
			if (gespeicherteEintraege.get(datum) != null) {
				String[] gespeicherterStatus = gespeicherteEintraege.get(datum).split("###");
				termin.setStatus(gespeicherterStatus[0]);
				if (gespeicherterStatus.length > 1) {
					System.out.println(gespeicherterStatus[1]);
					termin.setTerminFreitext(gespeicherterStatus[1]);
				}
				hinzugefuegteDaten.add(datum);
			}
		}

		for (Map.Entry<LocalDate, String> eintrag : gespeicherteEintraege.entrySet()) {
			if (hinzugefuegteDaten.contains(eintrag.getKey())) {
				continue;
			}
			FtpManuellerTagEintrag manuellerTag = new FtpManuellerTagEintrag();
			manuellerTag.setTag(tagLabelFuerDatum(eintrag.getKey()));
			manuellerTag.setText("Manuell gespeicherter Kalendereintrag");
			TerminMitStatus termin = new TerminMitStatus(vereinnr, pdfMonat, manuellerTag, pdfParcer.getParserAlle());
			termin.setStatus(eintrag.getValue());
			parserTerminStatusListe.add(termin);
		}
	}

	private LocalDate datumFuerTermin(TerminMitStatus termin) {
		if (termin == null || pdfMonat == null || termin.getTag() < 1 || termin.getTag() > pdfMonat.lengthOfMonth()) {
			return null;
		}
		return pdfMonat.atDay(termin.getTag());
	}

	private String tagLabelFuerDatum(LocalDate datum) {
		if (datum == null) {
			return "";
		}
		String tagPrefix = datum.getDayOfMonth() + ".";
		for (String eintrag : ueberschrift) {
			if (eintrag != null && eintrag.trim().startsWith(tagPrefix)) {
				return eintrag;
			}
		}
		return tagPrefix;
	}

	private String erstelleKiAnalyse(String pdfText, byte[] uploadedPdf) {

		if (ConfigManager.getConfigValue(vereinnr, "ki.model.hallenbelegung").isBlank()) {
			return "";
		}
		String prompt = ConfigManager.getConfigValue(vereinnr, "ki.prompt.hallenbelegung");

		String heimspieleText;

		if (heim.isEmpty()) {
			heimspieleText = "- Keine Heimspiele in dem Zeitraum";
		} else {
			heimspieleText = heim.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
		}

		prompt = prompt.replace("<Heimspiele>", heimspieleText);
		prompt = prompt.replace("<JSON>", bauePdfTabelle(pdfText));

		try {
			KiProvider ki = KiProviderFactory.create(vereinnr, prompt,
					ConfigManager.getConfigValue(vereinnr, "ki.model.hallenbelegung"), "high", 0.2, 0.0, 0.0, null);
			return ki.getResponse();
		} catch (IOException e) {
			return "KI-Analyse konnte nicht erstellt werden: " + e.getMessage();
		}
	}

	private String bauePdfTabelle(String pdfText) {
		if (pdfText == null || pdfText.isBlank()) {
			return new JSONObject().put("error", true).put("message", "Keine PDF-Daten verfügbar.").toString();
		}

		Pattern tagStart = Pattern.compile("^(\\d{1,2}\\.)\\s+(Mo|Di|Mi|Do|Fr|Sa|So)\\b.*");
		String[] zeilen = pdfText.split("\\R");
		List<String[]> eintraege = new ArrayList<>();

		String aktuellerTag = "";
		String aktuellerWochentag = "";
		StringBuilder block = new StringBuilder();

		for (String raw : zeilen) {
			String zeile = raw == null ? "" : raw.trim();
			Matcher matcher = tagStart.matcher(zeile);
			if (matcher.matches()) {
				if (!aktuellerTag.isBlank()) {
					eintraege.add(new String[] { aktuellerTag, aktuellerWochentag, block.toString().trim() });
				}
				aktuellerTag = matcher.group(1);
				aktuellerWochentag = matcher.group(2);
				block = new StringBuilder(zeile);
			} else if (!aktuellerTag.isBlank()) {
				if (!zeile.isBlank()) {
					block.append(" ").append(zeile);
				}
			}
		}
		if (!aktuellerTag.isBlank()) {
			eintraege.add(new String[] { aktuellerTag, aktuellerWochentag, block.toString().trim() });
		}

		// JSON-Struktur aufbauen
		JSONObject result = new JSONObject();
		JSONArray tageArray = new JSONArray();

		for (String[] e : eintraege) {
			JSONObject tagObject = new JSONObject();
			tagObject.put("tag", e[0]);
			tagObject.put("wochentag", e[1]);
			tagObject.put("text", e[2]);
			tageArray.put(tagObject);
		}

		result.put("anzahlTage", eintraege.size());
		result.put("eintraege", tageArray);

		return result.toString(2); // Einrückung mit 2 Leerzeichen für bessere Lesbarkeit
	}

	private String escapePipe(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("|", "\\|").replace("\n", " ");
	}

	public YearMonth ermittlePdfMonat(byte[] uploadedPdf) {
		try (PDDocument document = PDDocument.load(uploadedPdf)) {
			if (document.getNumberOfPages() <= 0) {
				return null;
			}
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(1);
			stripper.setEndPage(1);
			String ersteSeite = stripper.getText(document);
			return parseMonatAusText(ersteSeite);
		} catch (IOException e) {
			return null;
		}
	}

	private YearMonth parseMonatAusText(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		Matcher monatMatcher = MONATS_PATTERN.matcher(text);
		if (!monatMatcher.find()) {
			return null;
		}
		String monatRaw = monatMatcher.group(1).toLowerCase(Locale.GERMAN);
		Integer monat = mapMonat(monatRaw);
		if (monat == null) {
			return null;
		}
		Matcher jahrMatcher = Pattern.compile("\\b(20\\d{2})\\b").matcher(text);
		int jahr = LocalDate.now().getYear();
		if (jahrMatcher.find()) {
			jahr = Integer.parseInt(jahrMatcher.group(1));
		}

		return YearMonth.of(jahr, monat);
	}

	private Integer mapMonat(String monatRaw) {
		Map<String, Integer> monate = new HashMap<>();
		monate.put("januar", 1);
		monate.put("februar", 2);
		monate.put("märz", 3);
		monate.put("maerz", 3);
		monate.put("april", 4);
		monate.put("mai", 5);
		monate.put("juni", 6);
		monate.put("juli", 7);
		monate.put("august", 8);
		monate.put("september", 9);
		monate.put("oktober", 10);
		monate.put("november", 11);
		monate.put("dezember", 12);
		return monate.get(monatRaw);
	}

	private List<String> ladeHeimspiele(YearMonth pdfMonat) {
		spieleDaheim.clear();
		List<Map<String, String>> rows = dbService.ladeVerfuegbarkeitSpiele(vereinnr);
		List<String> heimspiele = new ArrayList<>();
		for (Map<String, String> row : rows) {
			String heim = row.getOrDefault("heim", "");
			if (heim == null || heim.isBlank()) {
				continue;
			}
			if (!heim.contains(ConfigManager.getSpielplanVerein(vereinnr))) {
				continue;
			}

			if (pdfMonat != null && !liegtImMonat(row.getOrDefault("datum", ""), pdfMonat)) {
				continue;
			}
			String datum = row.getOrDefault("datum", "");
			String zeit = row.getOrDefault("zeit", "");
			String liga = row.getOrDefault("liga", "");
			String gast = row.getOrDefault("gast", "");
			spieleDaheim.add(new Heimspiele(datum, zeit, liga, heim, gast));
			heimspiele.add(datum + " " + zeit + " | " + liga + " | " + heim + " - " + gast);
		}
		return heimspiele;
	}

	private boolean liegtImMonat(String datumText, YearMonth month) {
		if (datumText == null || datumText.isBlank()) {
			return false;
		}
		try {
			LocalDate datum = LocalDate.parse(datumText.trim(), DATUM_FORMAT);
			return YearMonth.from(datum).equals(month);
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	private SftpZiel ermittleSftpZiel(String dateiendung) {
		if (vereinnr == null || vereinnr.isBlank()) {
			throw new IllegalArgumentException(
					"Die Vereinsnummer fehlt. Bitte Seite über den regulären Einstieg öffnen.");
		}

		String host = ConfigManager.getSftpUrl(vereinnr);
		String user = ConfigManager.getSftpUser(vereinnr);
		String portRaw = ConfigManager.getSftpPort(vereinnr);
		String passwort = ConfigManager.getSftpPasswort(vereinnr);
		if (isBlank(host) || isBlank(user) || isBlank(portRaw) || isBlank(passwort)) {
			throw new IllegalArgumentException(
					"Bitte sftp.url, sftp.user, sftp.port und sftp.passwort in der Config prüfen.");
		}

		int port;
		try {
			port = Integer.parseInt(portRaw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("sftp.port ist keine Zahl: " + portRaw);
		}

		FtpUploadThema thema = FtpUploadThema.fromKey(ausgewaehltesThema);
		String originalDateiname = sanitizeDateiname(ausgewaehlterDateiname);
		String pdfDateiname = thema.neuerDateiname != null ? thema.neuerDateiname : originalDateiname;
		String zielDateiname = ersetzePdfEndung(pdfDateiname, dateiendung);
		String zielVerzeichnis = ConfigManager.getConfigValue(vereinnr, "sftp.verzeichnis.pdf") + thema.verzeichnis;
		String zielPfad = buildRemotePath(zielVerzeichnis, zielDateiname);
		String backupPfad = thema.alterDateiname != null
				? buildRemotePath(zielVerzeichnis, ersetzePdfEndung(thema.alterDateiname, dateiendung))
				: null;
		return new SftpZiel(host, user, port, passwort, zielPfad, backupPfad);
	}

	private byte[] erstelleTerminIcsBytes() {
		List<TerminMitStatus> termine = parserTerminStatusListe.stream().filter(this::istIcsTermin).toList();
		if (termine.isEmpty()) {
			return new byte[0];
		}

		StringBuilder ics = new StringBuilder();
		ics.append("BEGIN:VCALENDAR\r\n");
		ics.append("VERSION:2.0\r\n");
		ics.append("PRODID:-//SportBericht//Hallenbelegung//DE\r\n");
		ics.append("CALSCALE:GREGORIAN\r\n");
		ics.append("METHOD:PUBLISH\r\n");
		for (TerminMitStatus termin : termine) {
			LocalDate datum = datumFuerTermin(termin);
			if (datum == null) {
				continue;
			}
			String status = termin.getStatus();
			String titel = status;
			String beschreibung = termin.getTerminFreitext() == null ? "" : termin.getTerminFreitext().trim();
			if (TerminStatus.TERMINNEU.getLabel().equals(status)) {
				String[] zeilen = (termin.getTerminFreitext() == null ? "" : termin.getTerminFreitext()).split("\\R",
						-1);
				titel = zeilen.length > 0 && !zeilen[0].isBlank() ? zeilen[0].trim()
						: TerminStatus.TERMINNEU.getLabel();
				beschreibung = zeilen.length > 1
						? String.join("\n", Arrays.copyOfRange(zeilen, 1, zeilen.length)).trim()
						: "";
			}

			ics.append("BEGIN:VEVENT\r\n");
			appendIcsProperty(ics, "UID", UUID.randomUUID() + "@sportbericht");
			appendIcsProperty(ics, "DTSTAMP", DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
					.format(java.time.ZonedDateTime.now(ZoneOffset.UTC)));
			appendIcsProperty(ics, "SUMMARY", titel);
			appendIcsProperty(ics, "DESCRIPTION", beschreibung);
			appendIcsProperty(ics, "DTSTART;VALUE=DATE", datum.format(DateTimeFormatter.BASIC_ISO_DATE));
			appendIcsProperty(ics, "DTEND;VALUE=DATE", datum.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE));
			ics.append("END:VEVENT\r\n");
		}
		ics.append("END:VCALENDAR\r\n");
		return ics.toString().getBytes(StandardCharsets.UTF_8);
	}

	private boolean istIcsTermin(TerminMitStatus termin) {
		return termin != null && ICS_TERMIN_STATUS.contains(termin.getStatus()) && datumFuerTermin(termin) != null;
	}

	private void appendIcsProperty(StringBuilder ics, String name, String value) {
		String line = name + ":" + escapeIcsText(value);
		while (line.length() > 73) {
			ics.append(line, 0, 73).append("\r\n ");
			line = line.substring(73);
		}
		ics.append(line).append("\r\n");
	}

	private String escapeIcsText(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n")
				.replace("\n", "\\n").replace("\r", "\\n");
	}

	private boolean remoteExists(ChannelSftp sftp, String remotePath) {
		try {
			sftp.lstat(remotePath);
			return true;
		} catch (SftpException e) {
			return false;
		}
	}

	private void uploadMitBackup(ChannelSftp sftp, byte[] inhalt, String zielPfad, String backupPfad)
			throws IOException, SftpException {
		boolean bestehendeDateiGesichert = false;
		if (backupPfad != null && remoteExists(sftp, zielPfad)) {
			if (remoteExists(sftp, backupPfad)) {
				sftp.rm(backupPfad);
			}
			sftp.rename(zielPfad, backupPfad);
			bestehendeDateiGesichert = true;
		}

		try (InputStream inputStream = new java.io.ByteArrayInputStream(inhalt)) {
			sftp.put(inputStream, zielPfad, ChannelSftp.OVERWRITE);
		} catch (IOException | SftpException e) {
			if (bestehendeDateiGesichert && remoteExists(sftp, backupPfad) && !remoteExists(sftp, zielPfad)) {
				sftp.rename(backupPfad, zielPfad);
			}
			throw e;
		}
	}

	private String ersetzePdfEndungDurchHtml(String dateiname) {
		return ersetzePdfEndung(dateiname, "html");
	}

	private String ersetzePdfEndung(String dateiname, String neueEndung) {
		String endung = neueEndung.startsWith(".") ? neueEndung : "." + neueEndung;

		if (dateiname == null || dateiname.isBlank()) {
			return "upload" + endung;
		}
		if (dateiname.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			return dateiname.substring(0, dateiname.length() - 4) + endung;
		}
		return dateiname + endung;
	}

	private String buildRemotePath(String verzeichnis, String dateiname) {
		String basis = verzeichnis.endsWith("/") ? verzeichnis.substring(0, verzeichnis.length() - 1) : verzeichnis;
		return basis + "/" + dateiname;
	}

	private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String sanitizeDateiname(String dateiname) {
		if (dateiname == null || dateiname.isBlank()) {
			return "upload.bin";
		}
		String clean = dateiname.replace("\\", "/");
		int idx = clean.lastIndexOf('/');
		String result = idx >= 0 ? clean.substring(idx + 1) : clean;
		if (result.isBlank()) {
			return "upload.bin";
		}
		return result;
	}

	public List<SelectItem> getThemaOptionen() {
		return Arrays.stream(FtpUploadThema.values()).map(thema -> new SelectItem(thema.key, thema.label)).toList();
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getAusgewaehltesThema() {
		return ausgewaehltesThema;
	}

	public void setAusgewaehltesThema(String ausgewaehltesThema) {
		this.ausgewaehltesThema = ausgewaehltesThema;
	}

	public Part getUploadedDatei() {
		return uploadedDatei;
	}

	public void setUploadedDatei(Part uploadedDatei) {
		this.uploadedDatei = uploadedDatei;
	}

	public String getRuecksprung() {
		return ruecksprung;
	}

	public void setRuecksprung(String ruecksprung) {
		this.ruecksprung = ruecksprung;
	}

	public String ruecksprung() {
		return ruecksprung;
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public String getHtmlSeite() {
		return ConfigManager.getConfigValue(vereinnr, "sftp.seite.anzeige");
	}

	public String getAusgabeText() {
		return ausgabeText;
	}

	public void setAusgabeText(String ausgabeText) {
		this.ausgabeText = ausgabeText;
	}

	public String getKiAusgabeText() {
		return kiAusgabeText;
	}

	public String getOriginalPdfUrl() {
		if (originalPdfBytes == null || originalPdfBytes.length == 0) {
			return "";
		}
		String base64 = Base64.getEncoder().encodeToString(originalPdfBytes);
		return "data:application/pdf;base64," + base64;
	}

	public void neuerTag() {
		manuelleTage.add(new FtpManuellerTagEintrag());
	}

	public void sendeTerminMail() {
		if (!isHallenAnalyseBereit()) {
			addMessage(FacesMessage.SEVERITY_WARN, "Analyse fehlt", "Bitte zuerst eine PDF auswählen und analysieren.");
			return;
		}

		if (originalPdfBytes == null || originalPdfBytes.length == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine PDF verfügbar", "Bitte zuerst eine PDF hochladen.");
			return;
		}
		String empfaenger = ConfigManager.getMailEmpfaengerTermine(vereinnr);
		if (empfaenger == null || empfaenger.isBlank()) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Empfänger fehlt", "mail.termin.empfaenger ist nicht gepflegt.");
			return;
		}

		String inhalt = erstelleTerminHtml(TerminHtmlZweck.MAIL);
		try {
			new EmailService(vereinnr, empfaenger, null).sendEmail(vereinnr, "Terminabstimmung " + getVerein(), inhalt,
					originalPdfBytes, "Hallenbelegung.pdf", true);
			addMessage(FacesMessage.SEVERITY_INFO, "E-Mail versendet",
					"Termine wurden an " + empfaenger + " gesendet.");
		} catch (MessagingException ex) {
			addMessage(FacesMessage.SEVERITY_ERROR, "E-Mail fehlgeschlagen", ex.getMessage());
		}
	}

	private String erstelleTerminHtml(TerminHtmlZweck zweck) {
		String wrapperKlasse = zweck == TerminHtmlZweck.MAIL ? "mail-wrapper" : "mail-wrapper html-datei-wrapper";

		StringBuilder inhalt = new StringBuilder();
		inhalt.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'/>").append(
				"<style>body{font-family:Arial,sans-serif;font-size:14px;line-height:1.5;margin:0;padding:0}.mail-wrapper{max-width:600px;margin:0 auto;padding:0 12px}.mail-block{text-align:left}")
				.append(ParserAusgabeFormatter.css()).append("</style>");
		if (zweck == zweck.HTML_DATEI) {
			inhalt.append("<script>").append("function sendHeight() {")
					.append("    var height = document.documentElement.scrollHeight;")
					.append("    window.parent.postMessage(height, '*');").append("}")
					.append("window.onload = sendHeight;").append("window.onresize = sendHeight;")
					.append("var observer = new MutationObserver(sendHeight);")
					.append("observer.observe(document.body, { childList: true, subtree: true });").append("</script>");
		}
		inhalt.append("</head><body><div class='").append(wrapperKlasse).append("'>")
				.append("<div class='mail-block'>");
		if (zweck == zweck.MAIL) {
			inhalt.append("Hallo,<br/><br/>" + "ich habe das PDF der Hallennutzung vom " + getPdfMonat()
					+ " geprüft und die relevanten Termine bereits in unseren Kalender eingetragen.<br/>"
					+ "Unten findest du eine Zusammenfassung der Änderungen sowie die Ergebnisse der automatischen Auswertung zur Kontrolle.<br/><br/>");

			inhalt.append("<strong>Farblegende:</strong><br/>"
					+ "<span style='background-color:#ffebee;border-left:6px solid #c62828;padding:2px 6px;display:inline-block;'>Rot </span> = Halle gefunden -> evtl. Trainingsausfall<br/>"
					+ "<span style='background-color:#e8f5e9;border-left:6px solid #2e7d32;padding:2px 6px;display:inline-block;'>Grün</span> = Erwähnungen Tischtennis<br/>"
					+ "<span style='background-color:#e3f2fd;border-left:6px solid #1565c0;padding:2px 6px;display:inline-block;'>Blau</span> = Heimspiel<br/>"
					+ "<span style='background-color:#fff8e1;border-left:6px solid #ff8f00;padding:2px 6px;display:inline-block;'>Gelb</span> = Feiertag / Ferien<br/>"
					+ "<span style='background-color:#D1D1D1;border-left:6px solid #595959;padding:2px 6px;display:inline-block;'>Grau</span> = Manuell geprüft / eingetragen<br/><br/>");
		}

		if (!(emailFreitext == null || emailFreitext.isBlank())) {
			inhalt.append("<h2>Allgemeine Bemerkungen</h2>").append("<div class='hinweis'>");
			inhalt.append(escapeHtml(emailFreitext == null ? "" : emailFreitext.trim()).replace("\n", "<br/>"))
					.append("</div>");
		}
		inhalt.append("</div>");

		for (FtpManuellerTagEintrag p : manuelleTage) {
			TerminMitStatus termin = new TerminMitStatus(vereinnr, ermittlePdfMonat(originalPdfBytes), p,
					pdfParcer.getParserAlle());
			parserTerminStatusListe.add(termin);
			speichereHallenbelegung(termin);
		}
		Collections.sort(parserTerminStatusListe);
		manuelleTage.clear();

		inhalt.append("<div class='mail-block'>");

		if (zweck == zweck.MAIL) {
			inhalt.append("<h2>Hallenbelegung Trainingstage und Erwähnungen vom Tischtennis</h2>");
		}
		if (zweck == zweck.HTML_DATEI) {
			inhalt.append("<h2>" + getPdfMonat() + "</h2>");
			inhalt.append("Beachte die eingetragenen Termine in unserem Kalender</h2>");
		}

		for (TerminMitStatus p : parserTerminStatusListe) {
			if (!p.getStatus().equals("ignorieren")) {
				inhalt.append(p.getHtmlText(p.getStatus()));
			}
		}

		inhalt.append("</div>");
		if (zweck == zweck.MAIL) {
			if (!(kiAusgabeText == null || kiAusgabeText.isBlank())) {
				inhalt.append("<div class='mail-block'><h2>KI Auswertung der PDF-Daten</h2>");
				inhalt.append(kiAusgabeText);
				inhalt.append("</div>");
			}
		}
		inhalt.append("</div></body></html>");
		return inhalt.toString();
	}

	public void neuerTagHinzufuegen() {

		for (FtpManuellerTagEintrag p : manuelleTage) {
			TerminMitStatus termin = new TerminMitStatus(vereinnr, ermittlePdfMonat(originalPdfBytes), p,
					pdfParcer.getParserAlle());
			parserTerminStatusListe.add(termin);
		}
		Collections.sort(parserTerminStatusListe);
		manuelleTage.clear();
	}

	private List<FtpTerminEintrag> extrahiereTermine(String html) {
		List<FtpTerminEintrag> r = new ArrayList<>();
		if (html == null || html.isBlank()) {
			return r;
		}
		String p = html.replaceAll("(?s)<[^>]*>", " ").replaceAll("\\s+", " ").trim();
		Matcher m = Pattern.compile("\\b\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\b").matcher(p);
		while (m.find()) {
			int e = Math.min(p.length(), m.end() + 80);
			r.add(new FtpTerminEintrag(m.group(), p.substring(m.start(), e).trim()));
		}
		return r;
	}

	public String getEmailFreitext() {
		return emailFreitext;
	}

	public void setEmailFreitext(String emailFreitext) {
		this.emailFreitext = emailFreitext;
	}

	public List<FtpTerminEintrag> getTerminEintraege() {
		return terminEintraege;
	}

	public List<FtpManuellerTagEintrag> getManuelleTage() {
		return manuelleTage;
	}

	public List<TerminMitStatus> getParserTerminStatusListe() {
		return parserTerminStatusListe;
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public String getParserAusgabeCss() {
		return ParserAusgabeFormatter.css();
	}

	public List<Heimspiele> getSpieleDaheim() {
		return spieleDaheim;
	}

	public void setSpieleDaheim(List<Heimspiele> spieleDaheim) {
		this.spieleDaheim = spieleDaheim;
	}

	public List<String> getUeberschrift() {
		return ueberschrift;
	}

	public void setUeberschrift(List<String> ueberschrift) {
		this.ueberschrift = ueberschrift;
	}

	public String getAusgewaehlterDateiname() {
		return ausgewaehlterDateiname;
	}

	public void setAusgewaehlterDateiname(String ausgewaehlterDateiname) {
		this.ausgewaehlterDateiname = ausgewaehlterDateiname;
	}

	public String getPdfMonat() {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);
			String result = pdfMonat.format(formatter);
			return result.substring(0, 1).toUpperCase() + result.substring(1);
		} catch (Exception e) {
			return "";
		}
	}

	public void setPdfMonat(YearMonth pdfMonat) {
		this.pdfMonat = pdfMonat;
	}

}
