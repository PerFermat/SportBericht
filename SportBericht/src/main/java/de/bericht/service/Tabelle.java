package de.bericht.service;

public class Tabelle {
	private String rang;
	private String tendenz;
	private String mannschaft;
	private String beg;
	private String sieg;
	private String unentschieden;
	private String niederlagen;
	private String spiele;
	private String plus;
	private String punkte;

	public Tabelle(String rang, String tendenz, String mannschaft, String beg, String sieg, String unentschieden,
			String niederlagen, String spiele, String plus, String punkte) {
		this.rang = rang;
		this.tendenz = tendenz;
		this.mannschaft = mannschaft;
		this.beg = beg;
		this.sieg = sieg;
		this.unentschieden = unentschieden;
		this.niederlagen = niederlagen;
		this.spiele = spiele;
		this.plus = plus;
		this.punkte = punkte;
	}

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

	public String getSieg() {
		return sieg;
	}

	public void setSieg(String sieg) {
		this.sieg = sieg;
	}

	public String getUnentschieden() {
		return unentschieden;
	}

	public void setUnentschieden(String unentschieden) {
		this.unentschieden = unentschieden;
	}

	public String getNiederlagen() {
		return niederlagen;
	}

	public void setNiederlagen(String niederlagen) {
		this.niederlagen = niederlagen;
	}

	public String getSpiele() {
		return spiele;
	}

	public void setSpiele(String spiele) {
		this.spiele = spiele;
	}

	public String getPlus() {
		return plus;
	}

	public void setPlus(String plus) {
		this.plus = plus;
	}

	public String getPunkte() {
		return punkte;
	}

	public void setPunkte(String punkte) {
		this.punkte = punkte;
	}

	public String getTendenz() {
		return tendenz;
	}

	public void setTendenz(String tendenz) {
		this.tendenz = tendenz;
	}

}
