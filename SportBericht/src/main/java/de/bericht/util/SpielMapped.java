package de.bericht.util;

import de.bericht.service.Spiel;

public class SpielMapped {
	private String datum;
	private String liga;
	private String heim;
	private String gast;
	private String ergebnis;

	private String punkteHeim;
	private String punkteGast;
	private String kommentar;
	private String sieger;
	private String verlierer;

	public SpielMapped(Spiel original) {
		this.datum = original.getDatum();
		this.liga = original.getLiga();
		this.heim = original.getHeim();
		this.gast = original.getGast();
		this.ergebnis = original.getErgebnis();

		parseErgebnis(original.getErgebnis(), original.getHeim(), original.getGast());
	}

	private void parseErgebnis(String ergebnis, String heim, String gast) {
		if (ergebnis == null || ergebnis.isEmpty()) {
			punkteHeim = "";
			punkteGast = "";
			kommentar = "";
			sieger = "";
			verlierer = "";
			return;
		}

		// Fall: "9:0 NA"
		if (ergebnis.contains("NA")) {
			String[] teile = ergebnis.split(":");
			if (teile.length >= 2) {
				punkteHeim = teile[0].trim();
				punkteGast = teile[1].replace("NA", "").trim();
			} else {
				punkteHeim = "";
				punkteGast = "";
			}
			kommentar = "Nicht Angetreten";
			sieger = heim;
			verlierer = gast;
		} else if (ergebnis.contains("W")) {
			String[] teile = ergebnis.split(":");
			if (teile.length >= 2) {
				punkteHeim = teile[0].trim();
				punkteGast = teile[1].replace("W", "").trim();
			} else {
				punkteHeim = "";
				punkteGast = "";
			}
			kommentar = "Strafwertung";
			sieger = heim;
			verlierer = gast;
		}
		// Fall: "8:8" oder andere reguläre Ergebnisse
		else if (ergebnis.contains(":")) {
			String[] teile = ergebnis.split(":");
			punkteHeim = teile[0].trim();
			String punkteGastmitRest = teile[1].trim();
			String[] punkteGastArray = punkteGastmitRest.split(" ");
			punkteGast = punkteGastArray[0].trim();
			if (punkteGastArray.length > 1) {
				kommentar = punkteGastArray[1].trim();
			}

			if (punkteHeim.equals(punkteGast)) {
				sieger = "unentschieden";
				verlierer = "unentschieden";
				kommentar = "";
			} else {
				boolean heimGewinnt = Integer.parseInt(punkteHeim) > Integer.parseInt(punkteGast);
				sieger = heimGewinnt ? heim : gast;
				verlierer = heimGewinnt ? gast : heim;
				kommentar = "";
			}
		}
	}

	public String getDatum() {
		return datum;
	}

	public String getLiga() {
		return liga;
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

	public String getPunkteHeim() {
		return punkteHeim;
	}

	public String getPunkteGast() {
		return punkteGast;
	}

	public String getKommentar() {
		return kommentar;
	}

	public String getSieger() {
		return sieger;
	}

	public String getVerlierer() {
		return verlierer;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public void setLiga(String liga) {
		this.liga = liga;
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

	public void setPunkteHeim(String punkteHeim) {
		this.punkteHeim = punkteHeim;
	}

	public void setPunkteGast(String punkteGast) {
		this.punkteGast = punkteGast;
	}

	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}

	public void setSieger(String sieger) {
		this.sieger = sieger;
	}

	public void setVerlierer(String verlierer) {
		this.verlierer = verlierer;
	}

	// Getter & Setter (kannst du automatisch generieren lassen)
}
