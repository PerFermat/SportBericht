package de.bericht.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import de.bericht.util.ConfigManager;

public class TennisSpiel extends Spiel {

	private String titel;
	private String datumGesamt;
	private String wochentag;
	private LocalDate datum;
	private String uhrzeit;
	private String gruppe;
	private String heimLink;
	private String gastLink;
	private String spielort;
	private String matches;
	private String saetze;
	private String games;
	private List<LogEntry> logEntries;
	private static ConfigManager config;

	public TennisSpiel(String vereinnr, String titel, String datumGesamt, String heim, String heimLink, String gast,
			String gastLink, String spielort, String matches, String saetze, String games, String ergebnisLink,
			boolean bericht) {
		this.vereinnr = vereinnr;
		this.titel = titel;
		this.datumGesamt = datumGesamt;
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

		datumAufbereiten(datumGesamt);

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

	public void datumAufbereiten(String datumGesamt) {
		if (datumGesamt == null || datumGesamt.isBlank()) {
			return;
		}

		String[] teile = datumGesamt.split(" ");
		if (teile.length < 3) {
			return;
		}

		String datumsString = teile[1];
		DateTimeFormatter[] formatters = { DateTimeFormatter.ofPattern("d.MM.yyyy"),
				DateTimeFormatter.ofPattern("dd.MM.yyyy"), DateTimeFormatter.ofPattern("d.M.yyyy"),
				DateTimeFormatter.ofPattern("dd.M.yyyy") };

		for (DateTimeFormatter formatter : formatters) {
			try {
				datum = LocalDate.parse(datumsString, formatter);
				break;
			} catch (DateTimeParseException e) {
				// nächstes Format versuchen
			}
		}

		if (datum != null) {
			wochentag = teile[0].replace(",", "");
			uhrzeit = teile[2];
		}
	}

	public String getTitel() {
		return titel;
	}

	public String getDatumGesamt() {
		return datumGesamt;
	}

	public String getWochentag() {
		return wochentag;
	}

	public String getDatumAuf() {
		if (datum == null) {
			return "";
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		return datum.format(formatter);
	}

	public LocalDate getDatum() {
		return datum;
	}

	public String getUhrzeit() {
		return uhrzeit;
	}

	public String getGruppe() {
		return gruppe;
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
	public String getDatumAnzeige() {
		return getDatumAuf();
	}

	@Override
	public String getZeitAnzeige() {
		return uhrzeit;
	}

	@Override
	public String getLiga() {
		return gruppe;
	}

	@Override
	public String getLigaJugend() {
		if (gruppe == null) {
			return "";
		}
		if (gruppe.matches("^J\\s\\d{2}.*")) {
			String altersklasse = gruppe.substring(2, 4);
			return "Jugend U" + altersklasse;
		} else if (gruppe.matches("^M\\s\\d{2}.*")) {
			String altersklasse = gruppe.substring(2, 4);
			return "Mädchen U" + altersklasse;
		} else {
			return "";
		}
	}

	public void setGruppe(String gruppe) {
		this.gruppe = gruppe;
	}
}