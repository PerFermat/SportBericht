package de.bericht.service;

public abstract class Tabelle implements Comparable<Tabelle> {
	protected String rang;
	protected String mannschaft;
	protected String beg;
	protected String punkte;

	public String getRang() {
		return rang;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public String getBeg() {
		return beg;
	}

	public void setBeg(String beg) {
		this.beg = beg;
	}

	public String getPunkte() {
		return punkte;
	}

	public void setPunkte(String punkte) {
		this.punkte = punkte;
	}

}
