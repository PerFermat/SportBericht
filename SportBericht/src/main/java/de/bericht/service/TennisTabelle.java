package de.bericht.service;

public class TennisTabelle extends Tabelle {
	private String matches;
	private String games;
	private String saetze;

	public TennisTabelle(String rang, String mannschaft, String beg, String matches, String saetze, String games,
			String punkte) {
		this.matches = matches;
		this.games = games;
		this.saetze = saetze;
		this.rang = rang;
		this.mannschaft = mannschaft;
		this.beg = beg;
		this.punkte = punkte;
	}

	public String getMatches() {
		return matches;
	}

	public String getGames() {
		return games;
	}

	public String getSaetze() {
		return saetze;
	}

	public void setMatches(String matches) {
		this.matches = matches;
	}

	public void setGames(String games) {
		this.games = games;
	}

	public void setSaetze(String saetze) {
		this.saetze = saetze;
	}

	@Override
	public int compareTo(Tabelle arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
}
