package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.util.WebCache;

public class AufstellungService {
	List<Aufstellung> aufstellungen = new ArrayList<>();

	public AufstellungService(String liga, String uebergabe) {
		// HTML-Dokument von der URL laden

		String mannschaft = liga;
		String url = uebergabe;
		Document doc = null;
		try {
			doc = WebCache.getPage(url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Tabelle extrahieren (nehmen wir an, sie hat eine spezifische Klasse oder Tag)
		Elements tabelle = doc.select("table"); // Hier musst du eventuell den richtigen Selektor anpassen

		// Jede Zeile in der Tabelle durchgehen und ausgeben
		for (Element row : tabelle.select("tr")) {
			Elements cols = row.select("td");

			// Überprüfe, ob die Zeile genügend Spalten hat
			if (cols.size() >= 5) {
				String rang = cols.get(0).text();
				String qttr = cols.get(1).text();
				String name = cols.get(2).text();
				String a = cols.get(3).text();
				String status = cols.get(4).text();

				aufstellungen.add(new Aufstellung(0, mannschaft, rang, qttr, name, a, status));

			}
		}
	}

	public List<Aufstellung> getAufstellungen() {
		return aufstellungen;
	}

}
