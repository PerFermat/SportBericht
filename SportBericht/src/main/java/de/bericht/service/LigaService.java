package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.util.WebCache;

public class LigaService {
	String verein;

	List<Liga> ligen = new ArrayList<>();

	public LigaService(String spielplan) {

		try {
			Document doc = WebCache.getPage(spielplan);
			// Überprüfen, ob der Text "Ein Fehler ist aufgetreten" im HTML-Dokument
			// vorhanden ist
			int fehlercode = 0;
			if (doc.body().text().contains("Ein Fehler ist aufgetreten")) {
				fehlercode = -1;
			} else {
				fehlercode = 0;
			}
			verein = extractVerein(doc.select("h1").first().toString());

			Elements tables = doc.select("table"); // Alle Ligan auswählen
			Elements headings = doc.select("h3");
			int tab = 0;
			for (Element firstTable : tables) {

				if (firstTable != null) {

					Elements rows = firstTable.select("tbody tr"); // Zeilen innerhalb der ersten Liga
					int mannschaftIndex = -1;
					int fuehrerIndex = -1;
					int gruppeIndex = -1;
					int rangIndex = -1;
					int punkteIndex = -1;
					Elements headers = firstTable.select("th");
					// Spaltenindizes ermitteln
					for (int i = 0; i < headers.size(); i++) {

						String headerText = headers.get(i).text();
						if (headerText.equals("Mannschaft")) {
							mannschaftIndex = i;
						} else if (headerText.equals("Mannschaftsführer")) {
							fuehrerIndex = i;
						} else if (headerText.equals("Gruppe")) {
							gruppeIndex = i;
						} else if (headerText.equals("Tab.-Rang")) {
							rangIndex = i;
						} else if (headerText.equals("Punkte")) {
							punkteIndex = i;
						}
					}
					Element heading = headings.get(tab++); // Nimm das erste gefundene Element
					String runde = heading.text();
					for (Element row : rows) {
						Elements cols = row.select("td"); // Alle Spalten der Zeile

						if (cols.size() >= 5) {

							String mannschaft = cols.get(mannschaftIndex).text();
							String fuehrer = cols.get(fuehrerIndex).text();
							String gruppe = cols.get(gruppeIndex).text();
							String rang = cols.get(rangIndex).text();
							String punkte = cols.get(punkteIndex).text();
							Element ergebnisElement = cols.get(gruppeIndex).selectFirst("a");
							String gruppeUrl = (ergebnisElement != null) ? ergebnisElement.absUrl("href") : null;

							ligen.add(new Liga(runde, mannschaft, fuehrer, gruppe, gruppeUrl, rang, punkte));
						}
					}
				}
			}
		} catch (

		IOException e) {
			e.printStackTrace();
		}

	}

	public static String extractVerein(String htmlLine) {
		Pattern pattern = Pattern.compile("<br>(.*?)<small>");
		Matcher matcher = pattern.matcher(htmlLine);

		while (matcher.find()) {
			String vereinsname = matcher.group(1).trim();
			return vereinsname;
		}
		return "";
	}

	public String ausgabe() {
		// String-Variable, um alle Spiele zu speichern
		StringBuilder LigaListe = new StringBuilder();

		// Alle Spiele ausgeben und in String-Variable speichern
		for (Liga spiel : ligen) {
			LigaListe.append(spiel.getRunde()).append(" - ");
			LigaListe.append(spiel.getMannschaft()).append(" - ");
			LigaListe.append(spiel.getMannschaftsführer()).append(" - ");
			LigaListe.append(spiel.getGruppe()).append(" - ");
			LigaListe.append(spiel.getGruppeUrl()).append(" - ");
			LigaListe.append(spiel.getRang()).append(" - ");
			LigaListe.append(spiel.getPunkte()).append(" \n");
		}

		return LigaListe.toString();
	}

	public List<Liga> getLigen() {
		return ligen;
	}

	public void setLigen(List<Liga> ligen) {
		this.ligen = ligen;
	}

	public String getVerein() {
		return verein;
	}

	public void setVerein(String verein) {
		this.verein = verein;
	}

}