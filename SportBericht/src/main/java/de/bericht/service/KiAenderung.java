package de.bericht.service;

public class KiAenderung {

	private String beschreibung;
	private String typ;
	private Integer bereich_min;
	private Integer bereich_schritt;
	private Integer bereich_max;
	private String wert_text;
	private Integer wert_zahl;
	private String platzhalter;

	public KiAenderung(String beschreibung, String typ, int bereich_min, int bereich_schritt, int bereich_max,
			String wert_text, int wert_zahl, String platzhalter) {
		super();
		this.beschreibung = beschreibung;
		this.typ = typ;
		this.bereich_min = bereich_min;
		this.bereich_schritt = bereich_schritt;
		this.bereich_max = bereich_max;
		this.wert_text = wert_text;
		this.wert_zahl = wert_zahl;
		this.platzhalter = platzhalter;
	}

	public String getBeschreibung() {
		return beschreibung;
	}

	public String getTyp() {
		return typ;
	}

	public Integer getBereich_min() {
		return bereich_min;
	}

	public Integer getBereich_schritt() {
		return bereich_schritt;
	}

	public Integer getBereich_max() {
		return bereich_max;
	}

	public String getWert_text() {
		return wert_text;
	}

	public Integer getWert_zahl() {
		return wert_zahl;
	}

	public void setBeschreibung(String beschreibung) {
		this.beschreibung = beschreibung;
	}

	public void setTyp(String typ) {
		this.typ = typ;
	}

	public void setBereich_min(Integer bereich_min) {
		this.bereich_min = bereich_min;
	}

	public void setBereich_schritt(Integer bereich_schritt) {
		this.bereich_schritt = bereich_schritt;
	}

	public void setBereich_max(Integer bereich_max) {
		this.bereich_max = bereich_max;
	}

	public void setWert_text(String wert_text) {
		this.wert_text = wert_text;
	}

	public void setWert_zahl(Integer wert_zahl) {
		this.wert_zahl = wert_zahl;
	}

	public String getPlatzhalter() {
		return platzhalter;
	}

	public void setPlatzhalter(String platzhalter) {
		this.platzhalter = platzhalter;
	}

}
