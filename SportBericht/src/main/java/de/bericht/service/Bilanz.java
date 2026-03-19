package de.bericht.service;

public class Bilanz {

	private String rang;
	private String aufgestelltInMannschaft;
	private String position;
	private String name;
	private String einsaetze;
	private String position1;
	private String position2;
	private String position3;
	private String position4;
	private String position5;
	private String position6;
	private String gesamt;

	public Bilanz(String rang, String name, String einsaetze, String position1, String position2, String position3,
			String position4, String position5, String position6, String gesamt) {
		super();
		this.rang = rang;
		if (rang != null && rang.contains(".")) {
			String[] teile = rang.split("\\.");
			this.aufgestelltInMannschaft = teile[0];
			this.position = teile[1];
		} else {
			this.aufgestelltInMannschaft = "";
			this.position = "";
		}

		this.name = name;
		this.einsaetze = einsaetze;
		this.position1 = position1;
		this.position2 = position2;
		this.position3 = position3;
		this.position4 = position4;
		this.position5 = position5;
		this.position6 = position6;
		this.gesamt = gesamt;
	}

	public String getRang() {
		return rang;
	}

	public String getName() {
		return name;
	}

	public String getEinsaetze() {
		return einsaetze;
	}

	public String getPosition1() {
		return position1;
	}

	public String getPosition2() {
		return position2;
	}

	public String getPosition3() {
		return position3;
	}

	public String getPosition4() {
		return position4;
	}

	public String getPosition5() {
		return position5;
	}

	public String getPosition6() {
		return position6;
	}

	public String getGesamt() {
		return gesamt;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEinsaetze(String einsaetze) {
		this.einsaetze = einsaetze;
	}

	public void setPosition1(String position1) {
		this.position1 = position1;
	}

	public void setPosition2(String position2) {
		this.position2 = position2;
	}

	public void setPosition3(String position3) {
		this.position3 = position3;
	}

	public void setPosition4(String position4) {
		this.position4 = position4;
	}

	public void setPosition5(String position5) {
		this.position5 = position5;
	}

	public void setPosition6(String position6) {
		this.position6 = position6;
	}

	public void setGesamt(String gesamt) {
		this.gesamt = gesamt;
	}

	public String getAufgestelltInMannschaft() {
		return aufgestelltInMannschaft;
	}

	public String getPosition() {
		return position;
	}

	public void setAufgestelltInMannschaft(String aufgestelltInMannschaft) {
		this.aufgestelltInMannschaft = aufgestelltInMannschaft;
	}

	public void setPosition(String position) {
		this.position = position;
	}

}
