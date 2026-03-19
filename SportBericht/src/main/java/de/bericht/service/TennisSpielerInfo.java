package de.bericht.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
