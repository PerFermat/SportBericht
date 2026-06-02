package de.bericht.service;

import java.util.Arrays;
import java.util.List;

import de.bericht.util.enums.TerminStatus;
import jakarta.faces.model.SelectItem;

public final class ParserAusgabeFormatter {

	private ParserAusgabeFormatter() {
	}

	public record SuchbegriffFarbe(String suchbegriff, String wochentag, String cssStyleName, String cssStyle,
			List<TerminStatus> statusOption) {
		public List<SelectItem> getSelectItems() {
			return statusOption.stream().map(TerminStatus::toSelectItem).toList();
		}
	}

	private static final List<SuchbegriffFarbe> SUCHBEGRIFFE_FARBEN = List.of(
			new SuchbegriffFarbe("Halle", "Mo,Fr", "halle",
					".halle{background-color:#ffebee;border-left:6px solid #c62828}",
					List.of(TerminStatus.TRAININGSAUSFALL, TerminStatus.TRAINING, TerminStatus.TRAINING_NORMAL,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("Feiertag", "Mo,Fr", "feiertag",
					".feiertag{background-color:#fff8e1;border-left:6px solid #ff8f00}",
					List.of(TerminStatus.TRAININGSAUSFALL, TerminStatus.TRAINING, TerminStatus.TRAINING_NORMAL,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("Tischtennis", "Mo,Fr", "tischtennis",
					".tischtennis{background-color:#e8f5e9;border-left:6px solid #2e7d32}",
					List.of(TerminStatus.TRAININGSAUSFALL, TerminStatus.TRAINING, TerminStatus.TRAINING_NORMAL,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("TT-", "Mo,Fr", "tischtennis",
					".tischtennis{background-color:#e8f5e9;border-left:6px solid #2e7d32}",
					List.of(TerminStatus.TRAININGSAUSFALL, TerminStatus.TRAINING, TerminStatus.TRAINING_NORMAL,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("Tischtennis", "Di,Mi,Do,Sa,So", "tischtennis",
					".tischtennis{background-color:#e8f5e9;border-left:6px solid #2e7d32}",
					List.of(TerminStatus.TERMIN, TerminStatus.HALLE_FREIGEBEN, TerminStatus.NICHT_RELEVANT,
							TerminStatus.IGNORIEREN, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("TT-", "Di,Mi,Do,Sa,So", "tischtennis",
					".tischtennis{background-color:#e8f5e9;border-left:6px solid #2e7d32}",
					List.of(TerminStatus.TERMIN, TerminStatus.HALLE_FREIGEBEN, TerminStatus.NICHT_RELEVANT,
							TerminStatus.IGNORIEREN, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("Heimspiel", "heimspiel", "heimspiel",
					".heimspiel{background-color:#e3f2fd;border-left:6px solid #1565c0}",
					List.of(TerminStatus.SPIELTAG_OK, TerminStatus.SPIELTAG_KRITISCH, TerminStatus.HALLE_FREIGEBEN,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("Ferien", "Mo,Fr", "ferien",
					".ferien{background-color:#fff8e1;border-left:6px solid #ff8f00}",
					List.of(TerminStatus.TRAININGSAUSFALL, TerminStatus.TRAINING, TerminStatus.TRAINING_NORMAL,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),
			new SuchbegriffFarbe("Manuell", "manuell", "manuell",
					".manuell{background-color:#D1D1D1;border-left:6px solid #595959}",
					List.of(TerminStatus.values())));

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

	public static List<SelectItem> statusVarianten(String titel, String blockText, String wochentag) {
		for (SuchbegriffFarbe suchbegriffFarbe : SUCHBEGRIFFE_FARBEN) {
			if ((blockText.contains(suchbegriffFarbe.suchbegriff) && suchbegriffFarbe.wochentag.contains(wochentag))
					|| suchbegriffFarbe.wochentag.equals(wochentag)) {
				return suchbegriffFarbe.getSelectItems();
			}
		}

		return Arrays.stream(TerminStatus.values()).map(TerminStatus::toSelectItem).toList();
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
