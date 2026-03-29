package de.bericht.service;

import org.jsoup.nodes.Element;

import de.bericht.util.NamensSpeicher;

public class TennisDoppelInfo {

	private final TennisSpielerInfo spieler1;
	private final TennisSpielerInfo spieler2;

	public TennisDoppelInfo(String vereinnr, TennisSpielerInfo spieler1, TennisSpielerInfo spieler2, NamensSpeicher ns,
			Boolean verschluesseln) {
		this.spieler1 = spieler1 == null ? new TennisSpielerInfo(vereinnr, "", "", "", "", ns, verschluesseln)
				: spieler1;
		this.spieler2 = spieler2 == null ? new TennisSpielerInfo(vereinnr, "", "", "", "", ns, verschluesseln)
				: spieler2;
	}

	public static TennisDoppelInfo fromCell(String vereinnr, Element cell, NamensSpeicher ns, Boolean verschluesseln) {
		String html = cell == null ? "" : cell.html();
		return new TennisDoppelInfo(vereinnr, TennisSpielerInfo.parseCellHtml(vereinnr, html, 0, ns, verschluesseln),
				TennisSpielerInfo.parseCellHtml(vereinnr, html, 1, ns, verschluesseln), ns, verschluesseln);
	}

	public TennisSpielerInfo getSpieler1() {
		return spieler1;
	}

	public TennisSpielerInfo getSpieler2() {
		return spieler2;
	}
}