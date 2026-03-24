package de.bericht.service;

import org.jsoup.nodes.Element;

public class TennisDoppelInfo {

	private final TennisSpielerInfo spieler1;
	private final TennisSpielerInfo spieler2;

	public TennisDoppelInfo(TennisSpielerInfo spieler1, TennisSpielerInfo spieler2) {
		this.spieler1 = spieler1 == null ? new TennisSpielerInfo("", "", "", "") : spieler1;
		this.spieler2 = spieler2 == null ? new TennisSpielerInfo("", "", "", "") : spieler2;
	}

	public static TennisDoppelInfo fromCell(Element cell) {
		String html = cell == null ? "" : cell.html();
		return new TennisDoppelInfo(TennisSpielerInfo.parseCellHtml(html, 0), TennisSpielerInfo.parseCellHtml(html, 1));
	}

	public TennisSpielerInfo getSpieler1() {
		return spieler1;
	}

	public TennisSpielerInfo getSpieler2() {
		return spieler2;
	}
}