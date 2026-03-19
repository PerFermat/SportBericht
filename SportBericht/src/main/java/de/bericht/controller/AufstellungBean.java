package de.bericht.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.bericht.service.AufstellungSpielInfo;
import de.bericht.service.AufstellungSpieler;
import de.bericht.service.DatabaseService;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("aufstellungBean")
@ViewScoped
public class AufstellungBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ConfigManager configManager = ConfigManager.getInstance();
	private final DatabaseService databaseService = new DatabaseService();

	private String vereinnr;
	private String uniqueKey;
	private String ruecksprung;
	private String betreuer;
	private String rueckmeldeStatus;
	private String liga;

	private AufstellungSpielInfo spielInfo;
	private final List<AufstellungSpieler> spielerListe = new ArrayList<>();
	private final List<String> selectedSpielerKeys = new ArrayList<>();
	private final Map<String, Boolean> spielerAusgewaehlt = new HashMap<>();
	private final Map<String, String> spielerKommentare = new HashMap<>();
	private final Map<String, String> spielerVerfuegbarkeitHinweis = new HashMap<>();
	private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private String spielKommentar;

	private static final List<String> STANDARD_KOMMENTARE = List.of("Ja", "Nein", "Ja (im Notfall)",
			"Ja (aber noch nicht sicher)", "Nein (aber noch nicht sicher)");

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		uniqueKey = request.getParameter("uuid");
		ruecksprung = request.getParameter("ruecksprung");

		if (vereinnr == null || vereinnr.isBlank() || uniqueKey == null || uniqueKey.isBlank()) {
			context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehlende Parameter",
					"Bitte v (Verein) und uuid (Spielkennung) in der URL übergeben."));
			return;
		}

		ladeDaten();
	}

	private void ladeDaten() {
		ladeSpielInfo();
		ladeBetreuerInfo();
		ladeAufstellung();
		ladeSpielerVerfuegbarkeitHinweise();
		ladeAuswahl();
		ladeSpielKommentar();
	}

	private void ladeSpielInfo() {
		spielInfo = databaseService.ladeAufstellungSpielInfo(vereinnr, uniqueKey);
		if (spielInfo != null) {
			liga = spielInfo.getLiga();
		}
	}

	private void ladeSpielerVerfuegbarkeitHinweise() {
		spielerVerfuegbarkeitHinweis.clear();
		if (spielInfo == null) {
			return;
		}
		LocalDate spielDatum = parseDatumSafe(spielInfo.getDatum());
		LocalTime spielZeit = parseZeitSafe(spielInfo.getZeit());
		for (AufstellungSpieler spieler : spielerListe) {
			Map<String, String> passendeAntwort = findePassendeJaAntwort(spieler.getName(), spielDatum, spielZeit);
			if (passendeAntwort == null) {
				continue;
			}
			String zeitText = normalizeZeitText(passendeAntwort.get("uhrzeit"));
			String kommentar = passendeAntwort.get("kommentar");
			if (passendeAntwort.get("kommentar") == null) {
				kommentar = "";
			}
			String verfuegbarkeit = passendeAntwort.get("verfuegbarkeit");
			spielerVerfuegbarkeitHinweis.put(spieler.getKey(), verfuegbarkeit + " - " + kommentar);
		}

	}

	private Map<String, String> findePassendeJaAntwort(String spielerName, LocalDate spielDatum, LocalTime spielZeit) {
		if (spielerName == null || spielerName.isBlank()) {
			return null;
		}
		Map<String, String> kandidat = null;
		long bestDelta = Long.MAX_VALUE;
		for (Map<String, String> antwort : databaseService.ladeSpielerVerfuegbarkeitNachName(vereinnr, spielerName)) {
			if (!startsWithIgnoreCase(antwort.get("verfuegbarkeit"), "Ja")) {
				continue;
			}
			LocalDate antwortDatum = parseDatumSafe(antwort.get("datum"));
			if (!spielDatum.equals(antwortDatum)) {
				continue;
			}
			LocalTime antwortZeit = parseZeitSafe(antwort.get("uhrzeit"));
			long deltaMinutes = Math.abs(ChronoUnit.MINUTES.between(spielZeit, antwortZeit));
			if (deltaMinutes > 240) {
				continue;
			}
			if (deltaMinutes < bestDelta) {
				bestDelta = deltaMinutes;
				kandidat = antwort;
			}
		}
		return kandidat;
	}

	private LocalDate parseDatumSafe(String datum) {
		if (datum == null || datum.isBlank()) {
			return LocalDate.of(2999, 12, 31);
		}
		try {
			return LocalDate.parse(datum, DATUM_FORMAT);
		} catch (DateTimeParseException e) {
			return LocalDate.of(2999, 12, 31);
		}
	}

	private LocalTime parseZeitSafe(String zeit) {
		String normalisierteZeit = normalizeZeitText(zeit);
		if (normalisierteZeit.isBlank()) {
			return LocalTime.MIN;
		}
		try {
			return LocalTime.parse(normalisierteZeit, DateTimeFormatter.ofPattern("H:mm"));
		} catch (DateTimeParseException ex) {
			try {
				return LocalTime.parse(normalisierteZeit, DateTimeFormatter.ofPattern("HH:mm"));
			} catch (DateTimeParseException ignored) {
				return LocalTime.MIN;
			}
		}
	}

	private String normalizeZeitText(String zeit) {
		if (zeit == null) {
			return "";
		}
		String trimmed = zeit.trim();
		if (trimmed.length() > 5) {
			trimmed = trimmed.substring(0, 5);
		}
		return trimmed;
	}

	private boolean startsWithIgnoreCase(String value, String prefix) {
		if (value == null || prefix == null) {
			return false;
		}
		return value.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private void ladeBetreuerInfo() {
		Map<String, Object> betreuerStatus = databaseService.ladeBetreuerStatus(uniqueKey);
		if (!betreuerStatus.isEmpty()) {
			betreuer = (String) betreuerStatus.get("betreuer");
			Boolean bestaetigt = (Boolean) betreuerStatus.get("bestaetigt");
			if (bestaetigt == null) {
				rueckmeldeStatus = "offen";
			} else {
				rueckmeldeStatus = bestaetigt ? "bestätigt" : "abgelehnt";
			}
		}

		if (betreuer == null || betreuer.isBlank()) {
			betreuer = "";
		}
		if (rueckmeldeStatus == null || rueckmeldeStatus.isBlank()) {
			rueckmeldeStatus = "offen";
		}
	}

	private void ladeAufstellung() {
		spielerListe.clear();
		for (AufstellungSpieler spieler : databaseService.ladeAufstellungSpieler(vereinnr)) {
			if (isSpielberechtig(spieler)) {
				spielerListe.add(spieler);
			}
		}

		spielerListe.sort(Comparator.comparing(AufstellungSpieler::getRang, this::compareRang));
	}

	private boolean isSpielberechtig(AufstellungSpieler spieler) {
		if (liga.startsWith(spieler.getMannschaft())) {
			String wir;
			if (spielInfo.getHeim().contains(ConfigManager.getSpielplanVerein(vereinnr))) {
				wir = spielInfo.getHeim();
			} else {
				wir = spielInfo.getGast();
			}
			if (extrahiereZahlVorPunkt(spieler.getRang()) >= extrahiereRoemischeZahl(wir)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public int extrahiereZahlVorPunkt(String text) {
		if (text == null || text.isBlank()) {
			return -1;
		}

		int pos = text.indexOf(".");
		if (pos == -1) {
			return -1;
		}

		try {
			return Integer.parseInt(text.substring(0, pos));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public int extrahiereRoemischeZahl(String text) {
		if (text == null || text.isBlank()) {
			return 1;
		}

		// römische Zahl am Ende suchen
		String[] teile = text.trim().split(" ");
		String letztesWort = teile[teile.length - 1].toUpperCase();

		int wert = roemischZuInt(letztesWort);
		return wert == -1 ? 1 : wert;
	}

	private int roemischZuInt(String roman) {
		switch (roman) {
		case "I":
			return 1;
		case "II":
			return 2;
		case "III":
			return 3;
		case "IV":
			return 4;
		case "V":
			return 5;
		case "VI":
			return 6;
		case "VII":
			return 7;
		case "VIII":
			return 8;
		case "IX":
			return 9;
		case "X":
			return 10;
		default:
			return -1;
		}
	}

	private void ladeAuswahl() {
		selectedSpielerKeys.clear();
		spielerAusgewaehlt.clear();
		spielerKommentare.clear();
		for (Map<String, Object> row : databaseService.ladeSpielerAuswahl(uniqueKey, vereinnr)) {
			String rang = (String) row.get("rang");
			String name = (String) row.get("name");
			String key = (rang == null ? "" : rang) + "|" + (name == null ? "" : name);
			boolean ausgewaehlt = Boolean.TRUE.equals(row.get("ausgewaehlt"));
			spielerAusgewaehlt.put(key, ausgewaehlt);
			spielerKommentare.put(key, (String) row.get("kommentar"));
			if (ausgewaehlt) {
				selectedSpielerKeys.add(key);
			}
		}

		for (AufstellungSpieler spieler : spielerListe) {
			spielerAusgewaehlt.putIfAbsent(spieler.getKey(), Boolean.FALSE);
			spielerKommentare.putIfAbsent(spieler.getKey(), "");
		}
	}

	private void ladeSpielKommentar() {
		spielKommentar = databaseService.ladeSpielKommentar(uniqueKey, vereinnr);
		if (spielKommentar == null) {
			spielKommentar = "";
		}
	}

	public void speichern() {
		speichernIntern(true);
	}

	public void speichernAutomatisch() {
		speichernIntern(false);
	}

	private void speichernIntern(boolean zeigeErfolgsmeldung) {

		selectedSpielerKeys.clear();
		for (AufstellungSpieler spieler : spielerListe) {
			if (Boolean.TRUE.equals(spielerAusgewaehlt.get(spieler.getKey()))) {
				selectedSpielerKeys.add(spieler.getKey());
			}

		}

		boolean gespeichert = databaseService.speichereAufstellung(uniqueKey, vereinnr, spielerListe,
				spielerAusgewaehlt, spielerKommentare, spielKommentar);
		if (gespeichert) {
			if (zeigeErfolgsmeldung) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Gespeichert", "Die Spielerauswahl wurde erfolgreich gespeichert."));
			}
		} else {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Speichern fehlgeschlagen."));
		}
	}

	public List<String> completeKommentar(String query) {
		String suchtext = query == null ? "" : query.trim().toLowerCase();
		return STANDARD_KOMMENTARE.stream().filter(k -> suchtext.isBlank() || k.toLowerCase().contains(suchtext))
				.collect(Collectors.toList());
	}

	public List<String> getStandardKommentare() {
		return STANDARD_KOMMENTARE;
	}

	private int compareRang(String links, String rechts) {
		if (links == null) {
			return rechts == null ? 0 : 1;
		}
		if (rechts == null) {
			return -1;
		}
		String[] l = links.split("\\.");
		String[] r = rechts.split("\\.");
		int max = Math.max(l.length, r.length);
		for (int i = 0; i < max; i++) {
			int li = i < l.length ? parseTeil(l[i]) : 0;
			int ri = i < r.length ? parseTeil(r[i]) : 0;
			if (li != ri) {
				return Integer.compare(li, ri);
			}
		}
		return links.compareTo(rechts);
	}

	private int parseTeil(String teil) {
		try {
			return Integer.parseInt(teil);
		} catch (NumberFormatException ex) {
			return Integer.MAX_VALUE;
		}
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public String getUniqueKey() {
		return uniqueKey;
	}

	public String getRuecksprung() {
		return ruecksprung;
	}

	public String ruecksprung() {
		return ruecksprung;
	}

	public String getBetreuer() {
		return betreuer;
	}

	public String getRueckmeldeStatus() {
		return rueckmeldeStatus;
	}

	public AufstellungSpielInfo getSpielInfo() {
		return spielInfo;
	}

	public List<AufstellungSpieler> getSpielerListe() {
		return spielerListe;
	}

	public List<String> getSelectedSpielerKeys() {
		return selectedSpielerKeys;
	}

	public void setSelectedSpielerKeys(List<String> selectedSpielerKeys) {
		this.selectedSpielerKeys.clear();
		if (selectedSpielerKeys != null) {
			this.selectedSpielerKeys.addAll(selectedSpielerKeys);
		}
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public Map<String, Boolean> getSpielerAusgewaehlt() {
		return spielerAusgewaehlt;
	}

	public Map<String, String> getSpielerKommentare() {
		return spielerKommentare;
	}

	public String getSpielKommentar() {
		return spielKommentar;
	}

	public void setSpielKommentar(String spielKommentar) {
		this.spielKommentar = spielKommentar;
	}

	public String getSpielerVerfuegbarkeitHinweis(String key) {
		return spielerVerfuegbarkeitHinweis.getOrDefault(key, "");
	}

	public String spielerVerfuegbarkeitHinweis(String key) {
		return spielerVerfuegbarkeitHinweis.getOrDefault(key, "");
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}
}
