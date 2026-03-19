package de.bericht.service;

import de.bericht.util.SpielDetail;

public class TennisDoppelErgebnis implements SpielDetail {

	private String heim1;
	private String heim2;
	private String gast1;
	private String gast2;
	private String satz1;
	private String satz2;
	private String satz3;
	private String matches;
	private String saetze;
	private String games;

	public TennisDoppelErgebnis(String heim1, String heim2, String gast1, String gast2, String satz1, String satz2,
			String satz3, String matches, String saetze, String games) {
		this.heim1 = heim1;
		this.heim2 = heim2;
		this.gast1 = gast1;
		this.gast2 = gast2;
		this.satz1 = satz1;
		this.satz2 = satz2;
		this.satz3 = satz3;
		this.matches = matches;
		this.saetze = saetze;
		this.games = games;
	}

	@Override
	public String getHeim() {
		return getHeimPaarung();
	}

	@Override
	public String getGast() {
		return getGastPaarung();
	}

	public String getHeimPaarung() {
		return heim1 + "/" + heim2;
	}

	public String getGastPaarung() {
		return gast1 + "/" + gast2;
	}

	public String getHeimPaarungMitVornamen() {
		return getVorname(heim1) + "/" + getVorname(heim2);
	}

	public String getGastPaarungMitVornamen() {
		return getVorname(gast1) + "/" + getVorname(gast2);
	}

	private String getVorname(String vollerName) {
		if (vollerName == null || vollerName.trim().isEmpty()) {
			return "";
		}
		String[] teile = vollerName.trim().split(" ");
		return teile.length > 0 ? teile[0] : vollerName;
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
		return "Doppel";
	}

}
