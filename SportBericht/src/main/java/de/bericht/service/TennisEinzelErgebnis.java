package de.bericht.service;

import org.jsoup.nodes.Element;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.bericht.util.NamensSpeicher;
import de.bericht.util.SpielDetail;

public class TennisEinzelErgebnis implements SpielDetail {

	private String heim;
	private String gast;
	private String heimCellHtml;
	private String gastCellHtml;

	private String satz1;
	private String satz2;
	private String satz3;
	private String matches;
	private String saetze;
	private String games;
	private String vereinnr;
	private NamensSpeicher ns;
	private Boolean verschluesseln;

	public TennisEinzelErgebnis(String vereinnr, String heim, String gast, String satz1, String satz2, String satz3,
			String matches, String saetze, String games, NamensSpeicher ns, Boolean verschluesseln) {

		this.ns = ns;
		this.verschluesseln = verschluesseln;

		this.heim = heim;

		this.gast = gast;

		this.heimCellHtml = "";
		this.gastCellHtml = "";

		this.satz1 = satz1;
		this.satz2 = satz2;
		this.satz3 = satz3;
		this.matches = matches;
		this.saetze = saetze;
		this.games = games;
	}

	public TennisEinzelErgebnis(String vereinnr, Element heimZelle, Element gastZelle, String satz1, String satz2,
			String satz3, String matches, String saetze, String games, NamensSpeicher ns, Boolean verschluesseln) {
		this(vereinnr, heimZelle == null ? "" : heimZelle.text(), gastZelle == null ? "" : gastZelle.text(), satz1,
				satz2, satz3, matches, saetze, games, ns, verschluesseln);
		this.heimCellHtml = heimZelle == null ? "" : heimZelle.html();
		this.gastCellHtml = gastZelle == null ? "" : gastZelle.html();
	}

	@Override
	public String getHeim() {
		return heim;
	}

	@Override
	public String getGast() {
		return gast;
	}

	public String getSatz1() {
		return satz1;
	}

	public String getSatz2() {
		return satz2;
	}

	public String getSatz3() {
		return satz3;
	}

	public String getMatches() {
		return matches;
	}

	public String getSaetze() {
		return saetze;
	}

	public String getGames() {
		return games;
	}

	@Override
	@JsonIgnore
	public String getPosition() {
		return "Einzel";
	}

	@JsonProperty("Heim Spieler")
	public TennisSpielerInfo getHeimSpieler() {
		if (heimCellHtml != null && !heimCellHtml.isBlank()) {
			return TennisSpielerInfo.parseCellHtml(vereinnr, heimCellHtml, ns, verschluesseln);
		}
		return TennisSpielerInfo.parse(vereinnr, heim, ns, verschluesseln);
	}

	@JsonProperty("Gast Spieler")
	public TennisSpielerInfo getGastSpieler() {
		if (gastCellHtml != null && !gastCellHtml.isBlank()) {
			return TennisSpielerInfo.parseCellHtml(vereinnr, gastCellHtml, ns, verschluesseln);
		}
		return TennisSpielerInfo.parse(vereinnr, gast, ns, verschluesseln);
	}

}