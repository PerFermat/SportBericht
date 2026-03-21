package de.bericht.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.provider.SpielplanProvider;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.WebCache;

public class SpielplanServiceClick implements SpielplanProvider {

	private int fehlercode = 0;
	List<Spiel> spiele = new ArrayList<>();
	private ConfigManager config;

	public SpielplanServiceClick(String vereinnr) throws IOException {
		config = ConfigManager.getInstance();
		String ligaVorhanden = config.getSpielplanLiga(vereinnr);
		String url = config.getSpielplanURL(vereinnr);
		generierenSpielplan(vereinnr, url, ligaVorhanden);
	}

	public SpielplanServiceClick(String vereinnr, String url) throws IOException {
		String ligaVorhanden = "Nein";
		generierenSpielplan(vereinnr, url, ligaVorhanden);
	}

	@Override
	public void generierenSpielplan(String vereinnr, String spielplan, String ligaVorhanden) {
		String SPIELPLAN_URL = spielplan;

		fehlercode = -1;

		try {
			Document doc = WebCache.getPage(SPIELPLAN_URL);
			// Alle Tabellen finden
			Elements tables = doc.select("table");

			int tableIndex = 1;
			for (Element table : tables) {

				if (table != null) {
					Elements rows = table.select("tbody tr"); // Zeilen innerhalb der ersten Tabelle
					String letztesDatum = "";
					String letzterWochentag = "";
					int wochentagIndex = -1;
					int datumIndex = -1;
					int heimIndex = -1;
					int gastIndex = -1;
					int ligaIndex = -1;
					int zeitIndex = -1;
					int punkteIndex = -1;

					Elements headers = table.select("th");
					// Spaltenindizes ermitteln
					for (int i = 0; i < headers.size(); i++) {

						String headerText = headers.get(i).text();
						if (headerText.equals("Liga")) {
							ligaIndex = i + 2;
						} else if (headerText.equals("Tag Datum Zeit")) {
							wochentagIndex = i;
							datumIndex = i + 1;
							zeitIndex = i + 2;
						} else if (headerText.equals("Spiele")) {
							punkteIndex = i + 2;
						} else if (headerText.equals("Heimmannschaft")) {
							heimIndex = i + 2;
						} else if (headerText.equals("Gastmannschaft")) {
							gastIndex = i + 2;
						}
					}

					if (heimIndex > 0 || gastIndex > 0) {
						for (Element row : rows) {
							Elements cols = row.select("td"); // Alle Spalten der Zeile
							if (!cols.isEmpty()) {
								int korrektur = 0;
								if (cols.get(wochentagIndex).text().length() > 3) {
									korrektur = -1;
								}
								String datum = cols.get(datumIndex + korrektur).text();
								String wochentag = cols.get(wochentagIndex).text();
								String zeit = cols.get(zeitIndex + korrektur).text();
								if (wochentag.isBlank()) {
									wochentag = letzterWochentag;
								}
								if (datum.isBlank()) {
									datum = letztesDatum;
								}
								letztesDatum = datum;
								letzterWochentag = wochentag;
								String wochentagDatum = wochentag + " " + datum + "\n" + zeit;
								String liga = cols.get(ligaIndex + korrektur).text();
								String heim = cols.get(heimIndex + korrektur).text();
								String gast = cols.get(gastIndex + korrektur).text();
								if ((liga.startsWith("H") || liga.startsWith("E")) && heim.equals("TSGV Hattenhofen")) {
									heim += " I";
								}

								if ((liga.startsWith("H") || liga.startsWith("E")) && gast.equals("TSGV Hattenhofen")) {
									gast += " I";
								}
								String ergebnis = cols.get(punkteIndex + korrektur).text();
								if ("0:0".equals(ergebnis)) {
									ergebnis = "";
								}

								// Ergebnis-Link extrahieren (falls vorhanden)
								Element ergebnisElement = cols.get(punkteIndex + korrektur).selectFirst("a");
								String ergebnisLink = (ergebnisElement != null) ? ergebnisElement.absUrl("href") : null;

								fehlercode = 0;
								spiele.add(new TischtennisSpiel(vereinnr, wochentag + " " + datum + " " + zeit,
										wochentag, datum, zeit, liga, heim, gast, ergebnis, ergebnisLink));
							}
						}
					}
				}
			}
		} catch (IOException e) {
			fehlercode = -1;
			e.printStackTrace();
		}

	}

	public int getFehlercode() {
		return fehlercode;
	}

	public void setFehlercode(int fehlercode) {
		this.fehlercode = fehlercode;
	}

	@Override
	public List<Spiel> getSpielplan() {
		return spiele;
	}

	@Override
	public String ausgabe(List<Spiel> spiele) {
		StringBuilder tabelleListe = new StringBuilder();

		for (Spiel spiel : spiele) {
			tabelleListe.append(spiel.getDatumGesamt()).append(" - ");
			tabelleListe.append(spiel.getWochentag()).append(" - ");
			tabelleListe.append(spiel.getDatum()).append(" - ");
			tabelleListe.append(spiel.getZeit()).append(" - ");
			tabelleListe.append(spiel.getLiga()).append(" - ");
			tabelleListe.append(spiel.getHeim()).append(" - ");
			tabelleListe.append(spiel.getGast()).append(" - ");
			tabelleListe.append(spiel.getErgebnis()).append(" \n ");
		}

		return tabelleListe.toString();
	}

