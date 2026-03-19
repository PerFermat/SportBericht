package de.bericht.service;

import java.io.Serializable;

public class SpielerRueckmeldung implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String name;
	private final String verfuegbarkeit;
	private final String kommentar;
	private final String rang;

	public SpielerRueckmeldung(String name, String verfuegbarkeit, String kommentar, String rang) {
		this.name = name;
		this.verfuegbarkeit = verfuegbarkeit;
		this.kommentar = kommentar;
		this.rang = rang;
	}

	public String getName() {
		return name;
	}

	public String getVerfuegbarkeit() {
		return verfuegbarkeit;
	}

	public String getKommentar() {
		return kommentar;
	}

	public String getRang() {
		return rang;
	}
}