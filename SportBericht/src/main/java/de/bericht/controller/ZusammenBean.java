package de.bericht.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import de.bericht.provider.SpielplanProvider;
import de.bericht.service.BerichtText;
import de.bericht.service.DatabaseService;
import de.bericht.service.LogEntry;
import de.bericht.service.Spiel;
import de.bericht.service.TischtennisSpiel;
import de.bericht.service.WordpressMedia;
import de.bericht.util.BerichtData;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.WordPressAPIClient;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("zusammenBean")
@ViewScoped
public class ZusammenBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private List<Spiel> spieleFreigegeben;
	private String username;
	private String password;

	private boolean bereitsFreigegeben = false;
	private String berichte;
	private String berichtDatum;
	private Date berichtDatumCal;
	ConfigManager config;
	private DatabaseService dbService = new DatabaseService();
	List<BerichtText> meineBerichtTexte = new ArrayList<>();
	private Spiel selectedItem;
	private String iframeUrl;
	private String name;
	private String vereinnr;
	private String freieBerichte;
	private String bildLink;
	private String berichtText;
	private byte[] bildDaten;
	private String bildUnterschrift;
	private String spielErgebnis = ""; // Für die Anzeige der Spielergebnisse
	private StreamedContent downloadBild;
	private String heim;
	private String gast;
	private String datum;
	private String ergebnis;
	private String liga;
	private String ergebnisLink;
	private String ergebnisLinkAnfang;
	private List<LogEntry> logEntries;
	private List<String> selectKategorie;
	private String resizedImage;
	private int anzahlWordpress;
	private String altLink;
	private boolean freigegebeneBerichte = true;
	private Boolean mitSpielberichte;
	private SpielplanProvider provider;
	private List<Spiel> spiele;
	private String gruppeUrl;

	private String freierText;

	public ZusammenBean() throws UnsupportedEncodingException {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		gruppeUrl = params.get("gruppeUrl");
		vereinnr = params.get("vereinnr");
		datum = params.get("datum");
		liga = params.get("liga");
		try {
			if (params.get("heim") != null) {
				this.heim = URLDecoder.decode(params.get("heim"), "UTF-8");
			}
			if (params.get("gast") != null) {
				this.gast = URLDecoder.decode(params.get("gast"), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			this.heim = "Unbekannt";
			this.gast = "";
		}
		ergebnis = params.get("ergebnis");
		ergebnisLink = params.get("ergebnisLink");
		ergebnisLinkAnfang = ergebnisLink;
		spieleFreigegeben = new ArrayList<>();

		berichtLaden();

		String mail = params.get("mail");
		name = BerichtHelper.getHomepageStandardEinzel(vereinnr);
		if (datum != null) {
			this.selectedItem = new TischtennisSpiel(vereinnr, "", "", datum, "", liga, heim, gast, ergebnis,
					ergebnisLink);
			ladeIframe(selectedItem);
		}
		freieBerichte = params.get("frei");

		spiele = dbService.listeBerichteMitSpielMetadaten(vereinnr);

		erstellenBerichtListe(freigegebeneBerichte);

		if ("Ja".equals(mail)) {
			bereitsFreigegeben = true;
		}
	}

	public void erstellenBerichtListe(boolean freigegeben) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");

		spieleFreigegeben.clear();
		for (Spiel spiel : spiele) {
			if (matchesLigaFilter(spiel) && matchesFreiFilter(spiel)
					&& (!freigegeben || hasFreigabe(spiel.getErgebnisLink()))) {

				spieleFreigegeben.add(spiel);
			}
		}

		spieleFreigegeben.sort((s1, s2) -> {
			try {
				Date d1 = formatter.parse(s1.getDatum());
				Date d2 = formatter.parse(s2.getDatum());
				return d1.compareTo(d2);
			} catch (ParseException e) {
				return 0;
			}
		});
	}

	private void berichtLaden() {
		this.spielErgebnis = "";
		// Bericht und Bild aus DB laden, falls vorhanden
		if (ergebnisLink != null && !ergebnisLink.isEmpty()) {
			BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);
			this.berichtText = BerichtHelper.SAFE_HTML_POLICY
					.sanitize(data.getBerichtText() != null ? data.getBerichtText() : "");
			if (data.getBild() != null) {
				// Speichere das geladene Bild in der Hilfseigenschaft bildDaten
				this.bildDaten = data.getBild();
				this.bildUnterschrift = BerichtHelper.SAFE_HTML_POLICY
						.sanitize(data.getBildUnterschrift() != null ? data.getBildUnterschrift() : "");
				// Erzeuge einen Data-URI-String zur Anzeige
				this.bildLink = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bildDaten);
			} else {
				this.bildDaten = null;
				this.bildLink = null;
				this.bildUnterschrift = null;
			}

			// Spielergebnisse abrufen
			if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
				this.spielErgebnis = dbService.loadSpielstatistik(vereinnr, ergebnisLink);
			}

			this.mitSpielberichte = data.getMitSpielberichte();
		} else {
			this.berichtText = "";
		}
	}

	public List<Spiel> getSpieleFreigegeben() {
		long spielplanRueckschauTage = Long
				.parseLong(ConfigManager.getConfigValue(vereinnr, "spielplan.rueckschau.tage"));
		long freierBerichtRueckschauTage = Long
				.parseLong(ConfigManager.getConfigValue(vereinnr, "freierBericht.rueckschau.tage"));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate heute = LocalDate.now();
		LocalDate vorSpieltagen = heute.minusDays(spielplanRueckschauTage);
		LocalDate vorFreienTagen = heute.minusDays(freierBerichtRueckschauTage);

		return spieleFreigegeben.stream().filter(spiel -> {
			LocalDate spielDatum = LocalDate.parse(spiel.getDatum(), formatter);
			LocalDate grenze = spiel.getErgebnisLink().startsWith("http") ? vorSpieltagen : vorFreienTagen;
			return (spielDatum.isAfter(grenze) || spielDatum.isEqual(grenze));
		}).collect(Collectors.toList());
	}

	private boolean matchesLigaFilter(Spiel spiel) {
		if (liga == null || liga.isBlank()) {
			return true;
		}
		String spielLiga = spiel.getLiga();
		return spielLiga != null && liga.trim().equalsIgnoreCase(spielLiga.trim());
	}
	

	private boolean matchesFreiFilter(Spiel spiel) {
		boolean istSpielbericht = spiel.getErgebnisLink().startsWith("http");
		if ("Ja".equalsIgnoreCase(freieBerichte)) {
			return !istSpielbericht;
		}
		return istSpielbericht || spiel.isMitSpielberichte();
	}



	public void setBerichtDatum(String berichtDatum) {
		this.berichtDatum = berichtDatum;
	}

	public String getFreierText() {
		return freierText;
	}

	public void setFreierText(String freierText) {
		this.freierText = freierText;
	}

	public void onDateSelect() {
		if (berichtDatumCal != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
			berichtDatum = sdf.format(berichtDatumCal);
		}
	}

	// Getter und Setter
	public Date getBerichtDatumCal() {
		return berichtDatumCal;
	}

	public void setBerichtDatumCal(Date berichtDatumCal) {
		this.berichtDatumCal = berichtDatumCal;
		onDateSelect(); // Automatische Konvertierung nach der Eingabe
	}

	public String getBerichtDatum() {
		return berichtDatum;
	}

	public void updConfig() {
		config = ConfigManager.updInstance();
	}

	public String getupdConfig() {
		return "";
	}

	public void setUpdConfig(String ergebnis) {
	}

	public String getBerichte() {
		return berichte;
	}

	public void setBerichte(String berichte) {
		this.berichte = berichte;
	}

	// Download-Methode: jetzt über die Bean
	public List<BerichtText> getMeineBerichtTexte() {
		return meineBerichtTexte;
	}

	public void setMeineBerichtTexte(List<BerichtText> meineBerichtTexte) {
		this.meineBerichtTexte = meineBerichtTexte;
	}

	public void selectItem(Spiel item) {
		this.selectedItem = item;
	}

	public Spiel getSelectedItem() {
		return selectedItem;
	}

	public void ladeIframe(Spiel spiel) throws UnsupportedEncodingException {

		datum = spiel.getDatum();
		liga = spiel.getLiga();
		heim = spiel.getHeim();
		gast = spiel.getGast();
		ergebnis = spiel.getErgebnis();
		ergebnisLink = spiel.getErgebnisLink();

		berichtLaden();
	}

	public String getIframeUrl() {
		return iframeUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void login() {
		// Hier könntest du z.B. einfach die eingegebenen Werte in einem Log speichern.
		// Keine Navigation, nur Speicherung
	}

	public void altSetzen() {
		dbService.kopierenLogData(ergebnisLink, altLink, "Veröffentlichen");
	}

	public List<String> getHomepages() throws IOException, InterruptedException, URISyntaxException {
		String domains = ConfigManager.getConfigValue(vereinnr, "wordpress.domains");
		String[] werteArray = domains.split(",");
		return Arrays.asList(werteArray);

	}

	public String getHomepage() {
		return BerichtHelper.getHomepage(vereinnr);
	}

	public String getItemValue() {
		return BerichtHelper.getHomepageStandardEinzel(vereinnr);
	}

	public String getZeitung() {
		return BerichtHelper.getZeitungUrl(vereinnr);
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	// Gibt den Bild-Link zu=-1rück, damit h:graphicImage das Bild anzeigen kann.
	public String getBildUrl() {
		return bildLink;
	}

	public void prepareDownloadBild() {
		BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);
		if (data != null && data.getBild() != null) {
			InputStream stream = new ByteArrayInputStream(data.getBild());
			downloadBild = DefaultStreamedContent.builder().stream(() -> stream).contentType("image/jpeg")
					.name(datum + "_-_" + heim.replace(" ", "_") + "_-_" + gast.replace(" ", "_") + "_-_"
							+ ergebnis.replace(":", "_") + ".jpg")
					.build();

		} else {
			downloadBild = null;
		}
	}

	public boolean hasBild(String ergebnisLink) {
		return BerichtHelper.hasBild(vereinnr, ergebnisLink);
	}

	public boolean hasFreigabe(String ergebnisLink) {
		return BerichtHelper.hasFreigabe(vereinnr, ergebnisLink);
	}

	public boolean hasHomepage(String ergebnisLink) {
		return BerichtHelper.hasHomepage(vereinnr, ergebnisLink);
	}

	public boolean hasBlaettle(String ergebnisLink) {
		return BerichtHelper.hasBlaettle(vereinnr, ergebnisLink);
	}

	public boolean hasBericht(String ergebnisLink) {
		return BerichtHelper.hasBericht(vereinnr, ergebnisLink);
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public boolean isBereitsFreigegeben() {
		return bereitsFreigegeben;
	}

	public void setBereitsFreigegeben(boolean bereitsFreigegeben) {
		this.bereitsFreigegeben = bereitsFreigegeben;
	}

	public void resetFreigabe() {
		this.bereitsFreigegeben = false;
	}

	public StreamedContent getDownloadBild() {
		return downloadBild;
	}

	public void blaettle() {
		dbService.saveLogData(vereinnr, ergebnisLink, name, "Veröffentlichung Blättle", "J");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
	}

	public void freigabeAufheben() {
		dbService.deleteLogData(vereinnr, ergebnisLink, "Freigegeben");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
	}

	public List<LogEntry> getLogEntries() {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return new ArrayList<>();
		}
		// Lese die Log-Einträge nur einmal
		if (logEntries == null) {
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
		}
		return logEntries;
	}

	public List<String> getSelectKategorie() {
		return selectKategorie;
	}

	public void setSelectKategorie(List<String> selectKategorie) {
		this.selectKategorie = selectKategorie;
	}

	public void veroeffentlichen() throws URISyntaxException, IOException, InterruptedException {
		String wordpressDatum;
		if ("Spieldatum".equals(ConfigManager.getWordpressValue(vereinnr, name, "datum"))) {
			wordpressDatum = konvertiereDatum(datum);
		} else {
			wordpressDatum = createWordPressDateString();
		}

		String subject;
		if (!"".equals(gast)) {
			subject = heim + " - " + gast + "  " + ergebnis;
		} else {
			subject = heim;
		}

		String bildname = (datum + "_" + heim + "_" + gast).replace(" ", "_").replace(".", "-") + ".jpg";

		if (spielErgebnis == null) {
			spielErgebnis = "";
		}

		WordPressAPIClient client = new WordPressAPIClient(vereinnr, username, password, name);
		byte[] attachment = null;
		WordpressMedia image = new WordpressMedia();

		if (bildBytesAvailable()) {
			attachment = getBildBytes();
			image = client.uploadMediaAndInsertIntoPost(attachment, bildname, "image/jpeg", "Mannschaftsfoto",
					bildUnterschrift);
		}

		String body = getBerichtWerbungHomepage();
		String bildVariante = ConfigManager.getWordpressValue(vereinnr, name, "beitragsbild");

		// falls ein Bild vorhanden ist, füge es in den Content ein
		if (image != null && image.getMediaId() != -1 && !"nurBeitragsbild".equals(bildVariante)) {
			body = image.getHtml() + body;
		} else if ("nurBeitragsbild".equals(bildVariante) && (bildUnterschrift != null)) {
			StringBuilder text = new StringBuilder();
			text.append("<p style=\"text-align: center; font-style: italic; font-size: 0.8em;\"> \n\n");
			text.append(escapeHtml(getBildUnterschrift()));
			text.append("</p>\n\n");
			text.append(body);
			body = text.toString();

		}

		int postid;

		try {
			postid = client.createPost(vereinnr, subject, body, image != null ? image.getMediaId() : -1, // featured_media
					selectKategorie, wordpressDatum, image != null ? image.getUrl() : "" // falls deine createPost das
																							// braucht
			);

			if (postid == -1) {
				dbService.saveLogData(vereinnr, ergebnisLink, username, "Veröffentlichen Fehler", "N");
			} else {
				dbService.saveLogData(vereinnr, ergebnisLink, username,
						"Veröffentlichen " + name + " OK (PostId=" + postid + ")", "W");
			}
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);

		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Es konnte kein Blog-Eintrag erstellt werden"));
			dbService.saveLogData(vereinnr, ergebnisLink, username, "Veröffentlichen Fehler", "N");
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Spielbericht konnte nicht veröffentlicht werden: " + e.getMessage()));
			e.printStackTrace();
		}
	}

	/**
	 * Einfaches HTML-escaping (sehr leichtgewichtig, für Alt-Text/Caption/URL)
	 */
	private String escapeHtml(String input) {
		if (input == null) {
			return "";
		}
		String result = input;
		result = result.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		result = result.replace("\"", "&quot;");
		result = result.replace("'", "&#x27;");
		return result;
	}

	public static String konvertiereDatum(String datumString) {
		DateTimeFormatter eingabeFormatierer = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		DateTimeFormatter ausgabeFormatierer = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

		LocalDate datum;
		try {
			datum = LocalDate.parse(datumString, eingabeFormatierer);
		} catch (DateTimeParseException e) {
			// Wenn der String kein gültiges Datum ist, verwende das aktuelle Datum
			datum = LocalDate.now();
		}
		return datum.atStartOfDay().format(ausgabeFormatierer);
	}

	public static String createWordPressDateString() {
		// Aktuelles Datum und Uhrzeit
		Instant now = Instant.now();

		// DateTimeFormatter für ISO 8601 mit Zeitzoneninformationen
		// Die WordPress API bevorzugt UTC-Zeit (Z am Ende)
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);
		return formatter.format(now) + "Z"; // 'Z' für UTC-Zeitzone hinzufügen
	}

	private byte[] getBildBytes() {
		if (resizedImage != null && !resizedImage.isEmpty()) {
			try {
				String base64Data = resizedImage.split(",")[1];
				return Base64.getDecoder().decode(base64Data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (bildDaten != null && bildDaten.length > 0) {
			return bildDaten;
		}
		return null;
	}

	private boolean bildBytesAvailable() {
		return (resizedImage != null && !resizedImage.isEmpty()) || (bildDaten != null && bildDaten.length > 0);
	}

	public String getBerichtWerbungHomepage() {
		StringBuilder text = new StringBuilder();

		text.append(berichtText);

		String werbung = ConfigManager.getWordpressValue(vereinnr, this.name, "werbung");
		if (this.spielErgebnis != null || this.spielErgebnis.isBlank()) {
			text.append("<p></p>");
			text.append("<p>" + this.spielErgebnis + "</p>");
		}

		if (!"Nein".equals(werbung)) {
			text.append("<hr /> <p>" + werbung + "</p>");
		}
		return text.toString();
	}

	public String getBildUnterschrift() {
		return bildUnterschrift;
	}

	public List<String> getKategorie() throws IOException, InterruptedException, URISyntaxException {
		WordPressAPIClient client = new WordPressAPIClient(vereinnr, username, password, name);
		return client.getCategoryNames(vereinnr);
	}

	public int getAnzahlWordpress() {
		return dbService.anzahlWordpress(vereinnr, ergebnisLink, name);
	}

	public void setAnzahlWordpress(int anzahlWordpress) {
		this.anzahlWordpress = anzahlWordpress;
	}

	public String getLink() {
		return ConfigManager.getWordpressValue(vereinnr, name, "domain") + "/?p="
				+ dbService.getHomepageEntries(vereinnr, ergebnisLink, name);

	}

	public String getDomain() {
		return ConfigManager.getWordpressValue(vereinnr, name, "domain");
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public ConfigManager getConfig() {
		return config;
	}

	public DatabaseService getDbService() {
		return dbService;
	}

	public String getFreieBerichte() {
		return freieBerichte;
	}

	public String getBildLink() {
		return bildLink;
	}

	public String getBerichtText() {
		return berichtText;
	}

	public byte[] getBildDaten() {
		return bildDaten;
	}

	public String getSpielErgebnis() {
		return spielErgebnis;
	}

	public String getHeim() {
		return heim;
	}

	public String getGast() {
		return gast;
	}

	public String getDatum() {
		return datum;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public String getLiga() {
		return liga;
	}

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public String getResizedImage() {
		return resizedImage;
	}

	public void setSpieleFreigegeben(List<Spiel> spieleFreigegeben) {
		this.spieleFreigegeben = spieleFreigegeben;
	}

	public void setConfig(ConfigManager config) {
		this.config = config;
	}

	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	public void setSelectedItem(Spiel selectedItem) {
		this.selectedItem = selectedItem;
	}

	public void setIframeUrl(String iframeUrl) {
		this.iframeUrl = iframeUrl;
	}

	public void setFreieBerichte(String freieBerichte) {
		this.freieBerichte = freieBerichte;
	}

	public void setBildLink(String bildLink) {
		this.bildLink = bildLink;
	}

	public void setBerichtText(String berichtText) {
		this.berichtText = berichtText;
	}

	public void setBildDaten(byte[] bildDaten) {
		this.bildDaten = bildDaten;
	}

	public void setBildUnterschrift(String bildUnterschrift) {
		this.bildUnterschrift = bildUnterschrift;
	}

	public void setSpielErgebnis(String spielErgebnis) {
		this.spielErgebnis = spielErgebnis;
	}

	public void setDownloadBild(StreamedContent downloadBild) {
		this.downloadBild = downloadBild;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	public void setLogEntries(List<LogEntry> logEntries) {
		this.logEntries = logEntries;
	}

	public void setResizedImage(String resizedImage) {
		this.resizedImage = resizedImage;
	}

	public String getBerichtWerbung() {
		StringBuilder text = new StringBuilder();
		if (liga != null && !liga.isBlank()) {
			String textLiga = BerichtHelper.getLigaJugend(liga);

			if (!textLiga.isBlank()) {
				text.append("<Strong>" + BerichtHelper.getLigaJugend(liga) + "</Strong><br>");
			}
		}
		text.append("<Strong>");
		if (ergebnisLink.startsWith("http")) {
			text.append("<span style=\"display:inline-block; width:80%; text-align:left; font-weight:bold;\">");
			text.append(getHeim());
			text.append(" - ");
			text.append(getGast());
			text.append("  ");
			text.append("</span>");
			text.append("<span style=\"display:inline-block; width:18%; text-align:right; font-weight:bold;\">");
			text.append(getErgebnis());
			text.append("</span>");
		} else {
			text.append(getHeim());
		}
		text.append("</Strong><br>");

		return text.append(getBerichtWerbungHomepage()).toString();
	}

	public String getBerichtAnzeige() {
		return BerichtHelper.mergeParagraphsWithJsoup(getBerichtWerbung());
	}

	public String getBerichtWerbungBlaettle() {

		return BerichtHelper.convertQuillClassesToInlineStyles(getBerichtWerbung());
	}

	public void setBerichtWerbungBlaettle(String nichts) {
		return;
	}

	public String getAltLink() {
		return altLink;
	}

	public void setAltLink(String altLink) {
		this.altLink = altLink;
	}

	public boolean isFreigegebeneBerichte() {
		return freigegebeneBerichte;
	}

	public void setFreigegebeneBerichte(boolean freigegebeneBerichte) {
		this.freigegebeneBerichte = freigegebeneBerichte;
		erstellenBerichtListe(freigegebeneBerichte);
	}

	// Getter / Setter
	public boolean isMitSpielberichte() {
		return mitSpielberichte;
	}

	public void setMitSpielberichte(boolean mitSpielberichte) {
		this.mitSpielberichte = mitSpielberichte;
	}

	public void updateMitSpielberichte() {
		dbService.updateMitSpielberichte(vereinnr, ergebnisLink, mitSpielberichte);
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public String ruecksprung() {

		if ("Ja".equals(freieBerichte)) {
			return "freieBerichte.xhtml";
		}

		if (gruppeUrl != null && !gruppeUrl.isEmpty()) {
			return "spielplan.xhtml";
		}

		if (isTischtennis()) {
			return "spielplan.xhtml";
		}

		return "index.xhtml";

	}

	public void zurueck() {

	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}
}