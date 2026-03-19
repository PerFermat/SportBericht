package de.bericht.service;

import java.util.Base64;

import de.bericht.util.BerichtData;

public class FreigegebeneSpiele {
	private String vereinnr;
	private String ergebnisLink;
	private String heim;
	private String gast;
	private String datum;
	private String matches;
	private String name;
	private String liga;
	private boolean wahl = true;

	public FreigegebeneSpiele(String vereinnr, String ergebnisLink, String heim, String gast, String datum,
			String matches, String name, String liga) {
		super();
		this.vereinnr = vereinnr;
		this.ergebnisLink = ergebnisLink;
		this.heim = heim;
		this.gast = gast;
		this.datum = datum;
		this.matches = matches;
		this.name = name;
		this.liga = liga;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	public String getHeim() {
		return heim;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

	public String getGast() {
		return gast;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public String getDatum() {
		return datum;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public String getMatches() {
		return matches;
	}

	public void setMatches(String matches) {
		this.matches = matches;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	// Optional: toString-Methode zur schnellen Ausgabe

	@Override
	public String toString() {
		return "FreigegebeneSpiele{" + "vereinnr='" + vereinnr + '\'' + ", ergebnisLink='" + ergebnisLink + '\''
				+ ", heim='" + heim + '\'' + ", gast='" + gast + '\'' + ", datum='" + datum + '\'' + ", matches='"
				+ matches + '\'' + ", name='" + name + '\'' + ", liga='" + liga + '\'' + '}';
	}

	public boolean isWahl() {
		return wahl;
	}

	public void setWahl(boolean wahl) {
		this.wahl = wahl;
	}

	// Gibt den Bild-Link zurück, damit h:graphicImage das Bild anzeigen kann.
	public String getBildUrl() {
		DatabaseService dbService = new DatabaseService(vereinnr);
		BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);
		if (data.getBild() != null) {
			// Speichere das geladene Bild in der Hilfseigenschaft bildDaten
			byte[] bildDaten = data.getBild();
			// Erzeuge einen Data-URI-String zur Anzeige
			return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bildDaten);
		}
		return null;
	}
}
