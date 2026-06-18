package de.bericht.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.util.BerichtHelper;
import de.bericht.util.MatchErgebnis;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.TischtennisMatchSummary;
import de.bericht.util.enums.HeimGastArt;

public class SpielergebnisService extends AbstractTischtennisSpielergebnisService {

	public SpielergebnisService(String vereinnr, String url, NamensSpeicher ns, Boolean verschluesseln) {
		super(vereinnr, url, ns, verschluesseln);
	}

	public SpielergebnisService(String vereinnr, HeimGastArt art, String berichtMannschaft, String url,
			NamensSpeicher ns, Boolean verschluesseln) {
		super(vereinnr, art, berichtMannschaft, url, ns, verschluesseln);
	}

	@Override
	protected String ermittleOrt(String berichtMannschaft, Document doc) {

		Element header = doc.selectFirst("div.rounded-xl.shadow-card.bg-white.p-4.text-center");

		if (header == null) {
			return "";
		}

		Elements teams = header.select(".subgrid--mytt.grid-cols-9 > div");

		if (teams.size() < 3) {
			return "";
		}

		String heim = teams.get(0).text().trim();
		String gast = teams.get(2).text().trim();

		return BerichtHelper.ermittelnHeimGast(heim, gast, berichtMannschaft, art);
	}

	private String getCell(Elements cols, Integer index) {

		if (index == null) {
			return "";
		}

		if (index >= cols.size()) {
			return "";
		}

		return cols.get(index).text();
	}

	@Override
	protected TischtennisMatchSummary parseSummary(String vereinnr, String berichtMannschaft, Document doc) {

		Element header = doc.selectFirst("div.rounded-xl.shadow-card.bg-white.p-4.text-center");

		if (header == null) {
			return new TischtennisMatchSummary(berichtMannschaft, "unbekannt", "unbekannt", "unbekannt", "unbekannt",
					"unbekannt", "", "", "");
		}

		// Bezirk / Saison / Liga
		String titel = header.selectFirst(".text-h4.py-4").text();
		// Beispiel:
		// Bezirk Staufen 2025/26 - Erwachsene Bezirksliga

		Pattern p = Pattern.compile("(.+?)\\s+(\\d{4}/\\d{2})\\s+-\\s+(.+)");
		Matcher m = p.matcher(titel);

		String bezirk = "unbekannt";
		String saison = "unbekannt";
		String liga = "unbekannt";

		if (m.matches()) {
			bezirk = m.group(1).trim();
			saison = m.group(2).trim();
			liga = m.group(3).trim();
		}

		Elements teams = header.select(".subgrid--mytt.grid-cols-9 > div");

		String heim = "unbekannt";
		String gast = "unbekannt";
		String ergebnis = "";

		if (teams.size() >= 3) {
			heim = BerichtHelper.vereinsnummer(vereinnr, teams.get(0).text().trim(), liga);

			ergebnis = teams.get(1).selectFirst(".font-bold").text().trim();

			gast = BerichtHelper.vereinsnummer(vereinnr, teams.get(2).text().trim(), liga);
		}

		Map<String, String> zeiten = extractSpielzeiten(doc);

		return new TischtennisMatchSummary(berichtMannschaft, heim, gast, bezirk, saison, liga, ergebnis,
				zeiten.get("spielbeginn"), zeiten.get("spielende"));
	}

