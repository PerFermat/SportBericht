package de.bericht.service;

import de.bericht.util.SpielDetail;

public class TennisEinzelErgebnis implements SpielDetail {

	private String heim;
	private String gast;
	private String satz1;
	private String satz2;
	private String satz3;
	private String matches;
	private String saetze;
	private String games;

	public TennisEinzelErgebnis(String heim, String gast, String satz1, String satz2, String satz3, String matches,
			String saetze, String games) {
		this.heim = heim;
		this.gast = gast;
		this.satz1 = satz1;
		this.satz2 = satz2;
		this.satz3 = satz3;
		this.matches = matches;
		this.saetze = saetze;
		this.games = games;
	}

	public String getHeim() {
		return heim;
	}

	public String getGast() {
		return gast;
	}

	public String getSatz1() {
		return satz1;
	}

	public String getSatz2() {
		return satz2;
	}

	public String getSatz3() {
		return satz3;
	}

	public String getMatches() {
		return matches;
	}

	public String getSaetze() {
		return saetze;
	}

	public String getGames() {
		return games;
	}

	@Override
	public String getPosition() {
		return "Einzel";
	}
}