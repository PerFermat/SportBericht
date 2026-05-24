package de.bericht.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bericht.provider.SpielergebnisFactory;
import de.bericht.provider.SpielergebnisProvider;
import de.bericht.service.DatabaseService;

public class NamensSpeicher {
	DatabaseService db = null;
	private Map<String, String> namensListe;

	public NamensSpeicher() {
		namensListe = new HashMap<>();
	}

	public String speichereNamen(String klarname, boolean isVorname, String vereinsnr, boolean verschluesseln) {

		if (db == null) {
			db = new DatabaseService(vereinsnr);
		}
		String anonym;
		if (namensListe.containsValue(klarname)) {
			return klarname;
		}
		if (namensListe.containsKey(klarname)) {
			return namensListe.get(klarname);
		} else {
			if (verschluesseln) {
				if (isVorname) {
					anonym = db.anomisiereVorname(vereinsnr, klarname);
				} else {
					anonym = db.anomisiereNachname(vereinsnr, klarname);
				}
			} else {
				anonym = klarname;
			}

			namensListe.put(klarname, anonym);
			return anonym;
		}
	}

	// Optional: Methode zum Anzeigen aller gespeicherten Namen
	public String zeigeAlle() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<String, String> eintrag : namensListe.entrySet()) {
			text.append("Klarname: " + eintrag.getKey() + " - Anonym: " + eintrag.getValue() + "<br />");
		}
		return text.toString();
	}

	// Optional: Methode zum Anzeigen aller gespeicherten Namen
	public String ausgabe() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<String, String> eintrag : namensListe.entrySet()) {
			text.append("Klarname: " + eintrag.getKey() + " - Anonym: " + eintrag.getValue() + "\n");
		}
		return text.toString();
	}

	public String rueckuebersetzen(String text) {
		if (text == null) {
			return null;
		}
		String rueckgabe = text;
		if (namensListe == null) {
			return rueckgabe;
		}
		for (Map.Entry<String, String> eintrag : namensListe.entrySet()) {
			String klarname = eintrag.getKey();
			String anonym = eintrag.getValue();
			// "linke Grenze":
			// - Stringanfang (^)
			// - Nicht-Buchstabe/Zahl/Unterstrich
			// - oder die wörtliche Zeichenkette "\n"
			String leftBoundary = "(?:(?<![\\p{L}\\p{N}_])|(?<=\\\\n))";
			String rightBoundary = "(?![\\p{L}\\p{N}_])";

			// erst die Form mit "s", dann den Namen
			String regexMitS = "(?U)" + leftBoundary + Pattern.quote(anonym) + "s" + rightBoundary;
			String regex = "(?U)" + leftBoundary + Pattern.quote(anonym) + rightBoundary;

			rueckgabe = rueckgabe.replaceAll(regexMitS, Matcher.quoteReplacement(klarname + "s"));
			rueckgabe = rueckgabe.replaceAll(regex, Matcher.quoteReplacement(klarname));
		}

		return rueckgabe;
	}

	public String anonymisiereText(String text) {
		String ausgabe = text;

		if (!namensListe.isEmpty()) {
			for (Map.Entry<String, String> eintrag : namensListe.entrySet()) {
				String klarname = eintrag.getKey();
				String anonym = eintrag.getValue();

				// Ersetze nur ganze Wörter: \b für Wortgrenzen, Pattern.quote schützt vor
				// Sonderzeichen
				ausgabe = ausgabe.replaceAll("\\b" + Pattern.quote(klarname) + "\\b", anonym);
			}
		}

		return ausgabe;
	}

	public String formatName(String vereinsnr, String input, NamensSpeicher namensSpeicher) {
		return formatName2(vereinsnr, input, namensSpeicher, true);
	}

	public String formatName(String vereinsnr, String input, NamensSpeicher namensSpeicher, boolean verschluesseln) {
		return formatName2(vereinsnr, input, namensSpeicher, verschluesseln);
	}

	public String formatName2(String vereinsnr, String input, NamensSpeicher namensSpeicher, boolean verschluesseln) {

		String normalizedInput = normalizeWhitespace(input);
		if (normalizedInput.contains("nicht anwesend") || normalizedInput.contains("unbekannt")
				|| normalizedInput.contains("nachgenannt") || normalizedInput.equals("/")) {
			return input;
		} else if (normalizedInput.contains("/")) {
			String[] parts = normalizedInput.split("/");
			NamePart name1 = splitName(parts.length > 0 ? parts[0] : "");
			NamePart name2 = splitName(parts.length > 1 ? parts[1] : "");
			return anonymizedFullName(name1, vereinsnr, namensSpeicher, verschluesseln) + " / "
					+ anonymizedFullName(name2, vereinsnr, namensSpeicher, verschluesseln);
		} else {

			return anonymizedFullName(splitName(normalizedInput), vereinsnr, namensSpeicher, verschluesseln);
		}
	}

	public String formatName1(String vereinnr, String input, NamensSpeicher namensSpeicher, boolean verschluesseln) {
		return formatName2(vereinnr, input, namensSpeicher, verschluesseln);
	}

	public static NamePart splitName(String name) {
		return new NamePart(name);
	}

	private static String normalizeWhitespace(String input) {
		if (input == null) {
			return "";
		}
		return input.trim().replaceAll("\\s+", " ");
	}

	private String anonymizedFullName(NamePart parts, String vereinsnr, NamensSpeicher namensSpeicher,
			boolean verschluesseln) {
		String vorname = parts.getVorname().isEmpty() ? ""
				: namensSpeicher.speichereNamen(parts.getVorname(), true, vereinsnr, verschluesseln);
		String nachname = parts.getNachname().isEmpty() ? ""
				: namensSpeicher.speichereNamen(parts.getNachname(), false, vereinsnr, verschluesseln);
		return (vorname + " " + nachname).trim();
	}

	public void fuelleNamensspeicher(String vereinnr, String url, NamensSpeicher namensSpeicher) throws IOException {
		fuelleNamensspeicher(vereinnr, url, namensSpeicher, true);
	}

	public void fuelleNamensspeicher(String vereinnr, String url, NamensSpeicher namensSpeicher, Boolean verschluessen)
			throws IOException {

		SpielergebnisProvider provider = SpielergebnisFactory.create(vereinnr, url, namensSpeicher, verschluessen);
		List<? extends SpielDetail> spiele = provider.getSummary().getSpiele();

	}

	public void fuelleNamensspeicher(String vereinnr, SpielergebnisProvider provider, NamensSpeicher namensSpeicher)
			throws IOException {

		List<? extends SpielDetail> spiele = provider.getSummary().getSpiele();

		for (SpielDetail spiel : spiele) {
			String spielheim = null;
			String spielgast = null;

			if (spiel.getPosition().startsWith("D")) {
				spielheim = formatName(vereinnr, spiel.getHeim(), namensSpeicher);
				spielgast = formatName(vereinnr, spiel.getGast(), namensSpeicher);
			} else {
				spielheim = formatName(vereinnr, spiel.getHeim(), namensSpeicher);
				spielgast = formatName(vereinnr, spiel.getGast(), namensSpeicher);
			}

		}

	}

}