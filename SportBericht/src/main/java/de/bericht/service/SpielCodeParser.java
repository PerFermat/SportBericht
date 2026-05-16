package de.bericht.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import de.bericht.util.SpielCode;

public class SpielCodeParser {
	private List<SpielCode> spiele;

	public SpielCodeParser() {
	}

	public SpielCodeParser(String vereinnr, String mannschaft, String liga, String typ, PDDocument pdfDokument) {
		try {
			spiele = parseFromPDF(mannschaft, liga, typ, pdfDokument);
			DatabaseService db = new DatabaseService();
			db.insertSpielCode(vereinnr, spiele);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Regex-Muster für römische Zahlen (I bis X)
	private static final Pattern ROMAN_PATTERN = Pattern.compile("^(I|II|III|IV|V|VI|VII|VIII|IX|X)$");

	public List<SpielCode> parseFromPDF(String mannschaft, String liga, String art, PDDocument document)
			throws Exception {
		List<SpielCode> spiele = new ArrayList<>();

		PDFTextStripper stripper;
		stripper = new PDFTextStripper();
		String text = stripper.getText(document);

		// Zeilenweise verarbeiten
		String[] lines = text.split("\\r?\\n");

		// Überspringe Header-Zeilen (z.B. die Zeile mit "Datum, Uhrzeit ..." und
		// weitere Meta-Daten)
		boolean headerDone = false;
		for (String line : lines) {
			// Prüfen, ob die Zeile mit einem Wochentag beginnt
			if (line.startsWith("Sa.") || line.startsWith("So.") || line.startsWith("Mo.") || line.startsWith("Di.")
					|| line.startsWith("Mi.") || line.startsWith("Do.") || line.startsWith("Fr.")) {
				headerDone = true;
			}
			if (!headerDone) {
				continue; // überspringe Zeilen vor dem ersten Datensatz
			}
			// Verarbeite nur Zeilen, die mit einem Wochentag beginnen
			if (line.startsWith("Sa.") || line.startsWith("So.") || line.startsWith("Mo.") || line.startsWith("Di.")
					|| line.startsWith("Mi.") || line.startsWith("Do.") || line.startsWith("Fr.")) {

				// Zeile in Tokens aufteilen; als Trenner werden ein oder mehrere Leerzeichen
				// verwendet
				String[] tokens = line.trim().split("\\s+");
				if (tokens.length < 8) {
					// Falls nicht genügend Tokens vorhanden sind, Zeile überspringen
					continue;
				}

				// Die ersten vier Tokens: Wochentag, Datum, Uhrzeit, Spielanzahl (z.B. "(1)")
				int token = 0;
				String wochentag = "";
				String datum = "";
				if (tokens[token].length() > 4) {
					System.out.println("Token " + tokens[token]);
					System.out.println("Token " + tokens[token + 1]);
					wochentag = tokens[token].substring(1, 3);
					datum = tokens[token++].substring(4);
				} else {
					wochentag = tokens[token++];
					datum = tokens[token++];
				}
				String uhrzeit = tokens[token++];
				token++;
				if (token < tokens.length
						&& ("t".equals(tokens[token]) || "t/v".equals(tokens[token]) || "v".equals(tokens[token]))) {
					token++;
				}

				// Bestimme, ob die Heimmannschaft aus 2 oder 3 Tokens besteht:
				// Es wird geprüft, ob der Token nach den ersten beiden Namensbestandteilen
				// (also tokens[6])
				// eine römische Zahl ist.
				int heimTokenCount = 2;
				if (tokens.length > token + heimTokenCount && tokens[token + heimTokenCount].startsWith("(")) {
					System.out.println("( gefunden)" + tokens[token + heimTokenCount]);
					heimTokenCount++;
				}
				if (tokens.length > token + heimTokenCount
						&& ROMAN_PATTERN.matcher(tokens[token + heimTokenCount]).matches()) {
					heimTokenCount++;
				}
				// Heimteam: tokens[4] bis tokens[4 + heimTokenCount - 1]
				StringBuilder heimMannschaftBuilder = new StringBuilder();
				for (int i = 0; i < heimTokenCount && token < tokens.length; i++) {
					heimMannschaftBuilder.append(tokens[token++]).append(" ");
				}
				String heimMannschaft = heimMannschaftBuilder.toString().trim();

				// Bestimme den Startindex für die Gastmannschaft
				int guestStart = token;
				int guestTokenCount = tokens.length - guestStart - 1;
				// Gastteam: von tokens[guestStart] bis tokens[guestStart + guestTokenCount - 1]
				StringBuilder gastMannschaftBuilder = new StringBuilder();
				for (int i = guestStart; i < guestStart + guestTokenCount; i++) {
					gastMannschaftBuilder.append(tokens[i]).append(" ");
				}
				String gastMannschaft = gastMannschaftBuilder.toString().trim();

				// Das letzte Token ist der Spiel-Code
				String spielCode = tokens[tokens.length - 1];

				if (liga.startsWith("E") && heimMannschaft.equals("TSGV Hattenhofen")) {
					heimMannschaft += " I";
				}
				if (liga.startsWith("E") && gastMannschaft.equals("TSGV Hattenhofen")) {
					gastMannschaft += " I";
				}

				// Erstelle das SpielCode-Objekt und füge es zur Liste hinzu
				if ("Pin".equals(art)) {
					spiele.add(new SpielCode(mannschaft, liga, wochentag, datum, uhrzeit, heimMannschaft,
							gastMannschaft, "", spielCode));
				} else {
					spiele.add(new SpielCode(mannschaft, liga, wochentag, datum, uhrzeit, heimMannschaft,
							gastMannschaft, spielCode, ""));

				}
			}
		}
		return spiele;
	}

	public List<SpielCode> getSpiele() {
		return spiele;
	}
}
