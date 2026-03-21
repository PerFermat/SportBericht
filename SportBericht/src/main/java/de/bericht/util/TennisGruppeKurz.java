package de.bericht.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TennisGruppeKurz {

	public static String kuerzeGruppe(String text) {
		if (text == null || text.isBlank()) {
			return text;
		}

		String original = text.trim();
		String lower = original.toLowerCase();

		StringBuilder result = new StringBuilder();

		// 1) Grundklasse bestimmen
		if (lower.contains("junior")) {
			result.append("J");
		} else if (lower.contains("kids-cup")) {
			result.append("KC");
		} else if (lower.contains("herren")) {
			result.append("H");
		} else if (lower.contains("damen")) {
			result.append("D");
		} else {
			// Fallback: ersten Teil beibehalten, falls etwas ganz anderes kommt
			result.append(original);
			return original;
		}

		// 2) Altersklasse bestimmen
		String alter = findeJugendAlter(original);
		if (alter != null) {
			result.append(alter); // z. B. U12, U15, U18
		} else {
			String alterszahl = findeAktivenAlter(original);
			if (alterszahl != null) {
				result.append(alterszahl); // z. B. 40, 50, 60, 65
			}
		}

		// 3) Staffel bestimmen
		String staffel = findeStaffel(original);
		if (staffel != null && !staffel.isBlank()) {
			result.append(" ").append(staffel);
		}

		// 4) Gruppe bestimmen
		Integer gruppe = findeGruppe(original);
		if (gruppe != null) {
			result.append(" (").append(gruppe).append(")");
		}

		return result.toString().trim();
	}

	private static String findeJugendAlter(String text) {
		Matcher m = Pattern.compile("\\bU\\s*(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE).matcher(text);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

	private static String findeAktivenAlter(String text) {
		// Sucht Zahlen wie "Herren 40", "Damen 50", "Herren 65"
		Matcher m = Pattern.compile("\\b(?:Herren|Damen)\\s+(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE).matcher(text);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

	private static String findeStaffel(String text) {
		String lower = text.toLowerCase();

		Matcher kreis = Pattern.compile("\\bKreisstaffel\\s*(\\d+)?\\b", Pattern.CASE_INSENSITIVE).matcher(text);
		if (kreis.find()) {
			return "KS" + (kreis.group(1) != null ? kreis.group(1) : "");
		}

		Matcher bezirk = Pattern.compile("\\bBezirksstaffel\\s*(\\d+)?\\b", Pattern.CASE_INSENSITIVE).matcher(text);
		if (bezirk.find()) {
			return "BS" + (bezirk.group(1) != null ? bezirk.group(1) : "");
		}

		if (lower.contains("staffelliga")) {
			return "SL";
		}

		return null;
	}

	private static Integer findeGruppe(String text) {
		Matcher m = Pattern.compile("\\bGr\\.\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE).matcher(text);
		if (m.find()) {
			return Integer.parseInt(m.group(1)); // entfernt führende Nullen automatisch
		}
		return null;
	}

}