	@Override
	protected List<MatchErgebnis> parseMatches(String vereinnr, String berichtMannschaft, Document doc,
			NamensSpeicher ns, Boolean verschluesseln, TischtennisMatchSummary summary) {

		List<MatchErgebnis> matchList = new ArrayList<>();
		String ergebnis = "";

		boolean istHeim = false;
		if (summary.getHeimmannschaft().equals(berichtMannschaft)) {
			istHeim = true;
		} else if (summary.getGastmannschaft().equals(berichtMannschaft)) {
			istHeim = false;
		} else if (summary.getHeimmannschaft().contains(berichtMannschaft)) {
			istHeim = true;
		} else if (summary.getGastmannschaft().contains(berichtMannschaft)) {
			istHeim = false;
		}

		for (Element table : doc.select("table")) {

			Element headerRow = table.selectFirst("thead tr");
			if (headerRow == null) {
				continue;
			}

			Map<String, Integer> idx = new HashMap<>();

			Elements headers = headerRow.select("th");
			for (int i = 0; i < headers.size(); i++) {
				idx.put(headers.get(i).text().trim(), i);
			}

			Integer spielartIdx = idx.get("");
			Integer heimIdx = idx.get("Heim");
			Integer gastIdx = idx.get("Gast");
			Integer s1Idx = idx.get("S1");
			Integer s2Idx = idx.get("S2");
			Integer s3Idx = idx.get("S3");
			Integer s4Idx = idx.get("S4");
			Integer s5Idx = idx.get("S5");
			Integer saetzeIdx = idx.get("Sätze");
			Integer gesamtIdx = idx.get("Gesamt");

			if (spielartIdx == null || heimIdx == null || gastIdx == null || saetzeIdx == null || gesamtIdx == null) {
				continue;
			}

			for (Element row : table.select("tbody tr")) {

				Elements cols = row.select("td");

				// Normale Spielzeile
				if (cols.size() > gesamtIdx) {

					String spielart = cols.get(spielartIdx).text().trim();

					String spielheim;
					String spielgast;

					if (verschluesseln) {
						spielheim = ns.formatName(vereinnr, cols.get(heimIdx).text().trim(), ns);

						spielgast = ns.formatName(vereinnr, cols.get(gastIdx).text().trim(), ns);
					} else {
						spielheim = cols.get(heimIdx).text().trim();
						spielgast = cols.get(gastIdx).text().trim();
					}

					if (!spielart.startsWith("D") && spielheim.contains("/")) {
						spielart = "Doppel " + spielart;
					}

					String spielsaetze = cols.get(saetzeIdx).text().trim();

					if (!spielsaetze.isEmpty()) {

						matchList.add(new MatchErgebnis(istHeim, spielart, spielheim, spielgast, getCell(cols, s1Idx),
								getCell(cols, s2Idx), getCell(cols, s3Idx), getCell(cols, s4Idx), getCell(cols, s5Idx),
								spielsaetze, cols.get(gesamtIdx).text().trim()));
					}
				}

				// Abschlusszeile mit Gesamtergebnis
				else if (cols.size() == 4) {

					String wert = cols.get(3).text().trim();

					if (wert.matches("\\d+\\s*:\\s*\\d+")) {
						ergebnis = wert;
					}
				}
			}
		}

		if ((summary.getErgebnis() == null || summary.getErgebnis().isBlank()) && !ergebnis.isBlank()) {
			summary.setErgebnis(ergebnis);
		}

		return matchList;
	}

	private Map<String, Integer> ermittleSpaltenIndex(Element table) {

		Map<String, Integer> indexMap = new HashMap<>();

		Element headerRow = table.selectFirst("thead tr");

		if (headerRow == null) {
			return indexMap;
		}

		Elements headers = headerRow.select("th");

		for (int i = 0; i < headers.size(); i++) {
			indexMap.put(headers.get(i).text().trim(), i);
		}

		return indexMap;
	}

	public static Map<String, String> extractSpielzeiten(Document doc) {

		Map<String, String> result = new HashMap<>();

		for (Element div : doc.select("div.pb-2")) {

			String text = div.text();

			if (text.startsWith("Spielbeginn:")) {
				result.put("spielbeginn", text.replace("Spielbeginn:", "").replace("Uhr", "").trim());
			}

			if (text.startsWith("Spielende:")) {
				result.put("spielende", text.replace("Spielende:", "").replace("Uhr", "").trim());
			}
		}

		return result;
	}
}