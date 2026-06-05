package de.bericht.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import de.bericht.util.SchoolHolidayApiClient;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;

public class HallenPdfParser {

	private final String htmlText;
	private final String plainText;
	private List<String> ueberschrift = new ArrayList<>();
	private final List<Hallenbelegung> parserBloecke = new ArrayList<>();
	private final List<Hallenbelegung> parserAlle = new ArrayList<>();
	private YearMonth pdfMonat;

	private static final HolidayManager HOLIDAY_MANAGER = HolidayManager.getInstance(ManagerParameters.create("de"));

	/**
	 * Konstruktor: Interpretiert direkt das PDF aus dem InputStream.
	 */
	public HallenPdfParser(String vereinnr, InputStream pdfInputStream, YearMonth pdfMonat) throws IOException {
		this.pdfMonat = pdfMonat;

		String pdfText = lesePdf(pdfInputStream);

		this.htmlText = parseHallenTermine(vereinnr, pdfText);
		this.plainText = pdfText;
	}

	/**
	 * HTML-Ausgabe der gefundenen Montag/Freitag-Hallentermine.
	 */
	public String getHtmlText() {
		return htmlText;
	}

	public String getPlainText() {
		return plainText;
	}

	public List<Hallenbelegung> getParserBloecke() {
		return parserBloecke;
	}

	public List<Hallenbelegung> getParserAlle() {
		return parserAlle;
	}

	/**
	 * PDF lesen.
	 */
	private String lesePdf(InputStream inputStream) throws IOException {

		try (PDDocument document = PDDocument.load(inputStream)) {

			PDFTextStripper stripper = new PDFTextStripper();

			return stripper.getText(document);
		}
	}

	/**
	 * PDF-Inhalt interpretieren und HTML erzeugen.
	 */
	private String parseHallenTermine(String vereinnr, String text) {

		String[] zeilen = text.split("\\R");

		StringBuilder html = new StringBuilder();

		html.append("""
				<html>
				<head>
				    <style>
				        body {
				            font-family: Arial, sans-serif;
				            font-size: 14px;
				            line-height: 1.5;
				        }

				      """ + ParserAusgabeFormatter.css() + """
				    </style>
				</head>
				<body>
				<h2>Hallenbelegung - Montag & Freitag</h2>
				""");

		Pattern pattern = Pattern.compile("^(\\d+\\.)\\s+(Mo|Di|Mi|Do|Fr|Sa|So).*");

		String aktuellerTag = null;
		String aktuellerWochentag = null;

		StringBuilder aktuellerBlock = new StringBuilder();

		for (String zeile : zeilen) {

			Matcher matcher = pattern.matcher(zeile.trim());

			if (matcher.matches()) {

				// Vorherigen Block speichern
				speichereBlock(vereinnr, html, aktuellerTag, aktuellerWochentag, aktuellerBlock.toString(), null);

				aktuellerTag = matcher.group(1);
				aktuellerWochentag = matcher.group(2);

				aktuellerBlock = new StringBuilder();
				aktuellerBlock.append(zeile).append("\n");

			} else {

				aktuellerBlock.append(zeile).append("\n");
			}
		}

		// Letzten Block speichern
		speichereBlock(vereinnr, html, aktuellerTag, aktuellerWochentag, aktuellerBlock.toString(), null);

		html.append("""
				</body>
				</html>
				""");

		return html.toString();
	}

	/**
	 * Block prüfen und ggf. als HTML hinzufügen.
	 */
	private void speichereBlock(String vereinnr, StringBuilder html, String tag, String wochentag, String blockText,
			List<Heimspiele> spieleDaheim) {

		if (tag == null || wochentag == null) {
			return;
		}

		String titel = switch (wochentag) {
		case "Mo" -> "Montag";
		case "Di" -> "Dienstag";
		case "Mi" -> "Mittwoch";
		case "Do" -> "Donnerstag";
		case "Fr" -> "Freitag";
		case "Sa" -> "Samstag";
		case "So" -> "Sonntag";
		default -> "Unbekannter Tag";
		};
		this.ueberschrift.add(tag + " " + titel);
		parserAlle.add(new Hallenbelegung(tag, wochentag, titel, blockText));

		LocalDate datum = LocalDate.of(pdfMonat.getYear(), pdfMonat.getMonth(), numTag(tag));
		String feiertag = erstelleFeiertagTermin(datum);
		if (feiertag != null) {
			blockText = feiertag + "\n\n" + blockText;
		}

		SchoolHolidayApiClient api = new SchoolHolidayApiClient(vereinnr);
		SchoolHoliday ferien = api.getHolidayIfPresent(datum);
		if (ferien != null) {
			blockText = "Ferien: " + ferien.getName() + "\n\n" + blockText;
		}

		if (!ParserAusgabeFormatter.sucheBegriff(blockText, wochentag)) {
			return;
		}
		html.append(ParserAusgabeFormatter.formatBlock(tag + " " + titel, blockText, wochentag));
		parserBloecke.add(new Hallenbelegung(tag, wochentag, titel, blockText));
	}

	private int numTag(String tag) {
		if (tag == null) {
			return 0;
		}

		// Punkt entfernen und trimmen
		tag = tag.replace(".", "").trim();

		// führende Nullen entfernen (optional, aber sinnvoll)
		try {

			return Integer.parseInt(tag);
		} catch (NumberFormatException e) {
			return 99; // falls doch mal kein Zahlformat
		}
	}

	private String erstelleFeiertagTermin(LocalDate datum) {
		return HOLIDAY_MANAGER.getHolidays(datum.getYear(), "bw").stream().filter(h -> h.getDate().equals(datum))
				.map(h -> "Feiertag: " + h.getDescription()).findFirst().orElse(null);
	}

	public List<String> getUeberschrift() {
		return ueberschrift;
	}

	public YearMonth getPdfMonat() {
		return pdfMonat;
	}

}