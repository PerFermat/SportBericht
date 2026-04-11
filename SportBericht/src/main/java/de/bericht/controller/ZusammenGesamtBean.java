package de.bericht.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import de.bericht.provider.SpielplanProvider;
import de.bericht.service.BerichtText;
import de.bericht.service.Bilddaten;
import de.bericht.service.DatabaseService;
import de.bericht.service.Spiel;
import de.bericht.service.WordpressMedia;
import de.bericht.util.BerichtData;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.IgnorierteWoerte;
import de.bericht.util.WordPressAPIClient;
import de.bericht.util.enums.WordpressBeitragsbildOption;
import de.bericht.util.enums.WordpressDatumOption;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("zusammenGesamtBean")
@ViewScoped
public class ZusammenGesamtBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private List<Spiel> spieleFreigegeben;
	private String username;
	private String passwort;
	private String berichte;
	private String ueberschrift;
	private String berichtDatum;
	private Date berichtDatumCal;
	private String freieBerichte;
	ConfigManager config;
	private DatabaseService dbService = new DatabaseService();
	List<BerichtText> meineBerichtTexte = new ArrayList<>();
	private Spiel selectedItem;
	private String iframeUrl;
	private String name;
	private String liga;
	private StreamedContent downloadBild;
	private List<String> selectKategorie;
