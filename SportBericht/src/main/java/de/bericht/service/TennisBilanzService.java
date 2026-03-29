package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.provider.BilanzProvider;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.WebCache;

public class TennisBilanzService implements BilanzProvider {

	private String url;
	private String vereinnr;
	private List<Bilanz> bilanz = new ArrayList<>();
	private NamensSpeicher namensSpeicher;
	private Boolean verschluesseln;

	public TennisBilanzService(String vereinnr, String url, NamensSpeicher namensSpeicher, Boolean verschluesseln) {
		this.vereinnr = vereinnr;
		this.url = url;
		this.namensSpeicher = namensSpeicher;
		this.verschluesseln = verschluesseln;
		generiereBilanz();
	}

	public void generiereBilanz() {
		try {
			Document doc = WebCache.getPage(url);
			Elements tables = doc.select("table"); // Alle Bilanzn auswählen
			if (tables.size() >= 3) { // Prüfen, ob mindestens zwei Bilanzn vorhanden sind
				Element secondTable = tables.get(2); // Zweite Bilanz auswählen (Index 1)

				if (secondTable != null) {
					Elements rows = secondTable.select("tr");
					// Bilanz
					int rangIndex = -1;
					int nameIndex = -1;
					int lkIndex = -1;
					int nationIndex = -1;
					int einzelIndex = -1;
					int doppelIndex = -1;
					int gesamtIndex = -1;
					Elements headers = secondTable.select("th");
					// Spaltenindizes ermitteln
					for (int i = 0; i < 7; i++) {

						String headerText = headers.get(i).text();
						if (headerText.equals("Rang")) {
							rangIndex = i;
						} else if (headerText.equals("Name")) {
							nameIndex = i;
						} else if (headerText.equals("LK")) {
							lkIndex = i;
						} else if (headerText.equals("Nation")) {
							nationIndex = i;
						} else if (headerText.equals("Einzel")) {
							einzelIndex = i;
						} else if (headerText.equals("Doppel")) {
							doppelIndex = i;
						} else if (headerText.equals("Gesamt")) {
							gesamtIndex = i;
						}
					}

					for (Element row : rows) {
						Elements cols = row.select("td"); // Alle Spalten der Zeile

						if (cols.size() >= 6) {

							String rang = cols.get(rangIndex).text();
							String name = "";
							if (verschluesseln) {
								name = namensSpeicher.formatName(vereinnr, cols.get(nameIndex).text(), namensSpeicher);
							} else {
								name = cols.get(nameIndex).text();
							}
							String lk = cols.get(lkIndex).text();
							String nation = cols.get(nationIndex).text();
							String einzel = cols.get(einzelIndex).text();
							String doppel = cols.get(doppelIndex).text();
							String gesamt = cols.get(gesamtIndex).text();

							bilanz.add(new TennisBilanz(rang, lk, name, nation, einzel, doppel, gesamt));
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
	public String ausgabe(List<Bilanz> Bilanz) {
		// String-Variable, um alle Spiele zu speichern
		StringBuilder BilanzListe = new StringBuilder();

		// Alle Spiele ausgeben und in String-Variable speichern
		for (Bilanz spiel : Bilanz) {
			BilanzListe.append(spiel.getRang()).append(" - ");
			BilanzListe.append(spiel.getName()).append(" - ");
			BilanzListe.append(spiel.getGesamt()).append("\n");

		}

		return BilanzListe.toString();
	}

	@Override
	public List<Bilanz> getBilanz() {
		return bilanz;
	}

	public void setBilanz(List<Bilanz> bilanz) {
		this.bilanz = bilanz;
	}
}