package de.bericht.service;

import java.util.List;

import de.bericht.util.ConfigManager;

public class TennisSpiel extends Spiel {

	private String titel;
	private String heimLink;
	private String gastLink;
	private String spielort;
	private String matches;
	private String saetze;
	private String games;
	private List<LogEntry> logEntries;
	private static ConfigManager config;

	public TennisSpiel(String vereinnr, String titel, String datumGesamt, String wochentag, String datum, String zeit,
			String heim, String heimLink, String gast, String gastLink, String spielort, String matches, String saetze,
			String games, String ergebnisLink, boolean bericht) {
		this.vereinnr = vereinnr;
		this.titel = titel;
		this.datumGesamt = datumGesamt;
		this.wochentag = wochentag;
		this.datum = datum;
		this.zeit = zeit;
		this.heim = heim;
		this.gast = gast;
		this.heimLink = heimLink;
		this.gastLink = gastLink;
		this.spielort = spielort;
		this.matches = matches;
		this.saetze = saetze;
		this.games = games;
		this.ergebnisLink = ergebnisLink;
		this.bericht = bericht;
		this.bild = false;

		// Für gemeinsame Anzeige kann Ergebnis = Matches sein
		this.ergebnis = matches;

		config = ConfigManager.getInstance();
		// String sortlist = config.getConfigValue(vereinnr, "bericht.sortierung");
		// setSortierungAusKonfiguration(sortlist);
	}

	private void setSortierungAusKonfiguration(String sortlist) {
		sortierung = 99;

		if (sortlist == null || sortlist.isBlank()) {
			return;
		}

		String[] texte = sortlist.split(",");
		for (int i = 0; i < texte.length; i++) {
			String eintrag = texte[i].trim();
			if (eintrag.equals(heim) || eintrag.equals(gast)) {
				sortierung = i;
				break;
			}
		}
	}

	public String getTitel() {
		return titel;
	}

	public String getHeimLink() {
		return heimLink;
	}

	public String getGastLink() {
		return gastLink;
	}

	public String getSpielort() {
		return spielort;
	}

	public String getMatches() {
		return matches;
	}

	public String getSaetze() {
		return saetze;
	}

	public String getGames() {
		return games;
	}

	public List<LogEntry> getLogEntries() {
		return logEntries;
	}

	public boolean isBeendet() {
		return !"0:0".equals(saetze);
	}

	public void bericht() {
		DatabaseService dbService = new DatabaseService();
		dbService.saveLogData(vereinnr, ergebnisLink, "", "in Bearbeitung", "");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
	}

	@Override
	public String getSportart() {
		return "tennis";
	}

	@Override
	public String getLigaJugend() {
		if (liga == null) {
			return "";
		}
		if (liga.matches("^J\\s\\d{2}.*")) {
			String altersklasse = liga.substring(2, 4);
			return "Jugend U" + altersklasse;
		} else if (liga.matches("^M\\s\\d{2}.*")) {
			String altersklasse = liga.substring(2, 4);
			return "Mädchen U" + altersklasse;
		} else if (liga.matches("^Kid")) {
			return "Kids-Cup";
		} else {
			return "";
		}
	}

}