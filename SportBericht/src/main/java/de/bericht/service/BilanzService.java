package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.util.NamensSpeicher;
import de.bericht.util.WebCache;

public class BilanzService {

	public BilanzService() {
	}

	public List<Bilanz> getBilanz(String vereinnr, String spielplan, NamensSpeicher namensSpeicher) {
		return getBilanz(vereinnr, spielplan, namensSpeicher, true);
	}

	public List<Bilanz> getBilanz(String vereinnr, String spielplan, NamensSpeicher namensSpeicher,
			Boolean verschluesseln) {

		List<Bilanz> Bilanz = new ArrayList<>();

		try {
			Document doc = WebCache.getPage(spielplan);
			Elements tables = doc.select("table"); // Alle Bilanzn auswählen
			if (tables.size() >= 2) { // Prüfen, ob mindestens zwei Bilanzn vorhanden sind
				Element secondTable = tables.get(1); // Zweite Bilanz auswählen (Index 1)

				if (secondTable != null) {
					Elements rows = secondTable.select("tr");
					// Bilanz
					int rangIndex = -1;
					int nameIndex = -1;
					int einsaetzeIndex = -1;
					int position1Index = -1;
					int position2Index = -1;
					int position3Index = -1;
					int position4Index = -1;
					int position5Index = -1;
					int position6Index = -1;
					int gesamtIndex = -1;
					Elements headers = secondTable.select("th");
					// Spaltenindizes ermitteln
					for (int i = 0; i < 10; i++) {

						String headerText = headers.get(i).text();
						if (headerText.equals("Rang")) {
							rangIndex = i;
						} else if (headerText.equals("Name")) {
							nameIndex = i;
						} else if (headerText.equals("Einsätze")) {
							einsaetzeIndex = i;
						} else if (headerText.equals("1")) {
							position1Index = i;
						} else if (headerText.equals("2")) {
							position2Index = i;
						} else if (headerText.equals("3")) {
							position3Index = i;
						} else if (headerText.equals("4")) {
							position4Index = i;
						} else if (headerText.equals("5")) {
							position5Index = i;
						} else if (headerText.equals("6")) {
							position6Index = i;
						} else if (headerText.equals("Gesamt")) {
							gesamtIndex = i;
						}
					}

					for (Element row : rows) {
						Elements cols = row.select("td"); // Alle Spalten der Zeile

						if (cols.size() >= 8) {

							String rang = cols.get(rangIndex).text();
							String name = "";
							if (verschluesseln) {
								name = namensSpeicher.formatName(vereinnr, cols.get(nameIndex).text(), namensSpeicher);
							} else {
								name = cols.get(nameIndex).text();
							}
							String einsaetze = cols.get(einsaetzeIndex).text();
							String p1 = cols.get(position1Index).text();
							String p2 = cols.get(position2Index).text();
							String p3 = cols.get(position3Index).text();
							String p4 = cols.get(position4Index).text();
							String p5 = cols.get(position5Index).text();
							String p6 = cols.get(position6Index).text();
							String gesamt = cols.get(gesamtIndex).text();

							Bilanz.add(new Bilanz(rang, name, einsaetze, p1, p2, p3, p4, p5, p6, gesamt));
						} else if (cols.size() >= 5) {
							String name = "";
							if (verschluesseln) {
								name = namensSpeicher.formatName(vereinnr, cols.get(nameIndex).text(), namensSpeicher);
							} else {
								name = cols.get(nameIndex).text();
							}
							String einsaetze = cols.get(einsaetzeIndex).text();
							String gesamt = cols.get(4).text();

							Bilanz.add(new Bilanz("Doppel", name, einsaetze, "", "", "", "", "", "", gesamt));
						}
					}
				}
			}
		} catch (

		IOException e) {
			e.printStackTrace();
		}

		return Bilanz;
	}

	public String ausgabe(List<Bilanz> Bilanz) {
		// String-Variable, um alle Spiele zu speichern
		StringBuilder BilanzListe = new StringBuilder();

		// Alle Spiele ausgeben und in String-Variable speichern
		for (Bilanz spiel : Bilanz) {
			BilanzListe.append(spiel.getRang()).append(" - ");
			BilanzListe.append(spiel.getName()).append(" - ");
			BilanzListe.append(spiel.getEinsaetze()).append(" - ");
			BilanzListe.append(spiel.getGesamt()).append("\n");

		}

		return BilanzListe.toString();
	}
}