//	private String bildLink;
//	private byte[] bildDaten;
//	private String bildUnterschrift;
	private List<Bilddaten> bildArray = new ArrayList<>();
	private String domain;
	private String vereinnr;
	private String gruppeUrl;
	private int postid;
	private boolean freigegebeneBerichte = true;
	private SpielplanProvider provider;
	private List<Spiel> spiele;

	private String freierText;

	public ZusammenGesamtBean() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		this.vereinnr = params.get("vereinnr");
		this.liga = params.get("liga");
		this.gruppeUrl = params.get("gruppeUrl");
		freieBerichte = params.get("frei");
		spieleFreigegeben = new ArrayList<>();

		spiele = dbService.listeBerichteMitSpielMetadaten(vereinnr);

		erstellenBerichtListe(freigegebeneBerichte);
		name = BerichtHelper.getHomepageStandardZusammen(vereinnr);
		domain = ConfigManager.getWordpressValue(vereinnr, name, "domain");
	}

	public void erstellenBerichtListe(boolean freigegeben) {
		spieleFreigegeben.clear();
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");

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

		return "liga.xhtml";

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

	public String getBerichteUeberschrift() {
		String reinerText = getBerichte().replace("<br><br>", "<br>").replace("\n", "").replaceAll("\\[BILD\\d+\\]", "")
				.replaceAll("\\[UNTERSCHRIFT\\d+\\]", "");

		return ueberschrift + "<br><br>" + BerichtHelper.mergeParagraphsWithJsoup(reinerText);
	}

	public String getBerichte() {
		if (berichte == null || berichte.isEmpty() || berichte.isBlank()) {
			return "";
		}
		StringBuilder text = new StringBuilder();
		text.append(berichte);

		String werbung = ConfigManager.getWordpressValue(vereinnr, this.name, "werbung");

		if (!"Nein".equals(werbung)) {
			text.append("<hr /> <p>" + werbung + "</p>");
		}
		return text.toString();
	}

	public String getBerichteAnzeige() {
		return BerichtHelper.mergeParagraphsWithJsoup(getBerichte());
	}

	public void setBerichte(String berichte) {
		this.berichte = berichte;
	}

	public void setBerichteUeberschrift(String text) {

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

	public String getIframeUrl() {
		return iframeUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		domain = ConfigManager.getWordpressValue(vereinnr, this.name, "domain");
	}

	public void prepareDownloadBild(String ergebnisLink, Spiel spiel) {
		BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);
		if (data != null && data.getBild() != null) {
			InputStream stream = new ByteArrayInputStream(data.getBild());
			downloadBild = DefaultStreamedContent.builder().stream(() -> stream).contentType("image/jpeg")
					.name(spiel.getDatum() + "_-_" + spiel.getHeim().replace(" ", "_") + "_-_"
							+ spiel.getGast().replace(" ", "_") + "_-_" + spiel.getErgebnis().replace(":", "_")
							+ ".jpg")
					.build();
		} else {
			downloadBild = null;
		}
	}

	public StreamedContent getDownloadBild() {
		return downloadBild;
	}

	public void berichtupd() {
		bildArray.clear();

		StringBuilder text = new StringBuilder();
		WordpressBeitragsbildOption bildVariante = WordpressBeitragsbildOption
				.fromConfig(ConfigManager.getWordpressValue(vereinnr, name, "beitragsbild"));
		if (WordpressBeitragsbildOption.NUR_BEITRAGSBILD == bildVariante) {

			text.append("<p style=\"text-align: center; font-style: italic; font-size: 0.8em;\"> <br>");
			text.append("[UNTERSCHRIFT0]");
			text.append("</p><br>");
		}
		IgnorierteWoerte ignorieren = new IgnorierteWoerte();
		if (ConfigManager.isTennis(vereinnr)) {
			ueberschrift = "Tennis Spielberichte "
					+ berechneKalenderwoche(findeFruehestesDatum(getSpieleFreigegeben()));
		} else {
			ueberschrift = "Tischtennis Spielberichte "
					+ berechneKalenderwoche(findeFruehestesDatum(getSpieleFreigegeben()));

		}
		int bildnr = 0;
		for (Spiel spiel : getSpieleFreigegeben()) {

			if (!spiel.isWahl()) {
			} else if (spiel.getErgebnisLink() == null) {
				text.append("<hr /> <br>");
				text.append("<p>");
				text.append("<strong>");
				text.append(spiel.getHeim());
				text.append("</strong>");
				text.append("</p><p>");
				BerichtData data = dbService.loadBerichtData(vereinnr, spiel.getErgebnisLink());
				text.append(data.getBerichtText() != null
						? BerichtHelper.SAFE_HTML_POLICY.sanitize(data.getBerichtText().trim())
						: " Noch kein Bericht vorhanden ");
				text.append("</p><br><br>");
			} else {
				text.append("<hr /> <br>");
				text.append("<p>");
				text.append("<strong>");
				if (!spiel.getLigaJugend().isBlank()) {
					text.append(spiel.getLigaJugend());
					text.append("<br>");
				}

				if (spiel.getErgebnisLink().startsWith("http")) {
					text.append("<span style=\"display:inline-block; width:80%; text-align:left; font-weight:bold;\">");
					text.append(spiel.getHeim());
					text.append(" - ");
					text.append(spiel.getGast());
					text.append(" (");
					text.append(spiel.getDatum());
					text.append(")  ");
					text.append("</span>");
					text.append(
							"<span style=\"display:inline-block; width:18%; text-align:right; font-weight:bold;\">");
					text.append(spiel.getErgebnis());
					text.append("</span>");
				} else {
					text.append(spiel.getHeim());
				}
				text.append("</strong>");
				text.append("</p>");

				BerichtData data = dbService.loadBerichtData(vereinnr, spiel.getErgebnisLink());
				if (data.getBild() != null) {
					Bilddaten bildDaten = new Bilddaten();
					String bildname = (spiel.getDatum() + "_" + spiel.getHeim() + "_" + spiel.getGast())
							.replace(" ", "_").replace(".", "-").replace("\"", "HK") + ".jpg";
					bildDaten.setBildName(bildname);
					bildDaten.setBildUnterschrift(data.getBildUnterschrift() != null ? data.getBildUnterschrift() : "");
					bildDaten.setBildDaten(data.getBild());
					bildDaten.setBildLink(
							"data:image/jpeg;base64," + Base64.getEncoder().encodeToString(data.getBild()));
					bildArray.add(bildDaten);
					if (WordpressBeitragsbildOption.NUR_BEITRAGSBILD != bildVariante || bildnr > 0) {
						text.append("[BILD" + bildnr + "]" + "<br><br>");
					}
					bildnr++;
				} else {
					text.append("<p></p>");
				}
				text.append(data.getBerichtText() != null
						? BerichtHelper.SAFE_HTML_POLICY.sanitize(data.getBerichtText().trim())
						: " Noch kein Bericht vorhanden ");

				String ergebnis = null;
				if (spiel.getErgebnisLink().startsWith("http")) {
					ergebnis = dbService.loadSpielstatistik(vereinnr, spiel.getErgebnisLink());

					if (ergebnis != null) {
						text.append("<p></p>");
						text.append("<p>");
						text.append(ergebnis);
						text.append("</p> <br>");
					}
				}
			}
		}

		// ermittelnBild(spieleFreigegeben);
		berichte = getHtmlText(text.toString());
	}

	public String getHtmlText(String textMitUmbruechen) {
		// \n durch <br/> ersetzen
		return textMitUmbruechen;
		// return textMitUmbruechen.replaceAll("\n", "<br>");
	}

	public static String findeFruehestesDatum(List<Spiel> spiele) {
		if (spiele == null || spiele.isEmpty()) {
			return "Kein Datum vorhanden";
		}

		DateTimeFormatter eingabeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate minDatum = LocalDate.MAX;

		for (Spiel spiel : spiele) {

			try {
				String datumTeil = spiel.getDatum().substring(0, 10);
				LocalDate current = LocalDate.parse(datumTeil, eingabeFormat);
				if (current.isBefore(minDatum) && spiel.isWahl()) {
					minDatum = current;
				}
			} catch (Exception e) {
				// Ignoriere ungültige Einträge
			}
		}
		return minDatum.format(eingabeFormat);
	}

	public static String findeSpaetestesDatum(List<Spiel> spiele) {
		if (spiele == null || spiele.isEmpty()) {
			return "Kein Datum vorhanden";
		}

		DateTimeFormatter eingabeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate maxDatum = LocalDate.MIN;

		for (Spiel spiel : spiele) {
			try {
				String datumTeil = spiel.getDatum().substring(0, 10);
				LocalDate current = LocalDate.parse(datumTeil, eingabeFormat);
				if (current.isAfter(maxDatum) && spiel.isWahl()) {
					maxDatum = current;
				}
			} catch (Exception e) {
				// Ignoriere ungültige Einträge
			}
		}

		return maxDatum.format(eingabeFormat);
	}

//	public void ermittelnBild(List<Spiel> spiele) {
//		for (Spiel spiel : spiele) {
//			if (spiel.isWahl()) {
//				try {
//					DatabaseService db = new DatabaseService();
//					BerichtData data = db.loadBerichtData(vereinnr, spiel.getErgebnisLink());
//					if (data.getBild() != null && spiel.isWahl()) {
//						
//						this.bildUnterschrift = data.getBildUnterschrift() != null ? data.getBildUnterschrift() : "";
//						this.bildDaten = data.getBild();
//						this.bildLink = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bildDaten);
//						return;
//					}
//				} catch (Exception e) {
//					// Ignoriere ungültige Einträge
//				}
//			}
//		}
//		return;
//	}

	public static String berechneKalenderwoche(String datumString) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
			LocalDate datum = LocalDate.parse(datumString, formatter);

			WeekFields weekFields = WeekFields.of(Locale.GERMANY);
			int kalenderwoche = datum.get(weekFields.weekOfWeekBasedYear());
			int jahr = datum.get(weekFields.weekBasedYear());

			return "KW" + kalenderwoche + "/" + (jahr - 2000);
		} catch (Exception e) {
			return "";
		}
	}

	// Getter und Setter
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswort() {
		return passwort;
	}

	public void setPasswort(String passwort) {
		this.passwort = passwort;
	}

	public void login() {
		// Hier könntest du z.B. einfach die eingegebenen Werte in einem Log speichern.
		// Keine Navigation, nur Speicherung
	}

	public List<String> getSelectKategorie() {
		return selectKategorie;
	}

	public void setSelectKategorie(List<String> selectKategorie) {
		this.selectKategorie = selectKategorie;
	}

	public List<String> getKategorie() throws IOException, InterruptedException, URISyntaxException {
		WordPressAPIClient client = new WordPressAPIClient(vereinnr, username, passwort, name);
		return client.getCategoryNames(vereinnr);
	}

	public String getDomain() {
		return domain;
	}

	public void veroeffentlichen() throws URISyntaxException, IOException, InterruptedException {
		String wordpressDatum;
		WordpressDatumOption datumOption = WordpressDatumOption
				.fromConfig(ConfigManager.getWordpressValue(vereinnr, name, "datum"));
		if (WordpressDatumOption.SPIELDATUM == datumOption) {

			wordpressDatum = konvertiereDatum(findeSpaetestesDatum(spieleFreigegeben));
		} else {
			wordpressDatum = createWordPressDateString();
		}

		String subject = ueberschrift;

		WordPressAPIClient client = new WordPressAPIClient(vereinnr, username, passwort, name);
		if (!bildArray.isEmpty()) {
			for (Bilddaten bild : bildArray) {
				WordpressMedia media = client.uploadMediaAndInsertIntoPost(bild.getBildDaten(), bild.getBildName(),
						bild.getBildFormat(), "Mannschaftsfoto", bild.getBildUnterschrift());
				bild.setMediaId(media);
			}

		}

		String body = getBerichte() + "\n";
		try {

			postid = client.createPost(vereinnr, subject, body, bildArray, selectKategorie, wordpressDatum);
			for (Spiel spiel : spieleFreigegeben) {
				if (spiel.isWahl()) {
					if (postid == -1) {
						dbService.saveLogData(vereinnr, spiel.getErgebnisLink(), username, "Veröffentlichen Fehler",
								"N");
					} else {
						dbService.saveLogData(vereinnr, spiel.getErgebnisLink(), username,
								"Veröffentlichen " + name + " OK (PostId=" + postid + ")", "W");
					}
				}
			}

		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Spielbericht konnte konnte nicht veröffentlicht werden: " + e.getMessage()));
			e.printStackTrace();
		}
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

	public String getUeberschrift() {
		return ueberschrift;
	}

	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = ueberschrift;
	}

