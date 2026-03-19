package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.provider.SpielergebnisProvider;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.MatchErgebnis;
import de.bericht.util.MatchSummary;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.WebCache;

public class SpielergebnisClickTTService implements SpielergebnisProvider {

	private MatchSummary summary;
	private String ort;
	private String vereinnr;

	public SpielergebnisClickTTService(String vereinnr, String url, NamensSpeicher ns, Boolean verschluesseln) {

		this(vereinnr, ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"), url, ns, verschluesseln);

	}

	public SpielergebnisClickTTService(String vereinnr, String berichtMannschaft, String url, NamensSpeicher ns,
			Boolean verschluesseln) {
		// HTML-Dokument von der URL laden
		this.vereinnr = vereinnr;
		try {
			Document doc = WebCache.getPage(url);
			ort = ermittleOrt(berichtMannschaft, doc.html()).toUpperCase();
			Elements tabelle = doc.select("table"); // Hier musst du eventuell den richtigen Selektor anpassen
			String ergebnis = "";
			List<MatchErgebnis> matchList = new ArrayList<>();
			matchTitel(vereinnr, berichtMannschaft, doc.toString(), ergebnis);
			boolean istHeim = false;
			if (summary.getHeimmannschaft().contains(berichtMannschaft)) {
				istHeim = true;
			}

			// boolean istHeim = false;
			// Jede Zeile in der Tabelle durchgehen und ausgeben
			for (Element row : tabelle.select("tr")) {
				Elements cols = row.select("td");

				// Überprüfe, ob die Zeile genügend Spalten hat
				if (cols.size() >= 9) {
					String spielart = cols.get(0).text();
					String spielheim = null;
					String spielgast = null;

					// String spielmous = spielModus(cols.get(0).text());
					// String spielheim = namensSpeicher.formatName(vereinnr, cols.get(1).text(),
					// namensSpeicher);
					// String spielgast = namensSpeicher.formatName(vereinnr, cols.get(2).text(),
					// namensSpeicher);
//					if (spielart.startsWith("D")) {
					if (verschluesseln) {
						spielheim = ns.formatName(vereinnr, extractPlayers(cols.get(1)), ns);
						spielgast = ns.formatName(vereinnr, extractPlayers(cols.get(2)), ns);
					} else {
						spielheim = extractPlayers(cols.get(1));
						spielgast = extractPlayers(cols.get(2));
					}
					if (!spielart.startsWith("D") && spielheim.contains("/")) {
						spielart = "Doppel " + spielart;
					}
//					} else {
//						if (verschluesseln) {
//							spielheim = ns.formatName(vereinnr, cols.get(1).text(), ns);
//							spielgast = ns.formatName(vereinnr, cols.get(2).text(), ns);
//						} else {
//							spielheim = cols.get(1).text();
//							spielgast = cols.get(2).text();
//						}
//					}
					String spielsaetze = cols.get(8).text();
					if (!spielsaetze.isEmpty()) {
						matchList.add(new MatchErgebnis(istHeim, spielart, spielheim, spielgast, cols.get(3).text(),
								cols.get(4).text(), cols.get(5).text(), cols.get(6).text(), cols.get(7).text(),
								spielsaetze, cols.get(9).text()));
					}
				} else if (cols.size() == 4) {
					ergebnis = cols.get(3).text();
				}
			}
			summary.setErgebnis(ergebnis);
			summary.setSpiele(matchList);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getSpielErgebnis() {
		List<MatchErgebnis> spiele = summary.getSpiele();

		List<DoppelErgebnis> doppel = new ArrayList<>();
		List<EinzelErgebnis> einzel = new ArrayList<>();
		String fett1 = "";
		String fett2 = "";
		fett1 = "<strong>";
		fett2 = "</strong>";

		// Jede Zeile in der Tabelle durchgehen und ausgeben
		for (MatchErgebnis row : spiele) {

			if (row.getPosition().contains("D")) {
				DoppelErgebnis neuesHeimDoppel = new DoppelErgebnis("HEIM", row.getPosition(), row.getHeim(),
						row.getSaetze());
				if (!neuesHeimDoppel.getName().contains("anwesend")) {
					hinzufuegen(doppel, neuesHeimDoppel);

				}
				DoppelErgebnis neuesGastDoppel = new DoppelErgebnis("GAST", row.getPosition(), row.getGast(),
						row.getSaetze());
				if (!neuesGastDoppel.getName().contains("anwesend")) {
					hinzufuegen(doppel, neuesGastDoppel);
				}
			} else {
				EinzelErgebnis neuesHeimEinzel = new EinzelErgebnis("HEIM", row.getPosition(), row.getHeim(),
						row.getSaetze());
				if (!neuesHeimEinzel.getName().contains("nicht anwesend")) {
					hinzufuegen(einzel, neuesHeimEinzel);
				}
				EinzelErgebnis neuesGastEinzel = new EinzelErgebnis("GAST", row.getPosition(), row.getGast(),
						row.getSaetze());
				if (!neuesGastEinzel.getName().contains("nicht anwesend")) {
					hinzufuegen(einzel, neuesGastEinzel);
				}

			}
		}
		doppel.sort(null);
		einzel.sort(null);
		String[] internort = { "HEIM", "GAST" };
		StringBuilder ergebnisString = new StringBuilder();
		for (int intern = 0; intern < 2; intern++) {
			ergebnisString.append(fett1 + "Für " + BerichtHelper.getOrt(vereinnr) + " spielten:<br>" + fett2);

			ergebnisString.append(fett1 + "Doppel: " + fett2);
			int j = 0;
			for (int i = 0; i < doppel.size(); i++) {
				DoppelErgebnis doppelPaarung = doppel.get(i);
				if (doppelPaarung.getMannschaft().equals(ort)
						|| (doppelPaarung.getMannschaft().equals(internort[intern]) && ort.equals("HEIMGAST"))) {
					if (j > 0) {
						ergebnisString.append(", ");
					}
					j++;
					ergebnisString.append(doppelPaarung.getName());
					ergebnisString.append(" (" + doppelPaarung.getSiege() + ":" + doppelPaarung.getNiederlagen() + ")");
				}
			}

			ergebnisString.append("<br>");
			ergebnisString.append(fett1 + "Einzel: " + fett2);
			j = 0;
			;
			for (int i = 0; i < einzel.size(); i++) {
				EinzelErgebnis einzelPaarung = einzel.get(i);

				// Hier können Sie das SpielErgebnis-Objekt verarbeiten oder ausgeben
				if (einzelPaarung.getMannschaft().equals(ort)
						|| (einzelPaarung.getMannschaft().equals(internort[intern]) && ort.equals("HEIMGAST"))) {
					if (j > 0) {
						ergebnisString.append(", ");
					}
					j++;
					ergebnisString.append(einzelPaarung.getName());
					ergebnisString.append(" (" + einzelPaarung.getSiege() + ":" + einzelPaarung.getNiederlagen() + ")");
				}
			}
			if (!ort.equals("HEIMGAST")) {
				intern = 2;
			} else {
				ergebnisString.append("<br><br>");
			}
		}

		String result = ergebnisString.toString(); // Wandelt den StringBuilder in einen String um
		return result;
	}

	public static String extractPlayers(Element td) {
		if (td == null) {
			return "";
		}

		Elements links = td.select("a");
		if (links.isEmpty()) {
			return td.text().trim();
		}

		// Prüfen, ob "nicht anwesend" im Text vorkommt
		String fullText = td.text().toLowerCase();
		if (fullText.contains("nicht anwesend")) {
			return "nicht anwesend";
		}

		String ergebnis = links.stream().map(Element::text).collect(Collectors.joining(" / "));
		return ergebnis;
	}

	public static void hinzufuegen(List<DoppelErgebnis> doppel, DoppelErgebnis neuesDoppel) {
		boolean vorhanden = false;
		for (DoppelErgebnis vorhandenesDoppel : doppel) {
			if (vorhandenesDoppel.getName().equals(neuesDoppel.getName())) {
				vorhanden = true;
				vorhandenesDoppel.addSiege(neuesDoppel.getSiege());
				vorhandenesDoppel.addNiederlagen(neuesDoppel.getNiederlagen());
				break;
			}
		}

		if (!vorhanden) {
			doppel.add(neuesDoppel);
		}
	}

	public static void hinzufuegen(List<EinzelErgebnis> einzel, EinzelErgebnis neuesEinzel) {
		boolean vorhanden = false;
		for (EinzelErgebnis vorhandenesEinzel : einzel) {
			if (vorhandenesEinzel.getName().equals(neuesEinzel.getName())) {
				vorhanden = true;
				vorhandenesEinzel.addSiege(neuesEinzel.getSiege());
				vorhandenesEinzel.addNiederlagen(neuesEinzel.getNiederlagen());
				break;
			}
		}

		if (!vorhanden) {
			einzel.add(neuesEinzel);
		}
	}

	public static String ermittleOrt(String berichtMannschaft, String html) {
		// Extrahiere den Inhalt zwischen <h1> und </h1>
		Pattern h1Pattern = Pattern.compile("<h1>(.*?)</h1>", Pattern.DOTALL);
		Matcher h1Matcher = h1Pattern.matcher(html);

		if (!h1Matcher.find()) {
			System.out.println("Kein <h1>-Titel gefunden!");
			return "";
		}

		String title = h1Matcher.group(1).replace("&nbsp;", " ").replace("<br>", "XXXX").replaceAll("\\s+", " ").trim();

		// Zeilen trennen
		String[] parts = title.split("XXXX");

		if (parts.length < 3) {
			System.out.println("Titel hat unerwartetes Format!");
			return "";
		}

		// Dritte Zeile enthält "Heim - Gast , Datum, Uhrzeit"
		String line3 = parts[2].trim();

		// Heim- und Gastteam extrahieren
		Pattern teamsPattern = Pattern.compile("^(.+?)\\s*-\\s*(.+?),");
		Matcher teamsMatcher = teamsPattern.matcher(line3);

		if (!teamsMatcher.find()) {
			System.out.println("Konnte Heim- und Gastmannschaft nicht erkennen!");
			return "";
		}

		String heim = teamsMatcher.group(1).trim();
		String gast = teamsMatcher.group(2).trim();

		// Prüfe, ob der Verein Heim oder Gast ist
		if (heim.contains(berichtMannschaft) && gast.contains(berichtMannschaft)) {
			return "HEIMGAST";
		} else if (heim.contains(berichtMannschaft)) {
			return "HEIM";
		} else if (gast.contains(berichtMannschaft)) {
			return "GAST";
		} else {
			System.out.println("Verein '" + berichtMannschaft + "' wurde im Titel nicht gefunden!");
		}

		return "";
	}

	public void matchTitel(String vereinnr, String berichtMannschaft, String html, String ergebnis) {

		String liga = "unbekannt";
		String heim = "unbekannt";
		String gast = "unbekannt";
		String bezirk = "unbekannt";
		String saison = "unbekannt";
		String spielbeginn = "unbekannt";
		String spielende = "unbekannt";

		try {
			// Regex zum Extrahieren des Inhalts zwischen <h1> und </h1>
			Pattern h1Pattern = Pattern.compile("<h1>(.*?)</h1>", Pattern.DOTALL);
			Matcher h1Matcher = h1Pattern.matcher(html != null ? html : "");

			String title = null;
			if (h1Matcher.find()) {
				title = h1Matcher.group(1).replace("&nbsp;", " ").replace("<br>", "XXXX").replaceAll("\\s+", " ")
						.trim();
			} else {
				System.out.println("Kein <h1>-Titel gefunden!");
			}

			if (title != null && !title.isEmpty()) {
				// Jetzt Zeilen trennen
				String[] parts = title.split("XXXX");

				if (parts.length >= 3) {
					// Zeile 1: "Bezirk Staufen 2025/26"
					String line1 = parts[0].trim();
					// Zeile 2: "Erwachsene Bezirksliga"
					liga = parts[1].trim();
					// Zeile 3: "TSG Eislingen III - TSGV Hattenhofen , 02.11.2025, 10:00 Uhr"
					String line3 = parts[2].trim();

					// Bezirk und Saison trennen (Bezirk <Name> ODER TTBW)
					Pattern bezirkPattern = Pattern.compile("^(?:Bezirk\\s+(\\S+)|TTBW)\\s+(\\d{4}/\\d{2})$");
					Matcher bezirkMatcher = bezirkPattern.matcher(line1);

					if (bezirkMatcher.find()) {
						// Wenn Bezirk-Name existiert → Gruppe 1, sonst → "TTBW"
						bezirk = bezirkMatcher.group(1) != null ? bezirkMatcher.group(1).trim() : "TTBW";
						saison = bezirkMatcher.group(2) != null ? bezirkMatcher.group(2).trim() : "unbekannt";
					}

					// Heim- und Gastmannschaft trennen
					Pattern teamsPattern = Pattern.compile("^(.+?)\\s*-\\s*(.+?),");
					Matcher teamsMatcher = teamsPattern.matcher(line3);

					if (teamsMatcher.find()) {
						heim = teamsMatcher.group(1) != null ? teamsMatcher.group(1).trim() : "unbekannt";
						gast = teamsMatcher.group(2) != null ? teamsMatcher.group(2).trim() : "unbekannt";
					}
				} else {
					System.out.println("Titel hat unerwartetes Format!");
				}
			}
		} catch (Exception e) {
			System.out.println("Fehler beim Lesen des Titels: " + e.getMessage());
		}

		// Ergebnis (falls vorhanden)
		String ergebnisGefunden = (ergebnis != null && !ergebnis.isEmpty()) ? ergebnis : "";

		// Spielzeiten sicher lesen
		try {
			Map<String, String> zeiten = extractSpielzeiten(html);
			if (zeiten != null) {
				if (zeiten.get("spielbeginn") != null && !zeiten.get("spielbeginn").isBlank()) {
					spielbeginn = zeiten.get("spielbeginn");
				}
				if (zeiten.get("spielende") != null && !zeiten.get("spielende").isBlank()) {
					spielende = zeiten.get("spielende");
				}
			}
		} catch (Exception e) {
			System.out.println("Fehler beim Extrahieren der Spielzeiten: " + e.getMessage());
		}

		summary = new MatchSummary(
				berichtMannschaft != null && !berichtMannschaft.isBlank() ? berichtMannschaft : "unbekannt", heim, gast,
				bezirk, saison, liga, ergebnisGefunden, spielbeginn, spielende);
	}

	public static Map<String, String> extractSpielzeiten(String html) {
		Map<String, String> result = new HashMap<>();

		Document doc = Jsoup.parse(html);
		String text = doc.text();

		Pattern pattern = Pattern.compile("Spielbeginn:\\s*(\\d{2}:\\d{2}).*?Spielende:\\s*(\\d{2}:\\d{2})");

		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			result.put("spielbeginn", matcher.group(1));
			result.put("spielende", matcher.group(2));
		}

		return result;
	}

	@Override
	public String summaryToJson() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(summary);
	}

	// Methode zum Konvertieren einer Liste in JSON
	@Override
	public String listToJson(List<MatchErgebnis> matches) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(matches);
	}

	@Override
	public MatchSummary getSummary() {
		return summary;
	}

	public void setSummary(MatchSummary summary) {
		this.summary = summary;
	}

}
