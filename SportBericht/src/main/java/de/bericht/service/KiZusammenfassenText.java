package de.bericht.service;

public class KiZusammenfassenText {

	private String ueberschrift;
	private String text;

	public KiZusammenfassenText(String ueberschrift, String text) {
		super();
		this.ueberschrift = ueberschrift;
		this.text = text;
	}

	public String getUeberschrift() {
		return ueberschrift;
	}

	public String getText() {
		return text;
	}

	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = ueberschrift;
	}

	public void setText(String text) {
		this.text = text;
	}

}
