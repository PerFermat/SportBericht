package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.provider.TabellenProvider;
import de.bericht.util.WebCache;

public class TennisTabellenService implements TabellenProvider {

	private String url;
	private List<Tabelle> tabelle = new ArrayList<>();

	public TennisTabellenService(String url) {
		this.url = url;
		generiereTabelle();
	}

	public void generiereTabelle() {

		try {
			Document doc = WebCache.getPage(url);
			Elements tables = doc.select("table"); // Alle Tabellen auswählen
			if (tables.size() >= 2) { // Prüfen, ob mindestens zwei Tabellen vorhanden sind
				Element secondTable = tables.get(0); // Passt wirklich

				if (secondTable != null) {
					Elements rows = secondTable.select("tbody tr"); // Zeilen innerhalb der ersten Tabelle
					int rangIndex = -1;
					int mannschaftIndex = -1;
					int begIndex = -1;
					int matchesIndex = -1;
					int saetzeIndex = -1;
					int gamesIndex = -1;
					int punkteIndex = -1;
					Elements headers = secondTable.select("th");
					// Spaltenindizes ermitteln
					for (int i = 0; i < headers.size(); i++) {

						String headerText = headers.get(i).text();
						if (headerText.equals("Rang")) {
							rangIndex = i;
						} else if (headerText.equals("Mannschaft")) {
							mannschaftIndex = i;
						} else if (headerText.equals("Begegnungen")) {
							begIndex = i;
						} else if (headerText.equals("Matches")) {
							matchesIndex = i;
						} else if (headerText.equals("Sätze")) {
							saetzeIndex = i;
						} else if (headerText.equals("Games")) {
							gamesIndex = i;
						} else if (headerText.equals("Punkte")) {
							punkteIndex = i;
						}
					}

					for (Element row : rows) {
						Elements cols = row.select("td"); // Alle Spalten der Zeile

						if (cols.size() >= 5) {

							String rang = cols.get(rangIndex).text();
							String mannschaft = cols.get(mannschaftIndex).text();
							String beg = cols.get(begIndex).text();
							String punkte = cols.get(punkteIndex).text();
							String matches = cols.get(matchesIndex).text();
							String saetze = cols.get(saetzeIndex).text();
							String games = cols.get(gamesIndex).text();

							tabelle.add(new TennisTabelle(rang, mannschaft, beg, matches, saetze, games, punkte));
						}
					}
				}
			}
		} catch (

		IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public List<Tabelle> getTabelle() {
		return tabelle;
	}

	@Override
	public String ausgabe(List<Tabelle> spiele) {
		// String-Variable, um alle Spiele zu speichern
		StringBuilder tabelleListe = new StringBuilder();

		// Alle Spiele ausgeben und in String-Variable speichern
		for (Tabelle spiel : spiele) {
			tabelleListe.append(spiel.getRang()).append(" - ");
			tabelleListe.append(spiel.getMannschaft()).append(" - ");
			tabelleListe.append(spiel.getBeg()).append(" - ");
			tabelleListe.append(spiel.getPunkte()).append("\n");
		}

		return tabelleListe.toString();
	}
}