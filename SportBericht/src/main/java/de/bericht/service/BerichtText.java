package de.bericht.service;

import org.primefaces.model.StreamedContent;

import de.bericht.util.ErgebnisCache;

public class BerichtText {

	private String berichttext;
	private String ergebnisLink;
	private String datum;
	private String heim;
	private String gast;
	private String ergebnis;
	private String bildUrl;
	private DatabaseService dbService = new DatabaseService();
	private StreamedContent downloadBild;

	public BerichtText(String berichttext, String ergebnisLink, String datum, String heim, String gast, String ergebnis,
			String bildUrl) {
		super();
		this.berichttext = berichttext;
		this.ergebnisLink = ergebnisLink;
		this.datum = datum;
		this.heim = heim;
		this.gast = gast;
		this.ergebnis = ergebnis;
		this.bildUrl = bildUrl;
	}

	public String getBerichttext() {
		return berichttext;
	}

	public void setBerichttext(String berichttext) {
		this.berichttext = berichttext;
	}

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public String getBildUrl() {
		return bildUrl;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	public void setBildUrl(String bildUrl) {
		this.bildUrl = bildUrl;
	}

	public String getDatum() {
		return datum;
	}

	public String getHeim() {
		return heim;
	}

	public String getGast() {
		return gast;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
	}

	public void blaettle(String vereinnr) {
		// deine Logik hier, z. B. Bericht zur Bearbeitung markieren
		dbService.saveLogData(vereinnr, ergebnisLink, "", "Veröffentlichung Blättle", "J");
		ErgebnisCache.anzahl(vereinnr, "Blaettle", ergebnisLink, 0);
	}

}
