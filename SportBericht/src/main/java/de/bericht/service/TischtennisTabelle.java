package de.bericht.service;

public class TischtennisTabelle extends Tabelle {
	private String tendenz;
	private String sieg;
	private String unentschieden;
	private String niederlagen;
	private String spiele;
	private String plus;

	public TischtennisTabelle(String tendenz, String rang, String mannschaft, String beg, String sieg,
			String unentschieden, String niederlagen, String spiele, String plus, String punkte) {
		this.tendenz = tendenz;
		this.sieg = sieg;
		this.unentschieden = unentschieden;
		this.niederlagen = niederlagen;
		this.spiele = spiele;
		this.plus = plus;
		this.rang = rang;
		this.mannschaft = mannschaft;
		this.beg = beg;
		this.punkte = punkte;

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

	public String getTendenz() {
		return tendenz;
	}

	public void setTendenz(String tendenz) {
		this.tendenz = tendenz;
	}

	@Override
	public int compareTo(Tabelle arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

}