	@Override
	public void generiereVorschauBericht(String vereinnr, String was) {
		String textBody = ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.text");
		StringBuffer sb = new StringBuffer();
		LocalDate heute = LocalDate.now();

		long tageLetzterSatz = 9999;
		int i = 0;
		String aktDatum = "";
		sb.append("<p>");

		for (Spiel spiel : spiele) {
			Boolean spielDruck = false;
			if (!spiel.getErgebnis().isEmpty()) {
				spielDruck = false;
			} else if ("HEIM".equals(was)
					&& spiel.getHeim().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
				spielDruck = true;
			} else if ("GAST".equals(was)
					&& spiel.getGast().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
				spielDruck = true;
			} else if ("ALLE".equals(was)) {

				if (spiel.getGast().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
					spielDruck = true;
				} else if (spiel.getHeim().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
					spielDruck = true;
				}
			}
			if (spielDruck) {
				long tage = tageBisDatum(spiel.getDatum(), heute);
				long configTage = Long.parseLong(ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.tage"));
				if (tage <= configTage || tage <= tageLetzterSatz + 1 || i < 2 || aktDatum.equals(spiel.getDatum())) {
					if (!aktDatum.equals(spiel.getDatum())) {
						sb.append("<br><strong>" + wochentag(spiel.getDatum()) + ", " + spiel.getDatum()
								+ " </strong> <br> ");
					}
					i++;
					tageLetzterSatz = tage;
					sb.append("<strong>   - " + spiel.getZeit() + ": " + klartextLiga(spiel.getLiga()) + ": </strong> "
							+ spiel.getHeim() + " - " + spiel.getGast() + "<br>");

					aktDatum = spiel.getDatum();
				}
			}

		}
		sb.append("</p>");

		DatabaseService dbService = new DatabaseService(vereinnr);
		dbService.saveBerichtDataOhneHist(vereinnr, "31.12.2999 - Vorschaubericht",
				textBody.replace("[spielliste]", sb.toString()), null, null,
				ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.ueberschrift"));
	}

	private static String klartextLiga(String ligatext) {
		String[] liga = ligatext.split(" "); // Aufteilen an Leerzeichen
		String ausgabeliga = "";
		int i = 0;
		// Durch die Teile mit Index iterieren
		// Überprüfen, ob das erste Element "H" ist, dann ersetzen
		if (liga[0].equals("H")) {
			i++;
			ausgabeliga = "Herren";
		} else if (liga[0].equals("E")) {
			i++;
			ausgabeliga = "Erwachsene";
		} else if (liga[0].equals("D")) {
			i++;
			ausgabeliga = "Damen";
		} else if (liga[0].equals("S40")) {
			i++;
			ausgabeliga = "Senioren Ü40";
		} else if (liga[0].equals("J")) {
			i++;
			i++;
			ausgabeliga = "Jungen U" + liga[1];
		} else if (liga[0].equals("M")) {
			i++;
			i++;
			ausgabeliga = "Mädchen U" + liga[1];
		}
		if (i <= liga.length) {
			if (liga[i].equals("KL")) {
				ausgabeliga = ausgabeliga + " Kreisliga";
				i++;
			} else if (liga[i].equals("BK")) {
				i++;
				ausgabeliga = ausgabeliga + " Bezirksklasse";
			} else if (liga[i].equals("BL")) {
				i++;
				ausgabeliga = ausgabeliga + " Bezirksliga";
			} else if (liga[i].equals("LK")) {
				i++;
				ausgabeliga = ausgabeliga + " Landesklasse";
			} else if (liga[i].equals("LL")) {
				i++;
				ausgabeliga = ausgabeliga + " Landesliga";
			} else if (liga[i].equals("VL")) {
				i++;
				ausgabeliga = ausgabeliga + " Verbandsliga";
			}
		}
		// Durch die Teile mit Index iterieren und ausgeben
		for (int j = i; j < liga.length; j++) {
			ausgabeliga = ausgabeliga + " " + liga[j];
		}
		return ausgabeliga;
	}

	public static long tageBisDatum(String dateStr, LocalDate heute) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		try {
			LocalDate eingabeDatum = LocalDate.parse(dateStr, formatter);
			return ChronoUnit.DAYS.between(heute, eingabeDatum);
		} catch (Exception e) {
			System.err.println("Ungültiges Datumsformat. Bitte verwende TT.MM.JJJJ " + dateStr);
			return 99; // Oder eine andere sinnvolle Fehlerbehandlung
		}
	}

	public static String wochentag(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		LocalDate datum = LocalDate.parse(dateStr, formatter);

		return datum.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.GERMAN);

	}

	@Override
	public List<Spiel> getSpielplanFreigabe(String vereinnr) {

		List<Spiel> freigegeben = new ArrayList<>();
		for (Spiel spiel : spiele) {
			try {
				if (BerichtHelper.hasFreigabe(vereinnr, spiel.getErgebnisLink())) {
					freigegeben.add(spiel);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		java.util.Collections.sort(freigegeben);
		return freigegeben;
	}
}