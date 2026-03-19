package de.bericht.service;

public class SpielcodeEintrag {

	private String liga;
	private String datum;
	private String zeit;
	private String heim;
	private String gast;
	private String spielCode;
	private String pin;
	private boolean spielcodeGefunden;

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getDatum() {
		return datum;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public String getZeit() {
		return zeit;
	}

	public void setZeit(String zeit) {
		this.zeit = zeit;
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

	public String getSpielCode() {
		return spielCode;
	}

	public void setSpielCode(String spielCode) {
		this.spielCode = spielCode;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public boolean isSpielcodeGefunden() {
		return spielcodeGefunden;
	}

	public void setSpielcodeGefunden(boolean spielcodeGefunden) {
		this.spielcodeGefunden = spielcodeGefunden;
	}
}
