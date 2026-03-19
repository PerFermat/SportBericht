package de.bericht.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.provider.SpielplanProvider;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.WebCache;

public class TennisSpielplanService implements SpielplanProvider {

	private String verein;
	private String vereinnr;
	private String url;
	private int fehlercode;
	private List<Spiel> plan = new ArrayList<>();

	public TennisSpielplanService(String vereinnr, String url) {
		this.vereinnr = vereinnr;
		this.url = url;
		this.verein = ConfigManager.getSpielplanVerein(vereinnr);
		try {
			generierenSpielplan(vereinnr, url, "");
		} catch (Exception e) {
			fehlercode = -1;
		}
	}

	public int getFehlercode() {
		return fehlercode;
	}

	@Override
	public void generierenSpielplan(String vereinnr, String spielplan, String ligaVorhanden) throws Exception {
		plan.clear();

		try {
			Document doc = WebCache.getPage(spielplan);

			if (doc == null || doc.body() == null) {
				fehlercode = -1;
				return;
			}

			if (doc.body().text().contains("Ein Fehler ist aufgetreten")) {
				fehlercode = -1;
				return;
			}

			fehlercode = 0;
			String title = doc.title();

			Element zielTabelle = null;
			Elements tables = doc.select("table");
			if (tables.isEmpty()) {
				fehlercode = -1;
				return;
			}
			if (tables.size() >= 2) {
				zielTabelle = tables.get(1);
			} else {
				zielTabelle = tables.get(0);
			}

			Elements rows = zielTabelle.select("tbody tr");
			Elements headers = zielTabelle.select("th");

			int datumIndex = -1;
			int heimIndex = -1;
			int gastIndex = -1;
			int spielortIndex = -1;
			int matchesIndex = -1;
			int saetzeIndex = -1;
			int gamesIndex = -1;
			int berichtIndex = -1;

			for (int i = 0; i < headers.size(); i++) {
				String headerText = headers.get(i).text();
				if ("Datum".equals(headerText)) {
					datumIndex = i;
				} else if ("Heimmannschaft".equals(headerText)) {
					heimIndex = i;
				} else if ("Gastmannschaft".equals(headerText)) {
					gastIndex = i;
				} else if ("Spielort".equals(headerText)) {
					spielortIndex = i;
				} else if ("Matches".equals(headerText)) {
					matchesIndex = i;
				} else if ("Sätze".equals(headerText)) {
					saetzeIndex = i;
				} else if ("Games".equals(headerText)) {
					gamesIndex = i;
				} else if ("Spielbericht".equals(headerText)) {
					berichtIndex = i;
				}
			}

			for (Element row : rows) {
				Elements cols = row.select("td");
				if (cols.isEmpty()) {
					continue;
				}

				if (!isValidIndex(cols, datumIndex) || !isValidIndex(cols, heimIndex) || !isValidIndex(cols, gastIndex)
						|| !isValidIndex(cols, matchesIndex) || !isValidIndex(cols, saetzeIndex)
						|| !isValidIndex(cols, gamesIndex)) {
					continue;
				}

				String datum = formatiereDatumTennis(cols.get(datumIndex).text());
				String heim = cols.get(heimIndex).text();
				String gast = cols.get(gastIndex).text();

				String spielort = "";
				if (isValidIndex(cols, spielortIndex)) {
					spielort = cols.get(spielortIndex).text();
				}

				String matches = cols.get(matchesIndex).text();
				String saetze = cols.get(saetzeIndex).text();
				String games = cols.get(gamesIndex).text();

				boolean bericht = false;
				String ergebnisLink = "";

				if (!"0:0".equals(games) && isValidIndex(cols, berichtIndex)) {
					Element ergebnisElement = cols.get(berichtIndex).selectFirst("a");
					ergebnisLink = (ergebnisElement != null) ? ergebnisElement.absUrl("href") : "";
					bericht = ergebnisElement != null;
				}

				Element heimElement = cols.get(heimIndex).selectFirst("a");
				String heimLink = (heimElement != null) ? heimElement.absUrl("href") : null;

				Element gastElement = cols.get(gastIndex).selectFirst("a");
				String gastLink = (gastElement != null) ? gastElement.absUrl("href") : null;

				System.out.println(datum);
				System.out.println(heim + gast + verein);
				if (heim.contains(verein) || gast.contains(verein)) {
					plan.add(new TennisSpiel(vereinnr, title, datum, heim, heimLink, gast, gastLink, spielort, matches,
							saetze, games, ergebnisLink, bericht));
				}
			}

		} catch (IOException e) {
			fehlercode = -1;
			throw e;
		}
	}

	public static String formatiereDatumTennis(String datum) {
		if (datum == null || datum.isBlank()) {
			return datum;
		}

		try {
			// Eingabeformat: 2025-10-19
			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

			// Ausgabeformat: 10.10.2025
			DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

			LocalDate parsedDate = LocalDate.parse(datum.trim(), inputFormatter);
			return parsedDate.format(outputFormatter);

		} catch (DateTimeParseException e) {
			// Falls Format nicht passt → Original zurückgeben
			return datum;
		}
	}

	private boolean isValidIndex(Elements cols, int index) {
		return index >= 0 && index < cols.size();
	}

	@Override
	public List<Spiel> getSpielplan() {
		return plan;
	}

	@Override
	public List<Spiel> getSpielplanFreigabe(String vereinnr) {
		List<Spiel> freigegeben = new ArrayList<>();

		for (Spiel spiel : plan) {
			try {
				if (BerichtHelper.hasFreigabe(vereinnr, spiel.getErgebnisLink())) {
					freigegeben.add(spiel);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Collections.sort(freigegeben);
		return freigegeben;
	}

	@Override
	public void generiereVorschauBericht(String vereinnr, String was) {
		// aktuell leer wie bei deinem bisherigen Aufbau
	}

	@Override
	public String ausgabe(List<Spiel> spiele) {
		StringBuilder tabelleListe = new StringBuilder();

		for (Spiel spiel : spiele) {
			tabelleListe.append(spiel.getDatumAnzeige()).append(" - ");
			tabelleListe.append(spiel.getHeim()).append(" - ");
			tabelleListe.append(spiel.getGast()).append(" - ");
			tabelleListe.append(spiel.getErgebnis()).append(" - ");
		}

		return tabelleListe.toString();
	}
}