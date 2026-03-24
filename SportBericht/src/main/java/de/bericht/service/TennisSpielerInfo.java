package de.bericht.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TennisSpielerInfo {

	private static final Pattern LK_PATTERN = Pattern.compile("\\bLK\\s*(\\d{1,3})\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern POSITION_PATTERN = Pattern.compile("^\\s*(\\d{1,2})\\b");
	private static final Pattern MELDELISTE_PATTERN = Pattern.compile("(\\d{1,3})\\s*$");

	@JsonProperty("position")
	private final String position;

	@JsonProperty("name")
	private final String name;

	@JsonProperty("meldeliste")
	private final String meldeliste;

	@JsonProperty("leistungsklasse")
	private final String leistungsklasse;

	public TennisSpielerInfo(String position, String name, String meldeliste, String leistungsklasse) {
		this.position = position;
		this.name = name;
		this.meldeliste = meldeliste;
		this.leistungsklasse = leistungsklasse;
	}

	public static TennisSpielerInfo parse(String text) {
		if (text == null || text.isBlank()) {
			return new TennisSpielerInfo("", "", "", "");
		}
		if (text.contains("<") && text.contains(">")) {
			return parseHtml(text);
		}
		return parseLegacyText(text);
	}

	public static TennisSpielerInfo parseHtml(String html) {
		if (html == null || html.isBlank()) {
			return new TennisSpielerInfo("", "", "", "");
		}
		Element body = Jsoup.parseBodyFragment(html).body();
		if (body == null) {
			return parseLegacyText(html);
		}
		return parseElement(body);
	}

	public static TennisSpielerInfo parseCellHtml(String html) {
		return parseCellHtml(html, 0);
	}

	public static TennisSpielerInfo parseCellHtml(String html, int playerIndex) {
		if (html == null || html.isBlank()) {
			return new TennisSpielerInfo("", "", "", "");
		}
		Element body = Jsoup.parseBodyFragment(html).body();
		if (body == null) {
			return parseHtml(html);
		}
		Element segment = extrahiereSpielerSegment(body, playerIndex);
		if (segment == null) {
			return parseHtml(html);
		}
		return parseElement(segment);
	}

	private static Element extrahiereSpielerSegment(Element cellRoot, int playerIndex) {
		if (cellRoot == null) {
			return null;
		}
		String[] teile = cellRoot.html().split("(?i)<br\\s*/?>");
		int index = 0;
		for (String teil : teile) {
			Element segment = Jsoup.parseBodyFragment(teil).body();
			if (segment == null) {
				continue;
			}
			if (segment.selectFirst("abbr[title*=Quersumme]") != null) {
				continue;
			}
			if (segment.selectFirst("a") == null) {
				continue;
			}
			if (index == playerIndex) {
				return segment;
			}
			index++;
		}
		return null;
	}

	private static TennisSpielerInfo parseElement(Element root) {
		if (root == null) {
			return new TennisSpielerInfo("", "", "", "");
		}
		String normalizedText = root.text().replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();

		String position = textVonAbbr(root, "platzziffer");
		
		if (position.isBlank()) {
			Matcher m = POSITION_PATTERN.matcher(normalizedText);
			if (m.find()) {
				position = m.group(1);
			}
		}

		String meldeliste = textVonAbbr(root, "meldeliste");
		if (meldeliste.isBlank()) {
			Matcher m = Pattern.compile("(\\d{1,3})\\s*[·.]\\s*LK\\s*\\d{1,3}", Pattern.CASE_INSENSITIVE)
					.matcher(normalizedText);
			if (m.find()) {
				meldeliste = m.group(1);
			}
		}

		String lk = textVonAbbr(root, "leistungsklasse");
		if (lk.isBlank()) {
			Matcher m = LK_PATTERN.matcher(normalizedText);
			if (m.find()) {
				lk = "LK " + m.group(1);
			}
		} else if (!lk.toUpperCase().startsWith("LK")) {
			lk = "LK " + lk;
		}
		String name = "";
		Element link = root.selectFirst("a");
		if (link != null) {
			name = normalisiereName(link.text());
		}
		if (name.isBlank()) {
			String working = normalizedText;
			working = working.replaceAll("\\bLK\\s*\\d{1,3}\\b", " ");
			working = working.replaceAll("\\b\\d{1,3}\\s*[·.]\\s*", " ");
			working = working.replaceAll("^\\s*\\d{1,2}\\b", " ");
			working = working.replaceAll("\\s+", " ").trim();
			name = normalisiereName(working);
		}
		return new TennisSpielerInfo(position, name, meldeliste, lk);
	}

	private static TennisSpielerInfo parseLegacyText(String text) {

		String normalized = text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
		String working = normalized;

		String position = "";
		String meldeliste = "";
		String lk = "";

		Matcher lkMatcher = LK_PATTERN.matcher(working);
		if (lkMatcher.find()) {
			lk = "LK " + lkMatcher.group(1);
			working = lkMatcher.replaceFirst("").trim();
		}

		Matcher positionMatcher = POSITION_PATTERN.matcher(working);
		if (positionMatcher.find()) {
			position = positionMatcher.group(1);
			working = working.substring(positionMatcher.end()).trim();

		}

		working = working.replaceAll("^[·.\\-\\s]+|[·.\\-\\s]+$", "").trim();

		Matcher meldelisteMatcher = MELDELISTE_PATTERN.matcher(working);
		if (meldelisteMatcher.find()) {
			meldeliste = meldelisteMatcher.group(1);
			working = working.substring(0, meldelisteMatcher.start()).trim();
		}

		String name = normalisiereName(working);
		if (name.isBlank()) {
			name = normalisiereName(normalized);
		}
		return new TennisSpielerInfo(position, name, meldeliste, lk);

	}

	private static String textVonAbbr(Element root, String key) {
		if (root == null || key == null) {
			return "";
		}
		String lookup = key.toLowerCase();
		for (Element abbr : root.select("abbr[title]")) {
			String title = abbr.attr("title").toLowerCase();
			if (title.contains(lookup) || ("meldeliste".equals(lookup) && title.contains("nummer"))) {
				String text = abbr.text();
				if (text != null && !text.isBlank()) {
					return text.trim();
				}
			}
		}
		return "";
	}


	private static String normalisiereName(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String value = raw.trim();
		value = value.replaceAll("(?<=[\\p{Ll}])(?=\\p{Lu})", " ");
		value = value.replaceAll("\\s+", " ").trim();
		return value;
	}

	public String getPosition() {
		return position;
	}

	public String getName() {
		return name;
	}

	public String getMeldeliste() {
		return meldeliste;
	}

	public String getLeistungsklasse() {
		return leistungsklasse;
	}
}
