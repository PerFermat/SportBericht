package de.bericht.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TennisSpielerInfo {

	private static final Pattern TENNIS_SPIELER_PATTERN = Pattern
			.compile("^\\s*(\\d+)\\s*(.*?)\\s*(\\d+)\\s*[·.]?\\s*LK\\s*(\\d+)\\s*$");

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

		String normalized = text.replace('\u00A0', ' ').trim();
		Matcher m = TENNIS_SPIELER_PATTERN.matcher(normalized);
		if (m.matches()) {
			String position = m.group(1);
			String name = normalisiereName(m.group(2));
			String meldeliste = m.group(3);
			String lk = "LK " + m.group(4);
			return new TennisSpielerInfo(position, name, meldeliste, lk);
		}

		return new TennisSpielerInfo("", normalisiereName(normalized), "", "");
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
