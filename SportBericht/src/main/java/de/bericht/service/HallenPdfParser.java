package de.bericht.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class HallenPdfParser {

	private final String htmlText;
	private final String plainText;
	private final List<ParserBlock> parserBloecke = new ArrayList<>();

	/**
	 * Konstruktor: Interpretiert direkt das PDF aus dem InputStream.
	 */
	public HallenPdfParser(InputStream pdfInputStream) throws IOException {

		String pdfText = lesePdf(pdfInputStream);

		this.htmlText = parseHallenTermine(pdfText);
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

	public List<ParserBlock> getParserBloecke() {
		return parserBloecke;
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
	private String parseHallenTermine(String text) {

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
				speichereBlock(html, aktuellerTag, aktuellerWochentag, aktuellerBlock.toString());

				aktuellerTag = matcher.group(1);
				aktuellerWochentag = matcher.group(2);

				aktuellerBlock = new StringBuilder();
				aktuellerBlock.append(zeile).append("\n");

			} else {

				aktuellerBlock.append(zeile).append("\n");
			}
		}

		// Letzten Block speichern
		speichereBlock(html, aktuellerTag, aktuellerWochentag, aktuellerBlock.toString());

		html.append("""
				</body>
				</html>
				""");

		return html.toString();
	}

	/**
	 * Block prüfen und ggf. als HTML hinzufügen.
	 */
	private void speichereBlock(StringBuilder html, String tag, String wochentag, String blockText) {

		if (tag == null || wochentag == null) {
			return;
		}

		String cssClass;
		if (!(blockText.contains("TT-") || blockText.contains("Tischtennis"))) {
			// Nur Montag und Freitag
			if (!wochentag.equals("Mo") && !wochentag.equals("Fr")) {
				return;
			}

			// Nur Einträge mit Halle
			if (!blockText.contains("Halle")) {
				return;
			}
			cssClass = "halle";
		} else {
			cssClass = "tischtennis";
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
		html.append(ParserAusgabeFormatter.formatBlock(tag + " " + titel, blockText, "halle".equals(cssClass)));
		parserBloecke.add(new ParserBlock(tag, titel, blockText));
	}

	public static class ParserBlock implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String tag;
		private final String wochentag;
		private final String text;

		public ParserBlock(String tag, String wochentag, String text) {
			this.tag = tag;
			this.wochentag = wochentag;
			this.text = text;
		}

		public String getTag() {
			return tag;
		}

		public String getWochentag() {
			return wochentag;
		}

		public String getText() {
			return text;
		}
	}

}