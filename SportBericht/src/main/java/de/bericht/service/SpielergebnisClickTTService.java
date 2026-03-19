package de.bericht.service;

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

import de.bericht.util.MatchErgebnis;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.TischtennisMatchSummary;

public class SpielergebnisClickTTService extends AbstractTischtennisSpielergebnisService {

	public SpielergebnisClickTTService(String vereinnr, String url, NamensSpeicher ns, Boolean verschluesseln) {
		super(vereinnr, url, ns, verschluesseln);
	}

	public SpielergebnisClickTTService(String vereinnr, String berichtMannschaft, String url, NamensSpeicher ns,
			Boolean verschluesseln) {
		super(vereinnr, berichtMannschaft, url, ns, verschluesseln);
	}

	@Override
	protected String ermittleOrt(String berichtMannschaft, Document doc) {
		String html = doc.html();

		Pattern h1Pattern = Pattern.compile("<h1>(.*?)</h1>", Pattern.DOTALL);
		Matcher h1Matcher = h1Pattern.matcher(html);

		if (!h1Matcher.find()) {
			return "";
		}

		String title = h1Matcher.group(1).replace("&nbsp;", " ").replace("<br>", "XXXX").replaceAll("\\s+", " ").trim();
		String[] parts = title.split("XXXX");

		if (parts.length < 3) {
			return "";
		}

		String line3 = parts[2].trim();
		Pattern teamsPattern = Pattern.compile("^(.+?)\\s*-\\s*(.+?),");
		Matcher teamsMatcher = teamsPattern.matcher(line3);

		if (!teamsMatcher.find()) {
			return "";
		}

		String heim = teamsMatcher.group(1).trim();
		String gast = teamsMatcher.group(2).trim();

		if (heim.contains(berichtMannschaft) && gast.contains(berichtMannschaft)) {
			return "HEIMGAST";
		} else if (heim.contains(berichtMannschaft)) {
			return "HEIM";
		} else if (gast.contains(berichtMannschaft)) {
			return "GAST";
		}
		return "";
	}

	@Override
	protected TischtennisMatchSummary parseSummary(String vereinnr, String berichtMannschaft, Document doc) {
		String liga = "unbekannt";
		String heim = "unbekannt";
		String gast = "unbekannt";
		String bezirk = "unbekannt";
		String saison = "unbekannt";
		String spielbeginn = "unbekannt";
		String spielende = "unbekannt";

		String html = doc.toString();

		try {
			Pattern h1Pattern = Pattern.compile("<h1>(.*?)</h1>", Pattern.DOTALL);
			Matcher h1Matcher = h1Pattern.matcher(html);

			String title = null;
			if (h1Matcher.find()) {
				title = h1Matcher.group(1).replace("&nbsp;", " ").replace("<br>", "XXXX").replaceAll("\\s+", " ")
						.trim();
			}

			if (title != null && !title.isEmpty()) {
				String[] parts = title.split("XXXX");

				if (parts.length >= 3) {
					String line1 = parts[0].trim();
					liga = parts[1].trim();
					String line3 = parts[2].trim();

					Pattern bezirkPattern = Pattern.compile("^(?:Bezirk\\s+(\\S+)|TTBW)\\s+(\\d{4}/\\d{2})$");
					Matcher bezirkMatcher = bezirkPattern.matcher(line1);

					if (bezirkMatcher.find()) {
						bezirk = bezirkMatcher.group(1) != null ? bezirkMatcher.group(1).trim() : "TTBW";
						saison = bezirkMatcher.group(2) != null ? bezirkMatcher.group(2).trim() : "unbekannt";
					}

					Pattern teamsPattern = Pattern.compile("^(.+?)\\s*-\\s*(.+?),");
					Matcher teamsMatcher = teamsPattern.matcher(line3);

					if (teamsMatcher.find()) {
						heim = teamsMatcher.group(1) != null ? teamsMatcher.group(1).trim() : "unbekannt";
						gast = teamsMatcher.group(2) != null ? teamsMatcher.group(2).trim() : "unbekannt";
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Fehler beim Lesen des Titels: " + e.getMessage());
		}

		try {
			Map<String, String> zeiten = extractSpielzeiten(doc.toString());
			if (zeiten.get("spielbeginn") != null && !zeiten.get("spielbeginn").isBlank()) {
				spielbeginn = zeiten.get("spielbeginn");
			}
			if (zeiten.get("spielende") != null && !zeiten.get("spielende").isBlank()) {
				spielende = zeiten.get("spielende");
			}
		} catch (Exception e) {
			System.out.println("Fehler beim Extrahieren der Spielzeiten: " + e.getMessage());
		}

		return new TischtennisMatchSummary(berichtMannschaft, heim, gast, bezirk, saison, liga, "", spielbeginn,
				spielende);
	}

	@Override
	protected List<MatchErgebnis> parseMatches(String vereinnr, String berichtMannschaft, Document doc,
			NamensSpeicher ns, Boolean verschluesseln, TischtennisMatchSummary summary) {

		Elements tabellen = doc.select("table");
		List<MatchErgebnis> matchList = new ArrayList<>();
		String ergebnis = "";
		boolean istHeim = summary.getHeimmannschaft().contains(berichtMannschaft);

		for (Element row : tabellen.select("tr")) {
			Elements cols = row.select("td");

			if (cols.size() >= 10) {
				String spielart = cols.get(0).text();

				String spielheim;
				String spielgast;

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

				String spielsaetze = cols.get(8).text();
				if (!spielsaetze.isEmpty()) {
					matchList.add(new MatchErgebnis(istHeim, spielart, spielheim, spielgast, cols.get(3).text(),
							cols.get(4).text(), cols.get(5).text(), cols.get(6).text(), cols.get(7).text(), spielsaetze,
							cols.get(9).text()));
				}
			} else if (cols.size() == 4) {
				ergebnis = cols.get(3).text();
			}
		}

		summary.setErgebnis(ergebnis);
		return matchList;
	}

	public static String extractPlayers(Element td) {
		if (td == null) {
			return "";
		}

		Elements links = td.select("a");
		if (links.isEmpty()) {
			return td.text().trim();
		}

		String fullText = td.text().toLowerCase();
		if (fullText.contains("nicht anwesend")) {
			return "nicht anwesend";
		}

		return links.stream().map(Element::text).collect(Collectors.joining(" / "));
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
}