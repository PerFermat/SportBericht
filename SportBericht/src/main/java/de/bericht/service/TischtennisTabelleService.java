package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.provider.TabellenProvider;
import de.bericht.util.WebCache;

public class TischtennisTabelleService implements TabellenProvider {

	private String url;
	List<Tabelle> tabelle = new ArrayList<>();

	public TischtennisTabelleService(String url) {
		this.url = url;
		generiereTabelle();
	}

	public void generiereTabelle() {

		try {
			Document doc = WebCache.getPage(url);
			Elements tables = doc.select("table"); // Alle Tabellen auswählen
			if (tables.size() >= 2) { // Prüfen, ob mindestens zwei Tabellen vorhanden sind
				Element secondTable = tables.get(1); // Zweite Tabelle auswählen (Index 1)

				if (secondTable != null) {
					Elements rows = secondTable.select("tbody tr"); // Zeilen innerhalb der ersten Tabelle
					int rangIndex = -1;
					int mannschaftIndex = -1;
					int begIndex = -1;
					int sIndex = -1;
					int uIndex = -1;
					int nIndex = -1;
					int spieleIndex = -1;
					int plusIndex = -1;
					int punkteIndex = -1;
					Elements headers = secondTable.select("th");
					// Spaltenindizes ermitteln
					for (int i = 0; i < headers.size(); i++) {

						String headerText = headers.get(i).text();
						if (headerText.equals("Rang")) {
							rangIndex = i;
						} else if (headerText.equals("Mannschaft")) {
							mannschaftIndex = i;
						} else if (headerText.equals("Beg.")) {
							begIndex = i;
						} else if (headerText.equals("S")) {
							sIndex = i;
						} else if (headerText.equals("U")) {
							uIndex = i;
						} else if (headerText.equals("N")) {
							nIndex = i;
						} else if (headerText.equals("Spiele")) {
							spieleIndex = i;
						} else if (headerText.equals("+/-")) {
							plusIndex = i;
						} else if (headerText.equals("Punkte")) {
							punkteIndex = i;
						}
					}

					for (Element row : rows) {
						Elements cols = row.select("td"); // Alle Spalten der Zeile

						if (cols.size() >= 8) {

							String rang = cols.get(rangIndex).text();

							// Optional: Symbol (#rise oder #fall) finden
							Element use = row.selectFirst("use");
							String aufAb = "";

							if (use != null) {
								String href = use.attr("href");
								if (href.contains("#rise")) {
									aufAb = "Aufstieg";
								} else if (href.contains("#fall")) {
									aufAb = "Abstieg";
								}
							}

							String mannschaft = cols.get(mannschaftIndex).text();
							String beg = cols.get(begIndex).text();
							String sieg = cols.get(sIndex).text();
							String unentschieden = cols.get(uIndex).text();
							String niederlagen = cols.get(nIndex).text();
							String spiele = cols.get(spieleIndex).text();
							String plus = cols.get(plusIndex).text();
							String punkte = cols.get(punkteIndex).text();

							tabelle.add(new TischtennisTabelle(aufAb, rang, mannschaft, beg, sieg, unentschieden,
									niederlagen, spiele, plus, punkte));
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
	public String ausgabe(List<Tabelle> tabelle) {
		// String-Variable, um alle Spiele zu speichern
		StringBuilder tabelleListe = new StringBuilder();

		// Alle Spiele ausgeben und in String-Variable speichern
		for (Tabelle spiel : tabelle) {
			tabelleListe.append(spiel.getRang()).append(" - ");
			tabelleListe.append(spiel.getMannschaft()).append(" - ");
			tabelleListe.append(spiel.getBeg()).append(" - ");
			tabelleListe.append(spiel.getPunkte()).append("\n");
		}

		return tabelleListe.toString();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public List<Tabelle> getTabelle() {
		return tabelle;
	}

}