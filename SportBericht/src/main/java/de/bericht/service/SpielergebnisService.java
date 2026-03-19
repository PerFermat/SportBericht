package de.bericht.service;

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

import de.bericht.util.BerichtHelper;
import de.bericht.util.MatchErgebnis;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.TischtennisMatchSummary;

public class SpielergebnisService extends AbstractTischtennisSpielergebnisService {

	public SpielergebnisService(String vereinnr, String url, NamensSpeicher ns, Boolean verschluesseln) {
		super(vereinnr, url, ns, verschluesseln);
	}

	public SpielergebnisService(String vereinnr, String berichtMannschaft, String url, NamensSpeicher ns,
			Boolean verschluesseln) {
		super(vereinnr, berichtMannschaft, url, ns, verschluesseln);
	}

	@Override
	protected String ermittleOrt(String berichtMannschaft, Document doc) {
		String html = doc.html();

		String regexHeader = "(?:Bezirk\\s+([^\\d]+?)|Verbandsoberliga\\s+([^\\d]+?)|(TTBW))\\s+(\\d{4}/\\d{2}).*?-.*?(?:<!-- -->\\s*)*([^<]+)</div>";
		Pattern patternHeader = Pattern.compile(regexHeader, Pattern.DOTALL);
		Matcher matcherHeader = patternHeader.matcher(html);

		String regexTeams = "col-span-3[^>]*>([^<]+)</div>.*?" + "font-bold\">\\s*([^<]+)\\s*</div>.*?"
				+ "col-span-3[^>]*>([^<]+)</div>";
		Pattern patternTeams = Pattern.compile(regexTeams, Pattern.DOTALL);
		Matcher matcherTeams = patternTeams.matcher(html);

		if (matcherHeader.find() && matcherTeams.find()) {
			String heim = matcherTeams.group(1).trim();
			String gast = matcherTeams.group(3).trim();

			if (heim.contains(berichtMannschaft) && gast.contains(berichtMannschaft)) {
				return "HEIMGAST";
			} else if (heim.contains(berichtMannschaft)) {
				return "HEIM";
			} else if (gast.contains(berichtMannschaft)) {
				return "GAST";
			}
		}
		return "";
	}

	@Override
	protected TischtennisMatchSummary parseSummary(String vereinnr, String berichtMannschaft, Document doc) {
		String html = doc.html();

		String regexHeader = "(?:Bezirk\\s+([^\\d]+?)|Verbandsoberliga\\s+([^\\d]+?)|(TTBW))\\s+(\\d{4}/\\d{2}).*?-.*?(?:<!-- -->\\s*)*([^<]+)</div>";
		Pattern patternHeader = Pattern.compile(regexHeader, Pattern.DOTALL);
		Matcher matcherHeader = patternHeader.matcher(html);

		String regexTeams = "col-span-3[^>]*>([^<]+)</div>.*?" + "font-bold\">\\s*([^<]+)\\s*</div>.*?"
				+ "col-span-3[^>]*>([^<]+)</div>";
		Pattern patternTeams = Pattern.compile(regexTeams, Pattern.DOTALL);
		Matcher matcherTeams = patternTeams.matcher(html);

		if (matcherHeader.find() && matcherTeams.find()) {
			String bezirkRaw = matcherHeader.group(1) != null ? matcherHeader.group(1)
					: matcherHeader.group(2) != null ? matcherHeader.group(2) : matcherHeader.group(3);

			String bezirk = bezirkRaw.trim();
			String saison = matcherHeader.group(4).trim();
			String liga = matcherHeader.group(5).trim().replaceAll("<!--.*?-->", "").trim();

			String heim = BerichtHelper.vereinsnummer(vereinnr, matcherTeams.group(1).trim(), liga);
			String ergebnis = matcherTeams.group(2).trim();
			String gast = BerichtHelper.vereinsnummer(vereinnr, matcherTeams.group(3).trim(), liga);
			Map<String, String> zeiten = extractSpielzeiten(html);

			return new TischtennisMatchSummary(berichtMannschaft, heim, gast, bezirk, saison, liga, ergebnis,
					zeiten.get("spielbeginn"), zeiten.get("spielende"));
		}

		return new TischtennisMatchSummary(berichtMannschaft, "unbekannt", "unbekannt", "unbekannt", "unbekannt",
				"unbekannt", "", "", "");
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
							cols.get(4).text(), cols.get(5).text(), cols.get(6).text(), cols.get(7).text(), spielsaetze,
							cols.get(9).text()));
				}
			} else if (cols.size() == 4) {
				ergebnis = cols.get(3).text();
			}
		}

		if ((summary.getErgebnis() == null || summary.getErgebnis().isBlank()) && !ergebnis.isBlank()) {
			summary.setErgebnis(ergebnis);
		}
		return matchList;
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
}