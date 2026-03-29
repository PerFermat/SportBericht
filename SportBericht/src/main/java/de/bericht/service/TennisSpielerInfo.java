package de.bericht.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.bericht.util.NamensSpeicher;

public class TennisSpielerInfo {

	private static final Pattern LK_PATTERN = Pattern.compile("\\bLK\\s*(\\d{1,3})\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern POSITION_PATTERN = Pattern.compile("^\\s*(\\d{1,2})\\b");
	private static final Pattern MELDELISTE_PATTERN = Pattern.compile("(\\d{1,3})\\s*$");
	private static final Pattern STRAFWERTUNG_PATTERN = Pattern.compile("\\(\\s*(SW)\\s*\\)", Pattern.CASE_INSENSITIVE);

	@JsonProperty("position")
	private final String position;

	@JsonProperty("name")
	private final String name;

	@JsonProperty("meldeliste")
	private final String meldeliste;

	@JsonProperty("leistungsklasse")
	private final String leistungsklasse;

	@JsonProperty("strafwertung")
	private final boolean strafwertung;

	public TennisSpielerInfo(String vereinnr, String position, String name, String meldeliste, String leistungsklasse,
			NamensSpeicher ns, Boolean verschluesseln) {
		this(vereinnr, position, name, meldeliste, leistungsklasse, false, ns, verschluesseln);
	}

	public TennisSpielerInfo(String vereinnr, String position, String name, String meldeliste, String leistungsklasse,
			boolean strafwertung, NamensSpeicher ns, Boolean verschluesseln) {

		this.position = position;
		if (verschluesseln) {
			this.name = ns.formatName(vereinnr, name, ns);
		} else {
			this.name = name;
		}
		this.meldeliste = meldeliste;
		this.leistungsklasse = leistungsklasse;
		this.strafwertung = strafwertung;
	}

	public static TennisSpielerInfo parse(String vereinnr, String text, NamensSpeicher ns, Boolean verschluesseln) {
		if (text == null || text.isBlank()) {
			return new TennisSpielerInfo("", "", "", "", "", null, false);
		}
		boolean istStrafwertung = istStrafwertung(text);
		if (text.contains("<") && text.contains(">")) {
			return markiereStrafwertung(vereinnr, parseHtml(vereinnr, text, ns, verschluesseln), istStrafwertung, ns,
					verschluesseln);
		}
		return markiereStrafwertung(vereinnr, parseLegacyText(vereinnr, text, ns, verschluesseln), istStrafwertung, ns,
				verschluesseln);
	}

	public static TennisSpielerInfo parseHtml(String vereinnr, String html, NamensSpeicher ns, Boolean verschluesseln) {
		if (html == null || html.isBlank()) {
			return new TennisSpielerInfo("", "", "", "", "", null, false);
		}
		Element body = Jsoup.parseBodyFragment(html).body();
		if (body == null) {
			return parseLegacyText(vereinnr, html, ns, verschluesseln);
		}
		return parseElement(vereinnr, body, ns, verschluesseln);
	}

	public static TennisSpielerInfo parseCellHtml(String vereinnr, String html, NamensSpeicher ns,
			Boolean verschluesseln) {
		return parseCellHtml(vereinnr, html, 0, ns, verschluesseln);
	}

	public static TennisSpielerInfo parseCellHtml(String vereinnr, String html, int playerIndex, NamensSpeicher ns,
			Boolean verschluesseln) {
		if (html == null || html.isBlank()) {
			return new TennisSpielerInfo("", "", "", "", "", null, false);
		}
		Element body = Jsoup.parseBodyFragment(html).body();
		if (body == null) {
			return parseHtml(vereinnr, html, ns, verschluesseln);
		}
		boolean istStrafwertung = istStrafwertung(body.text());
		Element segment = extrahiereSpielerSegment(body, playerIndex);
		if (segment == null) {
			return markiereStrafwertung(vereinnr, parseHtml(vereinnr, html, ns, verschluesseln), istStrafwertung, ns,
					verschluesseln);
		}
		return markiereStrafwertung(vereinnr, parseElement(vereinnr, segment, ns, verschluesseln), istStrafwertung, ns,
				verschluesseln);
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

	private static TennisSpielerInfo markiereStrafwertung(String vereinnr, TennisSpielerInfo info, boolean strafwertung,
			NamensSpeicher ns, Boolean verschluesseln) {
		if (info == null) {
			return new TennisSpielerInfo("", "", "", "", "", strafwertung, null, false);
		}
		if (!strafwertung) {
			return info;
		}
		return new TennisSpielerInfo(vereinnr, info.position, info.name, info.meldeliste, info.leistungsklasse, true,
				ns, verschluesseln);
	}

	private static boolean istStrafwertung(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		return STRAFWERTUNG_PATTERN.matcher(text).find();
	}

	private static TennisSpielerInfo parseElement(String vereinnr, Element root, NamensSpeicher ns,
			Boolean verschluesseln) {
		if (root == null) {
			return new TennisSpielerInfo("", "", "", "", "", null, false);
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
		return new TennisSpielerInfo(vereinnr, position, name, meldeliste, lk, ns, verschluesseln);
	}

	private static TennisSpielerInfo parseLegacyText(String vereinnr, String text, NamensSpeicher ns,
			Boolean verschluesseln) {

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
		return new TennisSpielerInfo(vereinnr, position, name, meldeliste, lk, ns, verschluesseln);

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

	public boolean isStrafwertung() {
		return strafwertung;
	}

}
