package de.bericht.controller;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import de.bericht.service.DatabaseService;
import de.bericht.service.LogEntry;
import de.bericht.util.BerichtData;
import de.bericht.util.BerichtHelper;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class HistorienBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private String vereinnr;
	private String heim;
	private String gast;
	private String datum;
	private String ergebnis;
	private String name;
	private String liga;
	private String ergebnisLink;
	private String uuid;
	private String spielErgebnis;
	private List<LogEntry> logEntries;

	private BerichtData aktuellerBericht;
	private BerichtData historischerBericht;

	private List<String> historienTimestamps;
	private String ausgewaehlterTimestamp;
	private DatabaseService dbService = new DatabaseService();

	@PostConstruct
	public void init() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
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

		this.vereinnr = params.get("vereinnr");
		this.ergebnis = params.get("ergebnis");
		this.ergebnisLink = params.get("ergebnisLink");
		this.name = params.get("name");
		this.liga = params.get("liga");
		this.uuid = params.get("uuid");
		dbService.verarbeiteEintrag(vereinnr, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
		// Spielergebnisse abrufen
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
			this.spielErgebnis = dbService.loadSpielstatistik(vereinnr, ergebnisLink);
		}

		ladeAktuellenBericht();
		ladeHistorienTimestamps();

		if (!historienTimestamps.isEmpty()) {
			ausgewaehlterTimestamp = historienTimestamps.get(0); // Neuester
			ladeHistorischenBericht(ausgewaehlterTimestamp);
		}
	}

	public void ladeAktuellenBericht() {
		aktuellerBericht = dbService.loadBerichtData(vereinnr, this.ergebnisLink);
	}

	public void ladeHistorienTimestamps() {
		historienTimestamps = dbService.getHistorieTimestamps(vereinnr, this.ergebnisLink);
	}

	public void ladeHistorischenBericht(String timestamp) {
		this.historischerBericht = dbService.getBerichtDataFromHistorie(vereinnr, this.ergebnisLink, timestamp);
	}

	// Wird z. B. vom selectOneMenu ausgelöst
	public void onTimestampChanged() {
		ladeHistorischenBericht(ausgewaehlterTimestamp);
	}

	// Getter & Setter
	public BerichtData getAktuellerBericht() {
		return aktuellerBericht;
	}

	public BerichtData getHistorischerBericht() {
		return historischerBericht;
	}

	public List<String> getHistorienTimestamps() {
		return historienTimestamps;
	}

	public String getAusgewaehlterTimestamp() {
		return ausgewaehlterTimestamp;
	}

	public void setAusgewaehlterTimestamp(String ausgewaehlterTimestamp) {
		this.ausgewaehlterTimestamp = ausgewaehlterTimestamp;
	}

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	// Gibt den Spielergebnis-Text zurück (mit <br/> statt \n)
	public String getSpielErgebnis() {
		return spielErgebnis != null ? spielErgebnis.replace("\n", "<br/>") : "";
	}

	// Getter und Setter
	public String getHeim() {

		return decodeUrl(heim);
	}

	public String getGast() {
		return decodeUrl(gast);
	}

	public String getDatum() {
		return datum;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	// Diese Methode wird durch den "Speichern"-Button aufgerufen.
	public void zurueck() {
		dbService.deleteUUID(vereinnr, ergebnisLink, uuid);
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String decodeUrl(String url) {
		if (url == null) {
			return null;
		}

		if (enthaeltUmlaute(url)) {
			return url;

		}
		String enc = new String(url.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		return enc;
	}

	public static boolean enthaeltUmlaute(String text) {
		String umlaute = "äöüÄÖÜß";
		for (int i = 0; i < text.length(); i++) {
			if (umlaute.indexOf(text.charAt(i)) != -1) {
				return true;
			}
		}
		return false; // Kein Umlaut gefunden
	}

	public boolean getInBearbeitung() {
		return dbService.inBearbeitung(vereinnr, ergebnisLink, uuid);
	}

	public void updBearbeitung() {
		dbService.verarbeiteEintrag(vereinnr, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
	}

	public String getAktuellesBildUrl() {
		if (aktuellerBericht.getBild() != null) {
			return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(aktuellerBericht.getBild());
		}
		return null;
	}

	public String getHistorieBildUrl() {
		if (historischerBericht.getBild() != null) {
			return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(historischerBericht.getBild());
		}
		return null;
	}

	public String getAktuellesBildUnterschrift() {
		return aktuellerBericht.getBildUnterschrift();
	}

	public String getHistorieBildUnterschrift() {
		return historischerBericht.getBildUnterschrift();
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

	public String getAktuellerBerichtText() {
		return BerichtHelper.SAFE_HTML_POLICY.sanitize(aktuellerBericht.getBerichtText());
	}

	public String getHistorieBerichtText() {
		return BerichtHelper.SAFE_HTML_POLICY.sanitize(historischerBericht.getBerichtText());
	}

	public String getAktuelleUeberschrift() {
		return BerichtHelper.SAFE_HTML_POLICY.sanitize(aktuellerBericht.getUeberschrift());
	}

	public String getHistorieUeberschrift() {
		return BerichtHelper.SAFE_HTML_POLICY.sanitize(historischerBericht.getUeberschrift());
	}

	// Diese Methode wird durch den "Speichern"-Button aufgerufen.
	public void speichern() {
		// Speichere in der DB: Berichtstext, ergebnisLink und das Bild (als Byte[])
		dbService.saveBerichtData(vereinnr, ergebnisLink, aktuellerBericht.getBerichtText(), aktuellerBericht.getBild(),
				aktuellerBericht.getBildUnterschrift(), aktuellerBericht.getUeberschrift());
		dbService.saveLogData(vereinnr, ergebnisLink, name, "Historie widerherstellen", "");
	}

	public void uebernahmeText() {
		aktuellerBericht.setBerichtText(historischerBericht.getBerichtText());
	}

	public void uebernahmeUeberschrift() {
		aktuellerBericht.setUeberschrift(historischerBericht.getUeberschrift());
	}

	public void uebernahmeBild() {
		aktuellerBericht.setBild(historischerBericht.getBild());
	}

	public void uebernahmeBildUnterschrift() {
		aktuellerBericht.setBildUnterschrift(historischerBericht.getBildUnterschrift());
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}
}
