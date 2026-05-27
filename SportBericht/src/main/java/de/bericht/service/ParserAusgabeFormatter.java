package de.bericht.service;

import java.util.List;

public final class ParserAusgabeFormatter {

	private ParserAusgabeFormatter() {
	}

	public record SuchbegriffFarbe(String suchbegriff, String wochentag, String cssStyleName, String cssStyle) {
	}

	private static final List<SuchbegriffFarbe> SUCHBEGRIFFE_FARBEN = List.of(
			new SuchbegriffFarbe("Halle", "Mo,Fr", "halle",
					".halle{background-color:#ffebee;border-left:6px solid #c62828}"),
			new SuchbegriffFarbe("Tischtennis", "Mo,Di,Mi,Do,Fr,Sa,So", "tischtennis",
					".tischtennis{background-color:#e8f5e9;border-left:6px solid #2e7d32}"),
			new SuchbegriffFarbe("TT-", "Mo,Di,Mi,Do,Fr,Sa,So", "tischtennis",
					".tischtennis{background-color:#e8f5e9;border-left:6px solid #2e7d32}"),
			new SuchbegriffFarbe("Heimspiel", "heimspiel", "heimspiel",
					".heimspiel{background-color:#e3f2fd;border-left:6px solid #1565c0}"),
			new SuchbegriffFarbe("Manuell", "manuell", "manuell",
					".manuell{background-color:#D1D1D1;border-left:6px solid #595959}"));

	public static boolean sucheBegriff(String text, String wochentag) {

		for (SuchbegriffFarbe suchbegriffFarbe : SUCHBEGRIFFE_FARBEN) {
			if (text.contains(suchbegriffFarbe.suchbegriff) && suchbegriffFarbe.wochentag.contains(wochentag)) {
				return true;
			}
		}
		return false;
	}

	public static String css() {
		StringBuilder css = new StringBuilder();
		css.append(".tag{margin-top:25px;padding:10px;border-radius:8px}");
		css.append(".titel{font-size:18px;font-weight:bold;margin-bottom:10px}");
		css.append(
				".hinweis{background:#FFFFFF;border:1px solid #FFFFFF;border-radius:8px;padding:8px;margin-bottom:8px}");
		for (SuchbegriffFarbe suchbegriffFarbe : SUCHBEGRIFFE_FARBEN) {
			css.append(suchbegriffFarbe.cssStyle);
		}
		css.append(".inhalt{white-space:pre-line}");

		return css.toString();
	}

	public static String formatBlock(String titel, String blockText, String wochentag) {
		String cssClass = "";
		for (SuchbegriffFarbe suchbegriffFarbe : SUCHBEGRIFFE_FARBEN) {
			if ((blockText.contains(suchbegriffFarbe.suchbegriff) && suchbegriffFarbe.wochentag.contains(wochentag))
					|| suchbegriffFarbe.wochentag.equals(wochentag)) {
				cssClass = suchbegriffFarbe.cssStyleName;
				break;
			}
		}

		StringBuilder html = new StringBuilder();
		html.append("<div class='tag ").append(cssClass).append("'>").append("<div class='titel'>")
				.append(escapeHtml(titel)).append("</div>").append("<div class='inhalt'>")
				.append(formatBlockText(blockText)).append("</div>").append("</div>");
		return html.toString();
	}

	public static String formatBlockText(String text) {
		String ausgabe = escapeHtml(text);
		for (SuchbegriffFarbe suchbegriffFarbe : SUCHBEGRIFFE_FARBEN) {
			ausgabe = ausgabe.replace(suchbegriffFarbe.suchbegriff,
					"<strong>" + suchbegriffFarbe.suchbegriff + "</strong>");
		}
		return ausgabe;
	}

	public static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
