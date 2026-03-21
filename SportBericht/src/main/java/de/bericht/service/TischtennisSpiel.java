package de.bericht.service;

import java.util.List;

import de.bericht.util.ConfigManager;

public class TischtennisSpiel extends Spiel {

	private String liga;
	private DatabaseService dbService = new DatabaseService();
	private List<LogEntry> logEntries;
	private static ConfigManager config;

	public TischtennisSpiel(String vereinnr, String datumGesamt, String wochentag, String datum, String zeit,
			String liga, String heim, String gast, String ergebnis, String ergebnisLink) {
		this.vereinnr = vereinnr;
		this.liga = liga;
		this.datumGesamt = datumGesamt;
		this.wochentag = wochentag;
		this.datum = datum;
		this.zeit = zeit;
		this.heim = heim;
		this.gast = gast;
		this.ergebnis = ergebnis;
		this.ergebnisLink = ergebnisLink;

		config = ConfigManager.getInstance();
		String sortlist = config.getConfigValue(vereinnr, "bericht.sortierung");
		setSortierungAusKonfiguration(sortlist);
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

	@Override
	public String getLiga() {
		return liga;
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
		} else {
			return "";
		}
	}

	@Override
	public String getErgebnis() {
		if ("0:0".equals(ergebnis)) {
			return "";
		}
		return ergebnis;
	}

	public void bericht() {
		dbService.saveLogData(vereinnr, ergebnisLink, "", "in Bearbeitung", "");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
	}

	public void bericht(String alt) {
		dbService.saveLogData(vereinnr, ergebnisLink, "", "in Bearbeitung", "");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
	}

	public List<LogEntry> getLogEntries() {
		return logEntries;
	}

	@Override
	public String getSportart() {
		return "tischtennis";
	}

}