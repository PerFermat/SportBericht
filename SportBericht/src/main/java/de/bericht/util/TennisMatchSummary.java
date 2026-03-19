package de.bericht.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TennisMatchSummary extends MatchSummary {

	@JsonProperty("Gesamt_Saetze")
	private String gesamtSaetze;

	@JsonProperty("Gesamt_Games")
	private String gesamtGames;

	public TennisMatchSummary(String berichtMannschaft, String heimmannschaft, String gastmannschaft, String bezirk,
			String saison, String liga, String ergebnis, String spielBeginn, String spielEnde, String gesamtSaetze,
			String gesamtGames) {
		super("Tennis", berichtMannschaft, heimmannschaft, gastmannschaft, bezirk, saison, liga, ergebnis, spielBeginn,
				spielEnde);
		this.gesamtSaetze = gesamtSaetze;
		this.gesamtGames = gesamtGames;
	}

	public String getGesamtSaetze() {
		return gesamtSaetze;
	}

	public String getGesamtGames() {
		return gesamtGames;
	}

	public void setGesamtSaetze(String gesamtSaetze) {
		this.gesamtSaetze = gesamtSaetze;
	}

	public void setGesamtGames(String gesamtGames) {
		this.gesamtGames = gesamtGames;
	}
}