//
	public String getBildLink() {
		if (bildArray.size() > 0) {
			return bildArray.get(0).getBildLink();
		}
		return null;

	}

	public String getBildUnterschrift() {
		if (bildArray.size() > 0) {
			return bildArray.get(0).getBildUnterschrift();
		}
		return null;

	}

//	public byte[] getBildDaten() {
//		return bildDaten;
//	}
//
//	public String getBildUnterschrift() {
//		return bildUnterschrift;
//	}
//
//	public void setBildLink(String bildLink) {
//		this.bildLink = bildLink;
//	}
//
//	public void setBildDaten(byte[] bildDaten) {
//		this.bildDaten = bildDaten;
//	}
//
//	public void setBildUnterschrift(String bildUnterschrift) {
//		this.bildUnterschrift = bildUnterschrift;
//	}

	public int getPostid() {
		return postid;
	}

	public void setPostid(int postid) {
		this.postid = postid;
	}

	public String link() {
		domain = ConfigManager.getWordpressValue(vereinnr, name, "domain") + "/?p=" + postid;
		return domain;
	}

	public String domain() {
		domain = ConfigManager.getWordpressValue(vereinnr, name, "domain");
		return domain;
	}

	public List<String> getHomepages() throws IOException, InterruptedException, URISyntaxException {
		String domains = ConfigManager.getConfigValue(vereinnr, "wordpress.domains");
		String[] werteArray = domains.split(",");
		return Arrays.asList(werteArray);

	}

	public String getItemValue() {
		return BerichtHelper.getHomepageStandardZusammen(vereinnr);
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
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

	public void moveUp(Spiel spiel) {
		int index = spieleFreigegeben.indexOf(spiel);
		if (index > 0) {
			Collections.swap(spieleFreigegeben, index, index - 1);
		}
	}

	public void moveDown(Spiel spiel) {
		int index = spieleFreigegeben.indexOf(spiel);
		if (index < spieleFreigegeben.size() - 1) {
			Collections.swap(spieleFreigegeben, index, index + 1);
		}
	}

	public boolean isFirst(Spiel spiel) {
		return spieleFreigegeben.indexOf(spiel) == 0;
	}

	public boolean isLast(Spiel spiel) {
		return spieleFreigegeben.indexOf(spiel) == spieleFreigegeben.size() - 1;
	}

	public void setWahl(Spiel s) {
		s.setWahl(false);
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public boolean isFreigegebeneBerichte() {
		return freigegebeneBerichte;
	}

	public void setFreigegebeneBerichte(boolean freigegebeneBerichte) {
		this.freigegebeneBerichte = freigegebeneBerichte;
		erstellenBerichtListe(freigegebeneBerichte);
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public void zurueck() {

	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}
}