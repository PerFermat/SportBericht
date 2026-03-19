package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class SpielergebnisService implements SpielergebnisProvider {

	private MatchSummary summary;
	private String ort;
	private String vereinnr;

	public SpielergebnisService(String vereinnr, String url, NamensSpeicher ns, Boolean verschluesseln) {

		this(vereinnr, ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"), url, ns, verschluesseln);

	}

	public SpielergebnisService(String vereinnr, String berichtMannschaft, String url, NamensSpeicher ns,
			Boolean verschluesseln) {

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

			// Jede Zeile in der Tabelle durchgehen und ausgeben
			for (Element row : tabelle.select("tr")) {
				Elements cols = row.select("td");

				// Überprüfe, ob die Zeile genügend Spalten hat
				if (cols.size() >= 9) {
					String spielart = cols.get(0).text();
					String spielheim = "";
					String spielgast = "";
					if (verschluesseln) {
						spielheim = ns.formatName(vereinnr, cols.get(1).text(), ns);
						spielgast = ns.formatName(vereinnr, cols.get(2).text(), ns);
					} else {
						spielheim = cols.get(1).text();
						spielgast = cols.get(2).text();
					}

					if (!spielart.startsWith("D") && spielheim.contains("/")) {
						spielart = "Doppel " + spielart;
					}

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

	private static void hinzufuegen(List<DoppelErgebnis> doppel, DoppelErgebnis neuesDoppel) {
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

	private static void hinzufuegen(List<EinzelErgebnis> einzel, EinzelErgebnis neuesEinzel) {
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

	private static String ermittleOrt(String berichtMannschaft, String html) {
		// Header (Bezirk, Saison, Liga) – Kommentare werden rausgeworfen
		String regexHeader = "(?:Bezirk\\s+([^\\d]+?)|Verbandsoberliga\\s+([^\\d]+?)|(TTBW))\\s+(\\d{4}/\\d{2}).*?-.*?(?:<!-- -->\\s*)*([^<]+)</div>";

		Pattern patternHeader = Pattern.compile(regexHeader, Pattern.DOTALL);
		Matcher matcherHeader = patternHeader.matcher(html);

		// Heim, Ergebnis, Gast
		String regexTeams = "col-span-3[^>]*>([^<]+)</div>.*?" // Heim
				+ "font-bold\">\\s*([^<]+)\\s*</div>.*?" // Ergebnis
				+ "col-span-3[^>]*>([^<]+)</div>"; // Gast
		Pattern patternTeams = Pattern.compile(regexTeams, Pattern.DOTALL);
		Matcher matcherTeams = patternTeams.matcher(html);

		if (matcherHeader.find() && matcherTeams.find()) {
			String heim = matcherTeams.group(1).trim();
			String gast = matcherTeams.group(3).trim();

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
		} else {
			System.out.println("Kein Match gefunden!");
		}
		return "";
	}

	public void matchTitel(String vereinnr, String berichtMannschaft, String html, String ergebnis) throws IOException {

		// Header (Bezirk, Saison, Liga) – Kommentare werden rausgeworfen
		String regexHeader = "(?:Bezirk\\s+([^\\d]+?)|Verbandsoberliga\\s+([^\\d]+?)|(TTBW))\\s+(\\d{4}/\\d{2}).*?-.*?(?:<!-- -->\\s*)*([^<]+)</div>";

		Pattern patternHeader = Pattern.compile(regexHeader, Pattern.DOTALL);
		Matcher matcherHeader = patternHeader.matcher(html);

		// Heim, Ergebnis, Gast
		String regexTeams = "col-span-3[^>]*>([^<]+)</div>.*?" // Heim
				+ "font-bold\">\\s*([^<]+)\\s*</div>.*?" // Ergebnis
				+ "col-span-3[^>]*>([^<]+)</div>"; // Gast
		Pattern patternTeams = Pattern.compile(regexTeams, Pattern.DOTALL);
		Matcher matcherTeams = patternTeams.matcher(html);

		if (matcherHeader.find() && matcherTeams.find()) {

			String bezirkRaw = matcherHeader.group(1) != null ? matcherHeader.group(1)
					: matcherHeader.group(2) != null ? matcherHeader.group(2) : matcherHeader.group(3);

			String bezirk = bezirkRaw.trim();

			// ✔ Saison = Gruppe 4
			String saison = matcherHeader.group(4).trim();

			// ✔ Liga = Gruppe 5
			String liga = matcherHeader.group(5).trim().replaceAll("<!--.*?-->", "").trim();

			String heim = BerichtHelper.vereinsnummer(vereinnr, matcherTeams.group(1).trim(), liga);
			String ergebnisGefunden = matcherTeams.group(2).trim();
			String gast = BerichtHelper.vereinsnummer(vereinnr, matcherTeams.group(3).trim(), liga);
			Map<String, String> zeiten = extractSpielzeiten(html);

			summary = new MatchSummary(berichtMannschaft, heim, gast, bezirk, saison, liga, ergebnisGefunden,
					zeiten.get("spielbeginn"), zeiten.get("spielende"));

		} else {
			System.out.println("Kein Match gefunden!");
		}
	}

	public static Map<String, String> extractSpielzeiten(String html) {
		Map<String, String> result = new HashMap<>();

		Document doc = Jsoup.parse(html);
		Pattern timePattern = Pattern.compile("\\d{2}:\\d{2}");

		for (Element div : doc.select("div.pb-2")) {
			String text = div.text();

			Matcher matcher = timePattern.matcher(text);
			if (matcher.find()) {
				String time = matcher.group();

				if (text.startsWith("Spielbeginn")) {
					result.put("spielbeginn", time);
				} else if (text.startsWith("Spielende")) {
					result.put("spielende", time);
				}
			}
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
