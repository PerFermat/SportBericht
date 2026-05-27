package de.bericht.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
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
import de.bericht.service.HallenPdfParser;
import de.bericht.service.Heimspiele;
import de.bericht.service.ParserAusgabeFormatter;
import de.bericht.service.TerminMitStatus;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.LoginCookieDaten;
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
	private List<TerminEintrag> terminEintraege = new ArrayList<>();
	private List<ManuellerTagEintrag> manuelleTage = new ArrayList<>();
	private List<Heimspiele> spieleDaheim = new ArrayList<>();
	private List<String> heim;
	private List<TerminMitStatus> parserTerminStatusListe = new ArrayList<>();
	private List<String> ueberschrift = new ArrayList<>();
	private final DatabaseService dbService = new DatabaseService();
	private HallenPdfParser pdfParcer;
	private static final List<SelectItem> TERMIN_STATUS_OPTIONEN = List.of(
			new SelectItem("Trainingsausfall eingetragen"), new SelectItem("Termin eingetragen"),
			new SelectItem("Nicht relevant"), new SelectItem("Überprüfe"), new SelectItem("Spieltag OK"));

	private enum UploadThema {
		HALLENBELEGUNGN("HALLENBELEGUNGN", "Hallenbelegung neuer Monat", "Hallenbelegung", "Hallenbelegung.pdf",
				"Hallenbelegungalt.pdf", true),
		HALLENBELEGUNGE("HALLENBELEGUNGE", "Hallenbelegung ersetzen aktuellen Monat", "Hallenbelegung",
				"Hallenbelegung.pdf", null, true),
		KINDERFOTOS("KINDERFOTOS", "Einverständniserklärung Kinderfotos", "Foto", null, null, false);

		private final String key;
		private final String label;
		private final String verzeichnis;
		private final String neuerDateiname;
		private final String alterDateiname;
		private final boolean halleParcer;

		UploadThema(String key, String label, String verzeichnis, String neuerDateiname, String alterDateiname,
				boolean halleParcer) {
			this.key = key;
			this.label = label;
			this.verzeichnis = verzeichnis;
			this.neuerDateiname = neuerDateiname;
			this.alterDateiname = alterDateiname;
			this.halleParcer = halleParcer;
		}

		public static UploadThema fromKey(String key) {
			return Arrays.stream(values()).filter(v -> v.key.equals(key)).findFirst().orElse(HALLENBELEGUNGN);
		}

		public boolean isHalleParcer() {
			return halleParcer;
		}
	}

	private String vereinnr;
	private String passwort;
	private String ruecksprung;
	private String ausgewaehltesThema = UploadThema.HALLENBELEGUNGN.key;
	private Part uploadedDatei;
	private byte[] ausgewaehlteDateiBytes;
	private String ausgewaehlterDateiname;

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
		return UploadThema.fromKey(ausgewaehltesThema).isHalleParcer();
	}

	public boolean isAdminFreigabe() {
		return adminFreigabe;
	}

	public void hochladen() {
		ueberschrift.clear();
		if (!adminFreigabe) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Keine Berechtigung",
					"Upload per FTP ist nur mit Admin-Passwort möglich.");

			return;
		}
		if (ausgewaehlteDateiBytes == null || ausgewaehlteDateiBytes.length == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine Datei ausgewählt", "Bitte zuerst eine Datei auswählen.");
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

		UploadThema thema = UploadThema.fromKey(ausgewaehltesThema);
		String originalDateiname = sanitizeDateiname(ausgewaehlterDateiname);
		String zielDateiname = thema.neuerDateiname != null ? thema.neuerDateiname : originalDateiname;
		String zielPfad = buildRemotePath(
				ConfigManager.getConfigValue(vereinnr, "sftp.verzeichnis.pdf") + thema.verzeichnis, zielDateiname);
		String backupPfad = thema.alterDateiname != null
				? buildRemotePath(ConfigManager.getConfigValue(vereinnr, "sftp.verzeichnis.pdf") + thema.verzeichnis,
						thema.alterDateiname)
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

			boolean bestehendeDateiGesichert = false;
			if (backupPfad != null && remoteExists(sftp, zielPfad)) {
				if (remoteExists(sftp, backupPfad)) {
					sftp.rm(backupPfad);
				}
				sftp.rename(zielPfad, backupPfad);
				bestehendeDateiGesichert = true;
			}

			byte[] uploadedPdf = ausgewaehlteDateiBytes;
			try (InputStream inputStream = new java.io.ByteArrayInputStream(uploadedPdf)) {
				sftp.put(inputStream, zielPfad, ChannelSftp.OVERWRITE);
			} catch (IOException | SftpException e) {
				if (bestehendeDateiGesichert && remoteExists(sftp, backupPfad) && !remoteExists(sftp, zielPfad)) {
					sftp.rename(backupPfad, zielPfad);
				}
				throw e;
			}

			try {
				if (thema.isHalleParcer()) {
					pdfParcer = new HallenPdfParser(new java.io.ByteArrayInputStream(uploadedPdf));
					ueberschrift = pdfParcer.getUeberschrift();
					ausgabeText = pdfParcer.getHtmlText();
					YearMonth pdfMonat = ermittlePdfMonat(uploadedPdf);
					heim = ladeHeimspiele(pdfMonat);

					terminEintraege = extrahiereTermine(ausgabeText);
					parserTerminStatusListe = new ArrayList<>(pdfParcer.getParserBloecke().stream()
							.map(block -> new TerminMitStatus(block.getTag(),
									ParserAusgabeFormatter.formatBlock(
											block.getTag() + " " + block.getTitel() + " [status] ", block.getText(),
											block.getWochentag()),
									block.getWochentag()))
							.toList());
					manuelleTage = new ArrayList<>();
					emailFreitext = "";

					originalPdfBytes = uploadedPdf;

					for (int i = 0; i < 31; i++) {
						String spiele = "";
						Heimspiele heim = null;
						for (Heimspiele b : spieleDaheim) {
							if (i == b.getTag()) {
								spiele = spiele + b.getLiga() + ": Heimspiel gegen " + " " + b.getGast() + " um "
										+ b.getZeit() + " \n";
								heim = b;
							}

						}

						if (!spiele.isBlank()) {
							TerminMitStatus termin = new TerminMitStatus(heim, spiele, pdfParcer.getParserAlle());
							parserTerminStatusListe.add(termin);
						}
					}
				} else {
					heim.clear();
					ausgabeText = "";
					kiAusgabeText = "";
					originalPdfBytes = null;
				}
				Collections.sort(parserTerminStatusListe);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		UploadThema thema = UploadThema.fromKey(ausgewaehltesThema);
		if (!thema.isHalleParcer()) {
			addMessage(FacesMessage.SEVERITY_WARN, "Analyse nicht verfügbar",
					"Analyse ist nur für Hallen-PDF vorgesehen.");
			return;
		}

		try {
			byte[] uploadedPdf = ausgewaehlteDateiBytes;
			pdfParcer = new HallenPdfParser(new java.io.ByteArrayInputStream(uploadedPdf));
			ueberschrift = pdfParcer.getUeberschrift();
			ausgabeText = pdfParcer.getHtmlText();
			YearMonth pdfMonat = ermittlePdfMonat(uploadedPdf);
			heim = ladeHeimspiele(pdfMonat);

			terminEintraege = extrahiereTermine(ausgabeText);
			parserTerminStatusListe = new ArrayList<>(pdfParcer.getParserBloecke().stream()
					.map(block -> new TerminMitStatus(block.getTag(),
							ParserAusgabeFormatter.formatBlock(block.getTag() + " " + block.getTitel() + " [status] ",
									block.getText(), block.getWochentag()),
							block.getWochentag()))
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
					parserTerminStatusListe.add(termin);
				}
			}
			Collections.sort(parserTerminStatusListe);
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
		addMessage(FacesMessage.SEVERITY_INFO, "Datei übernommen",
				"Datei wurde lokal gespeichert: " + sanitizeDateiname(ausgewaehlterDateiname));
	}

	public void kiAnalyse() {
		kiAusgabeText = erstelleKiAnalyse(pdfParcer.getPlainText(), originalPdfBytes);
	}

	private String erstelleKiAnalyse(String pdfText, byte[] uploadedPdf) {

		if (ConfigManager.getConfigValue(vereinnr, "sftp.ki.model").isBlank()) {
			return "";
		}
		String prompt = """
								Situation: Tischtennis (TT) trainiert montags und freitags von 18:00 bis 22:00 Uhr in der Halle.
								Zusätzlich gibt es manchmal Ligaspiele (Heimspiele) an anderen oder gleichen Tagen.

								Aufgaben:

				1) **Analyse der Hallenbelegungen / Schließungen (nur an Trainingstagen: Montag & Freitag)**
				   Prüfe folgende JSON-Daten auf Termine, an denen KEIN Tischtennistraining stattfinden kann.
				   Trainingstage in denen das Training stattfinden kann sollen nicht erwähnt werden
				   Bedingungen:
				   - WICHTIG UNBEDINGT BEACHTEN: Der Wochentag muss "Mo" oder "Fr" sein.
				   - Der Text muss **eine Nutzung oder Sperrung der Halle** hindeuten, unabhängig von der Uhrzeit.
				     Dazu gehören:
				     * explizite Schließung: "Halle geschlossen", "Halle / MZR geschlossen"
				     * Belegung durch Dritte: "Halle belegt", "Halle: ... Aufbau ab ...", "Halle: ... Ganztags"
				     * Jeder Satz, der mit "Halle" enthällt und eine Aktivität nennt, gilt als Hallenbelegung.
				   - Wichtig: Auch wenn die Halle "ganztags" belegt ist, zählt das als **kein Training möglich** (auch abends).
				   - Gib alle solche Termine aus – auch wenn der Text keine Uhrzeit explizit nennt.

								2) **Extraktion aller Termine, die Tischtennis erwähnen**
								   Suche in der JSON nach Erwähnungen von "Tischtennis" ODER der Abkürzung "TT"
								   Nur wenn "TT" oder "Tischtennis" im Text vorkommt, wird der Termin aufgenommen.
								   Falls nichts dergleichen im Text steht (z. B. nur andere Sportarten), nicht aufnehmen.

								3) **Prüfung der Heimspiele**
								   Für jedes angegebene Heimspiel: Prüfe, ob die Halle am betreffenden Datum und zur Spielzeit laut JSON frei ist.
								   - Falls die Halle an dem Tag belegt oder geschlossen ist → Hallenstatus "belegt / nicht frei".
								   - Falls die Halle frei ist (keine Erwähnung einer Belegung/Schließung im JSON) → "frei".
								   - Falls das Heimspiel an einem Trainingstag (Mo/Fr) außerhalb der Trainingszeit (18–22 Uhr) liegt, ist das okay, solange die Halle frei ist.

								JSON-Daten (Hallenbelegungen / Schließungen):
								<JSON>

								Heimspiele (zusätzlich zu prüfen):
								<Heimspiele>

								---

								**Ausgabeformat:**
								Erstelle eine gut lesbare HTML-Antwort mit folgenden Strukturen. Das html-Dokument darf nur die Formatierungen innerhalb des <body> Teils enthalten.
								Verwende <h2>, <h3>, <p>, <table>, <thead>, <tbody>, <tr>, <th>, <td>, <ul>, <li>.

								<h2>Analyse der Hallenbelegungen für Tischtennis</h2>

								<h3>1) Kein Training möglich (nur Montag & Freitag, Halle nicht frei)</h3>
								<table border="1">
								  <thead>
								    <tr><th>Datum</th><th>Wochentag</th><th>Grund</th><th>Quelle (Textauszug)</th></tr>
								  </thead>
								  <tbody>
								    <!-- Befüllen mit Terminen aus Aufgabe 1 -->
								  </tbody>
								</table>

								<h3>2) Tischtennis erwähnt (inkl. Abkürzung "TT")</h3>
								<table border="1">
								  <thead>
								    <tr><th>Datum</th><th>Wochentag</th><th>Uhrzeit (falls vorhanden, sonst "-")</th><th>Textauszug</th></tr>
								  </thead>
								  <tbody>
								    <!-- Befüllen mit Terminen aus Aufgabe 2 -->
								  </tbody>
								</table>

								<h3>3) Heimspiele – Hallenstatusprüfung</h3>
								<table border="1">
								  <thead>
								    <tr><th>Heimspiel</th><th>Hallenstatus</th><th>Begründung</th></tr>
								  </thead>
								  <tbody>
								    <!-- Befüllen mit Ergebnissen aus Aufgabe 3 -->
								  </tbody>
								</table>

								<h3>4) Kurze Zusammenfassung</h3>
								<p>Anzahl Trainingstage ohne mögliches Training: X</p>
								<p>Anzahl Tischtennis-Erwähnungen: Y</p>
								<p>Heimspiele: Z geprüft, davon W mit Hallenkonflikt.</p>
								<p>Falls etwas unklar war oder Daten fehlten: <em>explizit benennen</em>.</p>
								""";

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
					ConfigManager.getConfigValue(vereinnr, "sftp.ki.model"), "high", 0.2, 0.0, 0.0, null);
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

	private YearMonth ermittlePdfMonat(byte[] uploadedPdf) {
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

	private boolean remoteExists(ChannelSftp sftp, String remotePath) {
		try {
			sftp.lstat(remotePath);
			return true;
		} catch (SftpException e) {
			return false;
		}
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
		return Arrays.stream(UploadThema.values()).map(thema -> new SelectItem(thema.key, thema.label)).toList();
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
		manuelleTage.add(new ManuellerTagEintrag());
	}

	public void sendeTerminMail() {
		if (originalPdfBytes == null || originalPdfBytes.length == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine PDF verfügbar", "Bitte zuerst eine PDF hochladen.");
			return;
		}
		String empfaenger = ConfigManager.getMailEmpfaengerTermine(vereinnr);
		if (empfaenger == null || empfaenger.isBlank()) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Empfänger fehlt", "mail.termin.empfaenger ist nicht gepflegt.");
			return;
		}
		StringBuilder inhalt = new StringBuilder();
		inhalt.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'/>").append(
				"<style>body{font-family:Arial,sans-serif;font-size:14px;line-height:1.5;margin:0;padding:0}.mail-wrapper{max-width:600px;margin:0 auto;padding:0 12px}.mail-block{text-align:left}")
				.append(ParserAusgabeFormatter.css()).append("</style></head><body><div class='mail-wrapper'>")
				.append("<div class='mail-block'>Hallo,<br/><br/>"
						+ "ich habe die neue Monats-PDF zur Hallennutzung geprüft und die relevanten Termine bereits in unseren Kalender eingetragen.<br/>"
						+ "Unten findest du eine Zusammenfassung der Änderungen sowie die Ergebnisse der automatischen Auswertung zur Kontrolle.<br/><br/>"

						+ "<strong>Farblegende:</strong><br/>"
						+ "<span style='background-color:#e8f5e9;border-left:6px solid #2e7d32;padding:2px 6px;display:inline-block;'>Grün</span> = Tischtennis gefunden<br/>"
						+ "<span style='background-color:#ffebee;border-left:6px solid #c62828;padding:2px 6px;display:inline-block;'>Rot</span> = Halle evtl. besetzt<br/>"
						+ "<span style='background-color:#e3f2fd;border-left:6px solid #1565c0;padding:2px 6px;display:inline-block;'>Blau</span> = Heimspiel<br/>"
						+ "<span style='background-color:#D1D1D1;border-left:6px solid #595959;padding:2px 6px;display:inline-block;'>Grau</span> = Manuell geprüft / eingetragen<br/><br/>")
				.append("<h2>Allgemeine Bemerkungen</h2>").append("<div class='hinweis'>")
				.append(escapeHtml(emailFreitext == null ? "" : emailFreitext.trim()).replace("\n", "<br/>"))
				.append("</div></div>");

		for (ManuellerTagEintrag p : manuelleTage) {
			TerminMitStatus termin = new TerminMitStatus(p, pdfParcer.getParserAlle());
			parserTerminStatusListe.add(termin);
		}
		Collections.sort(parserTerminStatusListe);
		manuelleTage.clear();

		inhalt.append("<div class='mail-block'><h2>Hallenbelegung Trainingstage und Erwähnungen vom Tischtennis</h2>");
		for (TerminMitStatus p : parserTerminStatusListe) {
			inhalt.append(p.getHtmlText(p.getStatus()));
		}

		inhalt.append("</div><div class='mail-block'><h2>KI Auswertung der PDF-Daten</h2>");
		inhalt.append(kiAusgabeText);
		inhalt.append("</div></div></body></html>");
		try {
			new EmailService(vereinnr, empfaenger, null).sendEmail(vereinnr, "Terminabstimmung " + getVerein(),
					inhalt.toString(), originalPdfBytes, "Hallenbelegung.pdf", true);
			addMessage(FacesMessage.SEVERITY_INFO, "E-Mail versendet",
					"Termine wurden an " + empfaenger + " gesendet.");
		} catch (MessagingException ex) {
			addMessage(FacesMessage.SEVERITY_ERROR, "E-Mail fehlgeschlagen", ex.getMessage());
		}
	}

	private List<TerminEintrag> extrahiereTermine(String html) {
		List<TerminEintrag> r = new ArrayList<>();
		if (html == null || html.isBlank()) {
			return r;
		}
		String p = html.replaceAll("(?s)<[^>]*>", " ").replaceAll("\\s+", " ").trim();
		Matcher m = Pattern.compile("\\b\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\b").matcher(p);
		while (m.find()) {
			int e = Math.min(p.length(), m.end() + 80);
			r.add(new TerminEintrag(m.group(), p.substring(m.start(), e).trim()));
		}
		return r;
	}

	public static class TerminEintrag implements Serializable {
		private static final long serialVersionUID = 1L;
		private String status = "Überprüfe";
		private String datum;
		private String text;

		public TerminEintrag(String d, String t) {
			datum = d;
			text = t;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getDatum() {
			return datum;
		}

		public String getText() {
			return text;
		}
	}

	public static class ManuellerTagEintrag implements Serializable {
		private static final long serialVersionUID = 1L;
		private String tag;
		private String text;

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public String getEmailFreitext() {
		return emailFreitext;
	}

	public void setEmailFreitext(String emailFreitext) {
		this.emailFreitext = emailFreitext;
	}

	public List<TerminEintrag> getTerminEintraege() {
		return terminEintraege;
	}

	public List<SelectItem> getTerminStatusOptionen() {
		return TERMIN_STATUS_OPTIONEN;
	}

	public List<ManuellerTagEintrag> getManuelleTage() {
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

}
