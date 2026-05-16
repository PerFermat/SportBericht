package de.bericht.util;

public class SpielCode {
	private String mannschaft;
	private String liga;
	private String vereinnr;
	private String uniqueKey;
	private String wochentag;
	private String datum;
	private String uhrzeit;
	private String heimmannschaft;
	private String gastmannschaft;
	private String spielCode;
	private String pin;

	public SpielCode(String mannschaft, String liga, String wochentag, String datum, String uhrzeit,
			String heimmannschaft, String gastmannschaft, String spielCode, String pin) {

		this.mannschaft = mannschaft;
		this.liga = liga;
		this.wochentag = wochentag;
		this.datum = datum;
		this.uhrzeit = uhrzeit;
		this.heimmannschaft = heimmannschaft;
		this.gastmannschaft = gastmannschaft;
		this.spielCode = spielCode;
		this.pin = pin;
	}

	public String getDatum() {
		return datum;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public String getUhrzeit() {
		return uhrzeit;
	}

	public void setUhrzeit(String uhrzeit) {
		this.uhrzeit = uhrzeit;
	}

	public String getHeimmannschaft() {
		return heimmannschaft;
	}

	public void setHeimmannschaft(String heimmannschaft) {
		this.heimmannschaft = heimmannschaft;
	}

	public String getGastmannschaft() {
		return gastmannschaft;
	}

	public void setGastmannschaft(String gastmannschaft) {
		this.gastmannschaft = gastmannschaft;
	}

	public String getSpielCode() {
		return spielCode;
	}

	public void setSpielCode(String spielCode) {
		this.spielCode = spielCode;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public String getWochentag() {
		return wochentag;
	}

	public void setWochentag(String wochentag) {
		this.wochentag = wochentag;
	}

	public String getTyp() {
		if (pin.isEmpty()) {
			return "Spiel-Codes";
		} else {
			return "Pin";
		}
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	@Override
	public String toString() {
		return mannschaft + ";" + liga + ";" + wochentag + ";" + datum + ";" + uhrzeit + ";" + heimmannschaft + ";"
				+ gastmannschaft + ";" + spielCode + ";" + pin;

	}
}