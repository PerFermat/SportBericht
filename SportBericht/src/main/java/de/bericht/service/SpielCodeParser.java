package de.bericht.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import de.bericht.util.ConfigManager;
import de.bericht.util.SpielCode;

public class SpielCodeParser {

	private List<SpielCode> spiele;

	/*
	 * Zusätzliche feste Marker zwischen Halle und Mannschaft
	 */
	private static final Set<String> ZUSATZ_MARKER = Set.of("t", "v", "t/v");

	public SpielCodeParser() {
	}

	public SpielCodeParser(String vereinnr, String mannschaft, String liga, PDDocument pdfDokument) {

		this(vereinnr, mannschaft, liga, pdfDokument, true);
	}

	public SpielCodeParser(String vereinnr, String mannschaft, String liga, PDDocument pdfDokument,
			boolean datumUhrzeitErsetzen) {

		try {

			spiele = parseFromPDF(vereinnr, mannschaft, liga, pdfDokument);

			DatabaseService db = new DatabaseService();

			db.insertSpielCode(vereinnr, spiele, datumUhrzeitErsetzen);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<SpielCode> parseFromPDF(String vereinnr, String mannschaft, String liga, PDDocument document)
			throws Exception {

		List<SpielCode> spiele = new ArrayList<>();

		/*
		 * Verein dynamisch laden
		 */
		String verein = ConfigManager.getSpielplanVerein(vereinnr).trim();

		String[] vereinTokens = verein.split("\\s+");

		PDFTextStripper stripper = new PDFTextStripper();

		String text = stripper.getText(document);

		String[] lines = text.split("\\r?\\n");

		for (String line : lines) {

			line = line.trim();

			if (line.isEmpty()) {
				continue;
			}

			// Nur Spielzeilen verarbeiten
			if (!(line.startsWith("Sa.") || line.startsWith("So.") || line.startsWith("Mo.") || line.startsWith("Di.")
					|| line.startsWith("Mi.") || line.startsWith("Do.") || line.startsWith("Fr."))) {

				continue;
			}

			String[] tokens = line.split("\\s+");

			if (tokens.length < 7) {
				continue;
			}

			try {

				int token = 0;

				String wochentag = tokens[token++];

				String datum = tokens[token++];

				String uhrzeit = tokens[token++];

				// Halle überspringen
				token++;

				// optionale Marker überspringen
				while (token < tokens.length && ZUSATZ_MARKER.contains(tokens[token])) {

					token++;
				}

				// letztes Token = Spielcode
				String spielCode = tokens[tokens.length - 1];

				/*
				 * Suche Verein innerhalb der Tokens
				 */
				int vereinIndex = -1;

				for (int i = token; i <= tokens.length - vereinTokens.length - 1; i++) {

					boolean match = true;

					for (int j = 0; j < vereinTokens.length; j++) {

						if (!tokens[i + j].equals(vereinTokens[j])) {

							match = false;
							break;
						}
					}

					if (match) {
						vereinIndex = i;
						break;
					}
				}

				/*
				 * Verein nicht gefunden
				 */
				if (vereinIndex == -1) {
					continue;
				}

				String heimMannschaft = "";
				String gastMannschaft = "";

				/*
				 * Vereinsname inkl. optionaler römischer Zahl
				 */
				String vereinMitZusatz = verein;

				int vereinEndIndex = vereinIndex + vereinTokens.length - 1;

				if (vereinEndIndex + 1 < tokens.length && isRoman(tokens[vereinEndIndex + 1])) {

					vereinMitZusatz += " " + tokens[vereinEndIndex + 1];

					vereinEndIndex++;
				}

				/*
				 * Verein früh -> Heim Verein spät -> Gast
				 */
				if (vereinIndex <= token + 1) {

					/*
					 * Heimspiel
					 */
					heimMannschaft = vereinMitZusatz;

					StringBuilder gast = new StringBuilder();

					for (int i = vereinEndIndex + 1; i < tokens.length - 1; i++) {

						gast.append(tokens[i]).append(" ");
					}

					gastMannschaft = gast.toString().trim();

				} else {

					/*
					 * Auswärtsspiel
					 */
					gastMannschaft = vereinMitZusatz;

					StringBuilder heim = new StringBuilder();

					for (int i = token; i < vereinIndex; i++) {

						heim.append(tokens[i]).append(" ");
					}

					heimMannschaft = heim.toString().trim();
				}

				heimMannschaft = cleanTeamName(heimMannschaft);

				gastMannschaft = cleanTeamName(gastMannschaft);

				if (liga.startsWith("E") && heimMannschaft.equals(verein)) {
					heimMannschaft += " I";
				}
				if (liga.startsWith("E") && gastMannschaft.equals(verein)) {
					gastMannschaft += " I";
				}

				if (spielCode.length() <= 4) {

					spiele.add(new SpielCode(mannschaft, liga, wochentag, datum, uhrzeit, heimMannschaft,
							gastMannschaft, "", spielCode));

				} else {

					spiele.add(new SpielCode(mannschaft, liga, wochentag, datum, uhrzeit, heimMannschaft,
							gastMannschaft, spielCode, ""));
				}

			} catch (Exception e) {

				System.out.println("Fehler beim Parsen der Zeile:");

				System.out.println(line);

				e.printStackTrace();
			}
		}

		return spiele;
	}

	private boolean isRoman(String text) {

		return text.matches("I|II|III|IV|V|VI|VII|VIII|IX|X");
	}

	private String cleanTeamName(String team) {

		if (team == null) {
			return "";
		}

		return team.replaceAll("\\s+", " ").trim();
	}

	public List<SpielCode> getSpiele() {
		return spiele;
	}
}