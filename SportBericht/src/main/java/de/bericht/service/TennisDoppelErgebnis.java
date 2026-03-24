package de.bericht.service;

import org.jsoup.nodes.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.bericht.util.SpielDetail;

public class TennisDoppelErgebnis implements SpielDetail {

	private String heim1;
	private String heim2;
	private String gast1;
	private String gast2;
	private TennisDoppelInfo heimInfo;
	private TennisDoppelInfo gastInfo;
	
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
		this.heimInfo = new TennisDoppelInfo(TennisSpielerInfo.parse(heim1), TennisSpielerInfo.parse(heim2));
		this.gastInfo = new TennisDoppelInfo(TennisSpielerInfo.parse(gast1), TennisSpielerInfo.parse(gast2));
		this.satz1 = satz1;
		this.satz2 = satz2;
		this.satz3 = satz3;
		this.matches = matches;
		this.saetze = saetze;
		this.games = games;
	}
	
	public TennisDoppelErgebnis(Element heimZelle, Element gastZelle, String satz1, String satz2, String satz3,
			String matches, String saetze, String games) {
		this(TennisDoppelInfo.fromCell(heimZelle).getSpieler1().getName(),
				TennisDoppelInfo.fromCell(heimZelle).getSpieler2().getName(),
				TennisDoppelInfo.fromCell(gastZelle).getSpieler1().getName(),
				TennisDoppelInfo.fromCell(gastZelle).getSpieler2().getName(), satz1, satz2, satz3, matches, saetze,
				games);
		this.heimInfo = TennisDoppelInfo.fromCell(heimZelle);
		this.gastInfo = TennisDoppelInfo.fromCell(gastZelle);
	}



	@Override
	@JsonIgnore
	public String getHeim() {
		return getHeimPaarung();
	}

	@Override
	@JsonIgnore
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

	public String getHeimPaarungMitNachnamen() {
		return getNachname(heim1) + "/" + getNachname(heim2);
	}

	public String getGastPaarungMitNachnamen() {
		return getNachname(gast1) + "/" + getNachname(gast2);
	}

	private String getVorname(String vollerName) {
		if (vollerName == null || vollerName.trim().isEmpty()) {
			return "";
		}
		String[] teile = TennisSpielerInfo.parse(vollerName).getName().split(" ");

		return teile.length > 0 ? teile[0] : vollerName;
	}

	private String getNachname(String vollerName) {
		if (vollerName == null || vollerName.trim().isEmpty()) {
			return "";
		}
		String[] teile = TennisSpielerInfo.parse(vollerName).getName().split(" ");
		return teile.length > 0 ? teile[teile.length - 1] : vollerName;
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
	@JsonIgnore
	public String getPosition() {
		return "Doppel";
	}
	@JsonIgnore
	public TennisSpielerInfo getHeimSpieler1() {
		return heimInfo == null ? TennisSpielerInfo.parse(heim1) : heimInfo.getSpieler1();
	}

	@JsonIgnore
	public TennisSpielerInfo getHeimSpieler2() {
		return heimInfo == null ? TennisSpielerInfo.parse(heim2) : heimInfo.getSpieler2();
	}

	@JsonIgnore
	public TennisSpielerInfo getGastSpieler1() {
		return gastInfo == null ? TennisSpielerInfo.parse(gast1) : gastInfo.getSpieler1();
	}

	@JsonIgnore
	public TennisSpielerInfo getGastSpieler2() {
		return gastInfo == null ? TennisSpielerInfo.parse(gast2) : gastInfo.getSpieler2();
	}


}
