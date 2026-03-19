package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.util.ConfigManager;
import de.bericht.util.WebCache;

public class MannschaftService {
	List<Mannschaft> mannschaften;

	public MannschaftService(String vereinnr) {

		String url = ConfigManager.getSpielplanURL(vereinnr);
		getMannschaften(url.replace("spielplan", "mannschaften"));
	}

	public void getMannschaften(String url) {

		mannschaften = new ArrayList<>();

		try {
			Document doc = WebCache.getPage(url);
			Elements firstTable = doc.select("table"); // Alle Tabellen auswählen
			// Element firstTable = tables.get(0);

			if (firstTable != null) {
				Elements rows = firstTable.select("tbody tr"); // Zeilen innerhalb der ersten Tabelle
				int mannschaftIndex = -1;
				int ligaIndex = -1;
				int rangIndex = -1;
				int punkteIndex = -1;
				Elements headers = firstTable.select("th");
				// Spaltenindizes ermitteln
				for (int i = 0; i < headers.size(); i++) {

					String headerText = headers.get(i).text();
					if (headerText.equals("Tab. Rang")) {
						rangIndex = i;
					} else if (headerText.equals("Mannschaft")) {
						mannschaftIndex = i;
					} else if (headerText.equals("Liga")) {
						ligaIndex = i;
					} else if (headerText.equals("Punkte")) {
						punkteIndex = i;
					}
				}

				for (Element row : rows) {
					Elements cols = row.select("td"); // Alle Spalten der Zeile

					if (cols.size() >= 3) {

						String mannschaft = cols.get(mannschaftIndex).text();
						String liga = cols.get(ligaIndex).text();
						String rang = cols.get(rangIndex).text();
						String punkte = cols.get(punkteIndex).text();

						// Ergebnis-Link extrahieren (falls vorhanden)
						Element ergebnisElement = cols.get(mannschaftIndex).selectFirst("a");
						String mannschaftUrl = (ergebnisElement != null) ? ergebnisElement.absUrl("href") : null;
						ergebnisElement = cols.get(ligaIndex).selectFirst("a");
						String ligaUrl = (ergebnisElement != null) ? ergebnisElement.absUrl("href") : null;

						mannschaften.add(new Mannschaft(mannschaft, mannschaftUrl, liga, ligaUrl, rang, punkte));
					}
				}
			}
		} catch (

		IOException e) {
			e.printStackTrace();
		}

	}

	public String ausgabe(List<Mannschaft> mannschaften) {
		// String-Variable, um alle Spiele zu speichern
		StringBuilder tabelleListe = new StringBuilder();

		// Alle Spiele ausgeben und in String-Variable speichern
		for (Mannschaft spiel : mannschaften) {
			tabelleListe.append(spiel.getMannschaft()).append(" - ");
			tabelleListe.append(spiel.getBilanzenUrl()).append(" - ");
			tabelleListe.append(spiel.getLiga()).append(" - ");
			tabelleListe.append(spiel.getLigaKurz()).append(" - ");
			tabelleListe.append(spiel.getRang()).append(" - ");
			tabelleListe.append(spiel.getPunkte()).append(" \n ");
			tabelleListe.append(spiel.getSpielplanUrl()).append(" - ");
		}

		return tabelleListe.toString();
	}

	public List<Mannschaft> getMannschaften() {
		return mannschaften;
	}

	public void setMannschaften(List<Mannschaft> mannschaften) {
		this.mannschaften = mannschaften;
	}
}
