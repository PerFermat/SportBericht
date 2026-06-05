package de.bericht.service;

import java.io.Serializable;
import java.time.LocalDate;

public class Hallenbelegung implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String tag;
	private final String wochentag;
	private final String text;
	private final String titel;
	private final LocalDate datum;

	public Hallenbelegung(String tag, String wochentag, String titel, String text) {
		this.tag = tag;
		this.wochentag = wochentag;
		this.titel = titel;
		this.text = text;
		this.datum = null;
	}

	public Hallenbelegung(LocalDate datum, String titel, String text) {
		this.tag = "";
		this.wochentag = "";
		this.datum = datum;
		this.titel = titel;
		System.out.println("Datenbank " + this.datum + " " + this.titel);
		this.text = text;
	}

	public String getTag() {
		return tag;
	}

	public String getWochentag() {
		return wochentag;
	}

	public String getTitel() {
		return titel;
	}

	public String getText() {
		return text;
	}

	public LocalDate getDatum() {
		return datum;
	}
}