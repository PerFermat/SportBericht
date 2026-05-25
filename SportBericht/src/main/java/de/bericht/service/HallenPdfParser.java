package de.bericht.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class HallenPdfParser {

	private final String htmlText;
	private final String plainText;

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

				        .tag {
				            margin-top: 25px;
				            padding: 10px;
				            border-radius: 8px;
				        }

				        .tischtennis {
				            background-color: #e8f5e9;
				            border-left: 6px solid #2e7d32;
				        }

				        .halle {
				            background-color: #ffebee;
				            border-left: 6px solid #c62828;
				        }

				        .titel {
				            font-size: 18px;
				            font-weight: bold;
				            margin-bottom: 10px;
				        }

				        .inhalt {
				            white-space: pre-line;
				        }
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

		html.append("<div class='tag ").append(cssClass).append("'>");

		html.append("<div class='titel'>").append(tag).append(" ").append(titel).append("</div>");

		html.append("<div class='inhalt'>").append(escapeHtml(blockText).replace("TT-", "<strong>TT-</strong>")
				.replace("Tischtennis", "<strong>Tischtennis</strong>").replace("Halle", "<strong>Halle</strong>"))
				.append("</div>");

		html.append("</div>");
	}

	/**
	 * HTML escapen.
	 */
	private String escapeHtml(String text) {

		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}