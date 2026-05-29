package de.bericht.service;

import java.io.Serializable;
import java.util.List;

import de.bericht.controller.FtpBean.ManuellerTagEintrag;
import de.bericht.service.HallenPdfParser.ParserBlock;

public class TerminMitStatus implements Serializable, Comparable<TerminMitStatus> {
	private static final long serialVersionUID = 1L;
	private String status = "Überprüfe";
	private final String htmlText;
	private String wochentag;
	private int tag;

	public TerminMitStatus(String tag, String htmlText, String wochentag) {
		this.htmlText = htmlText;
		this.wochentag = wochentag;
		this.tag = numTag(tag);
	}

	public TerminMitStatus(ManuellerTagEintrag manEintrag, List<ParserBlock> terminEintraege) {
		tag = 99;
		String text = null;
		String wochentag = null;
		for (ParserBlock terminEintrag : terminEintraege) {
			if (tagGleich(terminEintrag.getTag(), manEintrag.getTag())) {
				this.tag = numTag(terminEintrag.getTag());
				text = terminEintrag.getText();
				wochentag = terminEintrag.getWochentag();
			}
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

		htmlText = ParserAusgabeFormatter.formatBlock(manEintrag.getTag() + " [status]",
				manEintrag.getText() + "\n\n" + text, "manuell");
		this.wochentag = "manuell";
	}

	public TerminMitStatus(Heimspiele heim, String spiele, List<ParserBlock> terminEintraege) {
		tag = 99;
		String text = null;
		String wochentag = null;
		for (ParserBlock terminEintrag : terminEintraege) {
			if (tagGleich(terminEintrag.getTag(), heim.getTagText())) {
				this.tag = numTag(terminEintrag.getTag());
				text = terminEintrag.getText();
				wochentag = terminEintrag.getWochentag();
			}
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

		htmlText = ParserAusgabeFormatter.formatBlock(
				heim.getTagText().replace("Halle", "<strong>Halle</strong>") + ". " + titel + "[status]",
				spiele + "\n\n" + text, "heimspiel");
		this.wochentag = "heimspiel";
	}

	private boolean tagGleich(String tag1, String tag2) {
		return normalizeTag(tag1).equals(normalizeTag(tag2));
	}

	private String normalizeTag(String tag) {
		if (tag == null) {
			return "";
		}

		// Alle Nicht-Ziffern durch Leerzeichen ersetzen
		tag = tag.replaceAll("[^0-9]", " ").trim();

		// Nur den ersten Zahlenblock nehmen
		String[] parts = tag.split("\\s+");
		if (parts.length == 0) {
			return "";
		}

		try {
			return String.valueOf(Integer.parseInt(parts[0]));
		} catch (NumberFormatException e) {
			return parts[0]; // falls doch unerwartet kein gültiges Format
		}
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getHtmlText(String status) {
		return htmlText.replace("[status]", " - " + status);
	}

	public String getHtmlTextAnzeige() {
		return htmlText.replace("[status]", "");
	}

	public String getWochentag() {
		return wochentag;
	}

	@Override
	public int compareTo(TerminMitStatus other) {
		// zuerst nach Tag sortieren
		int result = Integer.compare(this.tag, other.tag);

		if (result != 0) {
			return result;
		}

		// optional: wenn gleicher Tag, nach Wochentag sortieren
		return this.wochentag.compareTo(other.wochentag);
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}
}
