package de.bericht.util;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchSummary {

	@JsonProperty("Bezirk")
	private String bezirk;

	@JsonProperty("Saison")
	private String saison;

	@JsonProperty("Liga")
	private String liga;

	@JsonProperty("Heimmannschaft")
	private String heimmannschaft;

	@JsonProperty("Gastmannschaft")
	private String gastmannschaft;

	@JsonProperty("Ergebnis")
	private String ergebnis;

	// Neue, KI-freundliche Felder
	@JsonProperty("berichtMannschaft")
	private String berichtMannschaft;

	@JsonProperty("berichtMannschaft_ist_Heim")
	private boolean berichtMannschaftIstHeim;

	@JsonProperty("punkte_Heimmannschaft")
	private int punkteHeimmannschaft;

	@JsonProperty("punkte_Gastmannschaft")
	private int punkteGastmannschaft;

	@JsonProperty("punkte_berichtMannschaft")
	private int punkteBerichtMannschaft;

	@JsonProperty("punkte_Gegner")
	private int punkteGegner;

	@JsonProperty("spielentscheidung")
	private String spielEntscheidung;

	@JsonProperty("spiel_Beginn")
	private String spielBeginn;

	@JsonProperty("spiel_Ende")
	private String spielEnde;

	@JsonProperty("Spiele")
	private List<MatchErgebnis> spiele;

	public MatchSummary(String berichtMannschaft, String heimmannschaft, String gastmannschaft, String bezirk,
			String saison, String liga, String ergebnis, String spielBeginn, String spielEnde) {

		this.saison = saison;
		this.bezirk = bezirk;
		this.liga = liga;
		this.berichtMannschaft = berichtMannschaft;
		this.heimmannschaft = heimmannschaft;
		this.gastmannschaft = gastmannschaft;
		this.ergebnis = ergebnis;
		this.spielBeginn = spielBeginn;
		this.spielEnde = spielEnde;

		this.analyseErgebnis();
	}

	private void analyseErgebnis() {

		// Schritt 2: Ergebnis auslesen
		try {
			String reinesErgebnis = ergebnis.split(" ")[0]; // Nur "6:3" behalten
			String[] parts = reinesErgebnis.split(":");
			this.punkteHeimmannschaft = Integer.parseInt(parts[0].trim());
			this.punkteGastmannschaft = Integer.parseInt(parts[1].trim());
		} catch (Exception e) {
			this.punkteHeimmannschaft = -1;
			this.punkteGastmannschaft = -1;
			this.punkteBerichtMannschaft = -1;
			this.punkteGegner = -1;
			this.spielEntscheidung = "Unbekannt";
			return;
		}

		// Schritt 3: Prüfen, ob berichtMannschaft Heim oder Gast ist
		berichtMannschaftIstHeim = heimmannschaft.toLowerCase().contains(berichtMannschaft.toLowerCase());

		if (berichtMannschaftIstHeim) {
			punkteBerichtMannschaft = punkteHeimmannschaft;
			punkteGegner = punkteGastmannschaft;
		} else {
			punkteBerichtMannschaft = punkteGastmannschaft;
			punkteGegner = punkteHeimmannschaft;
		}

		// Schritt 4: Spielausgang bestimmen
		if (punkteBerichtMannschaft > punkteGegner) {
			spielEntscheidung = "Sieg_BerichtMannschaft";
		} else if (punkteBerichtMannschaft < punkteGegner) {
			spielEntscheidung = "Sieg_Gegner";
		} else {
			spielEntscheidung = "Unentschieden";
		}

	}

	public List<MatchErgebnis> getSpiele() {
		return spiele;
	}

	public void setSpiele(List<MatchErgebnis> spiele) {
		this.spiele = spiele;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
		this.analyseErgebnis();
	}

	public String getHeimmannschaft() {
		return heimmannschaft;
	}

	public String getGastmannschaft() {
		return gastmannschaft;
	}

	public void setHeimmannschaft(String heimmannschaft) {
		this.heimmannschaft = heimmannschaft;
	}

	public void setGastmannschaft(String gastmannschaft) {
		this.gastmannschaft = gastmannschaft;
	}

	public String getSpielBeginn() {
		return spielBeginn;
	}

	public String getSpielEnde() {
		return spielEnde;
	}

	public void setSpielBeginn(String spielBeginn) {
		this.spielBeginn = spielBeginn;
	}

	public void setSpielEnde(String spielEnde) {
		this.spielEnde = spielEnde;
	}
}
