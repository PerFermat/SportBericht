package de.bericht.util;

public class Fehler {
	private String fehlerhaftesWort;
	private String vorschlag;
	private String erklärung;
	private boolean button;

	public Fehler(String fehlerhaftesWort, String vorschlag, String erklärung, boolean button) {
		this.fehlerhaftesWort = fehlerhaftesWort;
		this.vorschlag = vorschlag;
		this.erklärung = erklärung;
		this.button = button;
	}

	public String getFehlerhaftesWort() {
		return fehlerhaftesWort;
	}

	public String getVorschlag() {
		return vorschlag;
	}

	public String getErklärung() {
		return erklärung;
	}

	public boolean isbutton() {
		return button;
	}
}
