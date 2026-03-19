package de.bericht.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Spielbericht {
	@JsonProperty("Ueberschrift")
	private String ueberschrift = "";

	@JsonProperty("Erklaerung")
	private String erklaerung = "";

	@JsonProperty("Variante")
	private String variante = "";

	@JsonProperty("Stilversion")
	private String stilversion = "";

	@JsonProperty("Text")
	private String text = "";

	// Getter und Setter
	public String getVariante() {
		return variante;
	}

	public void setVariante(String variante) {
		this.variante = variante;
	}

	public String getStilversion() {
		return stilversion;
	}

	public void setStilversion(String stilversion) {
		this.stilversion = stilversion;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getErklaerung() {
		return erklaerung;
	}

	public void setErklaerung(String erklaerung) {
		this.erklaerung = erklaerung;
	}

	public String getUeberschrift() {
		return ueberschrift;
	}

	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = ueberschrift;
	}

}