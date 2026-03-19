package de.bericht.service;

public class Liga {
	String runde;
	String mannschaft;
	String mannschaftsführer;
	String gruppe;
	String gruppeUrl;
	String rang;
	String punkte;

	public Liga(String runde, String mannschaft, String mannschaftsführer, String gruppe, String gruppeUrl, String rang,
			String punkte) {
		super();
		this.runde = runde;
		this.mannschaft = mannschaft;
		this.mannschaftsführer = mannschaftsführer;
		this.gruppe = gruppe;
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

	public String getGruppe() {
		return gruppe;
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

	public void setGruppe(String gruppe) {
		this.gruppe = gruppe;
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
