package de.bericht.service;

public final class ParserAusgabeFormatter {

	private ParserAusgabeFormatter() {
	}

	public static String css() {
		return ".tag{margin-top:25px;padding:10px;border-radius:8px}.tischtennis{background-color:#e8f5e9;border-left:6px solid #2e7d32}.halle{background-color:#ffebee;border-left:6px solid #c62828}.titel{font-size:18px;font-weight:bold;margin-bottom:10px}.inhalt{white-space:pre-line}";
	}

	public static String formatBlock(String titel, String blockText, boolean halle) {
		String cssClass = halle ? "halle" : "tischtennis";
		StringBuilder html = new StringBuilder();
		html.append("<div class='tag ").append(cssClass).append("'>").append("<div class='titel'>")
				.append(escapeHtml(titel)).append("</div>").append("<div class='inhalt'>")
				.append(formatBlockText(blockText)).append("</div>").append("</div>");
		return html.toString();
	}

	public static String formatBlockText(String text) {
		return escapeHtml(text).replace("TT-", "<strong>TT-</strong>")
				.replace("Tischtennis", "<strong>Tischtennis</strong>").replace("Halle", "<strong>Halle</strong>");
	}

	public static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
