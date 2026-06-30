package de.bericht.service;

import java.util.Arrays;
import java.util.List;

import de.bericht.util.enums.SuchbegriffCss;
import de.bericht.util.enums.TerminStatus;
import jakarta.faces.model.SelectItem;

public final class ParserAusgabeFormatter {

	private ParserAusgabeFormatter() {
	}

	public record SuchbegriffFarbe(String suchbegriff, String wochentag, String cssStyleName, SuchbegriffCss farbe,
			List<TerminStatus> statusOption) {

		public List<SelectItem> getSelectItems() {
			return statusOption.stream().map(TerminStatus::toSelectItem).toList();
		}

		public String getCssStyle() {
			return farbe.createCss(cssStyleName);
		}
	}

	private static final List<SuchbegriffFarbe> SUCHBEGRIFFE_FARBEN = List.of(

			new SuchbegriffFarbe("Halle", "Mo,Fr", "halle", SuchbegriffCss.HALLE,
					List.of(TerminStatus.TERMINOK, TerminStatus.TERMINFEHLT, TerminStatus.NICHT_RELEVANT,
							TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("Feiertag", "Mo,Fr", "feiertag", SuchbegriffCss.GELB,
					List.of(TerminStatus.TERMINOK, TerminStatus.TERMINFEHLT, TerminStatus.NICHT_RELEVANT,
							TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("Tischtennis", "Mo,Fr", "tischtennis", SuchbegriffCss.TISCHTENNIS,
					List.of(TerminStatus.TERMINOK, TerminStatus.TERMINFEHLT, TerminStatus.NICHT_RELEVANT,
							TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("TT-", "Mo,Fr", "tischtennis", SuchbegriffCss.TISCHTENNIS,
					List.of(TerminStatus.TERMINOK, TerminStatus.TERMINFEHLT, TerminStatus.NICHT_RELEVANT,
							TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("Tischtennis", "Di,Mi,Do,Sa,So", "tischtennis", SuchbegriffCss.TISCHTENNIS,
					List.of(TerminStatus.TERMINOK, TerminStatus.TERMINFEHLT, TerminStatus.HALLE_FREIGEBEN,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("TT-", "Di,Mi,Do,Sa,So", "tischtennis", SuchbegriffCss.TISCHTENNIS,
					List.of(TerminStatus.TERMINOK, TerminStatus.TERMINFEHLT, TerminStatus.HALLE_FREIGEBEN,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("Heimspiel", "heimspiel", "heimspiel", SuchbegriffCss.HEIMSPIEL,
					List.of(TerminStatus.SPIELTAG_OK, TerminStatus.SPIELTAG_KRITISCH, TerminStatus.HALLE_FREIGEBEN,
							TerminStatus.NICHT_RELEVANT, TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("Ferien", "Mo,Fr", "ferien", SuchbegriffCss.GELB,
					List.of(TerminStatus.TERMINOK, TerminStatus.TERMINFEHLT, TerminStatus.NICHT_RELEVANT,
							TerminStatus.UEBERPRUEFE)),

			new SuchbegriffFarbe("Manuell", "manuell", "manuell", SuchbegriffCss.MANUELL,
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
			css.append(suchbegriffFarbe.getCssStyle());
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
