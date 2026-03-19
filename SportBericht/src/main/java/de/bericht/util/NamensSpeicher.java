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
		if (input.contains(",")) {
			return formatName1(vereinsnr, input, namensSpeicher, true);
		}
		return formatName2(vereinsnr, input, namensSpeicher, true);
	}

	public String formatName(String vereinsnr, String input, NamensSpeicher namensSpeicher, boolean verschluesseln) {
		if (input.contains(",")) {
			return formatName1(vereinsnr, input, namensSpeicher, verschluesseln);
		}
		return formatName2(vereinsnr, input, namensSpeicher, verschluesseln);
	}

	public String formatName2(String vereinsnr, String input, NamensSpeicher namensSpeicher, boolean verschluesseln) {

		if (input.contains("nicht anwesend") || input.contains("unbekannt") || input.contains("nachgenannt")
				|| input.equals("/".trim())) {
			return input;
		} else if (input.contains("/")) {
			String[] parts = input.split("/");
			String[] name1 = parts[0].trim().split(" ");
			String[] name2 = parts[1].trim().split(" ");
			String vorname1 = "";
			String vorname2 = "";
			String nachname1 = "";
			String nachname2 = "";

			for (int i = 0; i < name1.length; i++) {
				if (i == 0) {
					vorname1 = namensSpeicher.speichereNamen(name1[i].trim(), true, vereinsnr, verschluesseln);
				} else {
					nachname1 = nachname1 + " "
							+ namensSpeicher.speichereNamen(name1[i].trim(), false, vereinsnr, verschluesseln);
				}
			}

			for (int i = 0; i < name2.length; i++) {
				if (i == 0) {
					vorname2 = namensSpeicher.speichereNamen(name2[i].trim(), true, vereinsnr, verschluesseln);
				} else {
					nachname2 = nachname2 + " "
							+ namensSpeicher.speichereNamen(name2[i].trim(), false, vereinsnr, verschluesseln);
				}
			}

			return vorname1.trim() + " " + nachname1.trim() + " / " + vorname2.trim() + " " + nachname2.trim();
		} else {
			String[] name1 = input.trim().split(" ");
			String vorname = "";
			String nachname = "";

			for (int i = 0; i < name1.length; i++) {
				if (i == 0) {
					vorname = namensSpeicher.speichereNamen(name1[i].trim(), true, vereinsnr, verschluesseln);
				} else {
					nachname = nachname
							+ namensSpeicher.speichereNamen(name1[i].trim(), false, vereinsnr, verschluesseln);
				}
			}

			return vorname.trim() + " " + nachname.trim();
		}
	}

	public String formatName1(String vereinnr, String input, NamensSpeicher namensSpeicher, boolean verschluesseln) {

		if (input.contains("nicht anwesend")) {
			return input;
		} else if (!input.contains(",")) {
			return input;
		} else if (input.contains("/")) {
			String[] parts = input.split("/");
			String[] name1 = parts[0].trim().split(",");
			String[] name2 = parts[1].trim().split(",");
			String vorname1 = namensSpeicher.speichereNamen(name1[1].trim(), true, vereinnr, verschluesseln);
			String nachname1 = namensSpeicher.speichereNamen(name1[0].trim(), false, vereinnr, verschluesseln);
			String vorname2 = namensSpeicher.speichereNamen(name2[1].trim(), true, vereinnr, verschluesseln);
			String nachname2 = namensSpeicher.speichereNamen(name2[0].trim(), false, vereinnr, verschluesseln);
			return vorname1 + " " + nachname1 + " / " + vorname2 + " " + nachname2;

		} else {
			String[] name = input.trim().split(",");
			String vorname = namensSpeicher.speichereNamen(name[1].trim(), true, vereinnr, verschluesseln);
			String nachname = namensSpeicher.speichereNamen(name[0].trim(), false, vereinnr, verschluesseln);
			return vorname + " " + nachname;
		}

	}

	public void fuelleNamensspeicher(String vereinnr, String url, NamensSpeicher namensSpeicher) throws IOException {
		fuelleNamensspeicher(vereinnr, url, namensSpeicher, true);
	}

	public void fuelleNamensspeicher(String vereinnr, String url, NamensSpeicher namensSpeicher, Boolean verschluessen)
			throws IOException {

		SpielergebnisProvider provider = SpielergebnisFactory.create(vereinnr, url, namensSpeicher, verschluessen);
		List<MatchErgebnis> spiele = provider.getSummary().getSpiele();

//		for (MatchErgebnis spiel : spiele) {
//			String spielheim = null;
//			String spielgast = null;
//
//			spielheim = formatName(vereinnr, spiel.getHeim(), namensSpeicher, verschluessen);
//			spielgast = formatName(vereinnr, spiel.getGast(), namensSpeicher, verschluessen);
//		}

	}

	public void fuelleNamensspeicher(String vereinnr, SpielergebnisProvider provider, NamensSpeicher namensSpeicher)
			throws IOException {

		List<MatchErgebnis> spiele = provider.getSummary().getSpiele();

		for (MatchErgebnis spiel : spiele) {
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