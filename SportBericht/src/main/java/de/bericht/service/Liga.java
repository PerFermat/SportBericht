package de.bericht.service;

import de.bericht.util.TennisGruppeKurz;

public class Liga {
	String runde;
	String mannschaft;
	String mannschaftsführer;
	String liga;
	String gruppeUrl;
	String rang;
	String punkte;

	public Liga(String runde, String mannschaft, String mannschaftsführer, String liga, String gruppeUrl, String rang,
			String punkte) {
		super();
		this.runde = runde;
		this.mannschaft = mannschaft;
		this.mannschaftsführer = mannschaftsführer;
		this.liga = liga;
		this.gruppeUrl = gruppeUrl;
		this.rang = rang;
		this.punkte = punkte;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public String getMannschaftsführer() {
		return mannschaftsführer;
	}

	public String getLiga() {
		return liga;
	}

	public String getLigaKurz() {
		return TennisGruppeKurz.kuerzeGruppe(liga);
	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public String getRang() {
		return rang;
	}

	public String getPunkte() {
		return punkte;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public void setMannschaftsführer(String mannschaftsführer) {
		this.mannschaftsführer = mannschaftsführer;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public void setPunkte(String punkte) {
		this.punkte = punkte;
	}

	public String getRunde() {
		return runde;
	}

	public void setRunde(String runde) {
		this.runde = runde;
	}

}
