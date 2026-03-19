package de.bericht.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "Sportart", "Bezirk", "Saison", "Liga", "Heimmannschaft", "Gastmannschaft", "Ergebnis",
		"berichtMannschaft", "berichtMannschaft_ist_Heim", "punkte_Heimmannschaft", "punkte_Gastmannschaft",
		"punkte_berichtMannschaft", "punkte_Gegner", "spielentscheidung", "spiel_Beginn", "spiel_Ende", "Spiele" })
public abstract class MatchSummary {

	@JsonProperty("Sportart")
	private String sportart;

	@JsonProperty("Bezirk")
	private String bezirk;

	@JsonProperty("Saison")
	private String saison;

	@JsonProperty("Liga")
	private String liga;

	@JsonProperty("Heimmannschaft")
	private String heimmannschaft;

	@JsonProperty("Gastmannschaft")
	private String gastmannschaft;

	@JsonProperty("Ergebnis")
	private String ergebnis;

	@JsonProperty("berichtMannschaft")
	private String berichtMannschaft;

	@JsonProperty("berichtMannschaft_ist_Heim")
	private boolean berichtMannschaftIstHeim;

	@JsonProperty("punkte_Heimmannschaft")
	private int punkteHeimmannschaft;

	@JsonProperty("punkte_Gastmannschaft")
	private int punkteGastmannschaft;

	@JsonProperty("punkte_berichtMannschaft")
	private int punkteBerichtMannschaft;

	@JsonProperty("punkte_Gegner")
	private int punkteGegner;

	@JsonProperty("spielentscheidung")
	private String spielEntscheidung;

	@JsonProperty("spiel_Beginn")
	private String spielBeginn;

	@JsonProperty("spiel_Ende")
	private String spielEnde;

	@JsonProperty("Spiele")
	private List<? extends SpielDetail> spiele;

	protected MatchSummary(String sportart, String berichtMannschaft, String heimmannschaft, String gastmannschaft,
			String bezirk, String saison, String liga, String ergebnis, String spielBeginn, String spielEnde) {
		this.sportart = sportart;
		this.berichtMannschaft = berichtMannschaft;
		this.heimmannschaft = heimmannschaft;
		this.gastmannschaft = gastmannschaft;
		this.bezirk = bezirk;
		this.saison = saison;
		this.liga = liga;
		this.ergebnis = ergebnis;
		this.spielBeginn = spielBeginn;
		this.spielEnde = spielEnde;
		analyseErgebnis();
	}

	protected void analyseErgebnis() {
		if (ergebnis == null || ergebnis.isBlank()) {
			setUnknownResult();
			return;
		}

		try {
			Pattern pattern = Pattern.compile("(\\d+)\\s*:\\s*(\\d+)");
			Matcher matcher = pattern.matcher(ergebnis);

			if (!matcher.find()) {
				setUnknownResult();
				return;
			}

			this.punkteHeimmannschaft = Integer.parseInt(matcher.group(1));
			this.punkteGastmannschaft = Integer.parseInt(matcher.group(2));

			this.berichtMannschaftIstHeim = containsIgnoreCase(heimmannschaft, berichtMannschaft);

			if (berichtMannschaftIstHeim) {
				punkteBerichtMannschaft = punkteHeimmannschaft;
				punkteGegner = punkteGastmannschaft;
			} else {
				punkteBerichtMannschaft = punkteGastmannschaft;
				punkteGegner = punkteHeimmannschaft;
			}

			if (punkteBerichtMannschaft > punkteGegner) {
				spielEntscheidung = "Sieg_BerichtMannschaft";
			} else if (punkteBerichtMannschaft < punkteGegner) {
				spielEntscheidung = "Sieg_Gegner";
			} else {
				spielEntscheidung = "Unentschieden";
			}
		} catch (Exception e) {
			setUnknownResult();
		}
	}

	private void setUnknownResult() {
		this.punkteHeimmannschaft = -1;
		this.punkteGastmannschaft = -1;
		this.punkteBerichtMannschaft = -1;
		this.punkteGegner = -1;
		this.spielEntscheidung = "Unbekannt";
	}

	private boolean containsIgnoreCase(String text, String part) {
		if (text == null || part == null) {
			return false;
		}
		return text.toLowerCase().contains(part.toLowerCase());
	}

	public String getSportart() {
		return sportart;
	}

	public String getBezirk() {
		return bezirk;
	}

	public String getSaison() {
		return saison;
	}

	public String getLiga() {
		return liga;
	}

	public String getHeimmannschaft() {
		return heimmannschaft;
	}

	public String getGastmannschaft() {
		return gastmannschaft;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public String getBerichtMannschaft() {
		return berichtMannschaft;
	}

	public boolean isBerichtMannschaftIstHeim() {
		return berichtMannschaftIstHeim;
	}

	public int getPunkteHeimmannschaft() {
		return punkteHeimmannschaft;
	}

	public int getPunkteGastmannschaft() {
		return punkteGastmannschaft;
	}

	public int getPunkteBerichtMannschaft() {
		return punkteBerichtMannschaft;
	}

	public int getPunkteGegner() {
		return punkteGegner;
	}

	public String getSpielEntscheidung() {
		return spielEntscheidung;
	}

	public String getSpielBeginn() {
		return spielBeginn;
	}

	public String getSpielEnde() {
		return spielEnde;
	}

	public List<? extends SpielDetail> getSpiele() {
		return spiele;
	}

	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
		analyseErgebnis();
	}

	public void setSpiele(List<? extends SpielDetail> spiele) {
		this.spiele = spiele;
	}

	public void setSpielBeginn(String spielBeginn) {
		this.spielBeginn = spielBeginn;
	}

	public void setSpielEnde(String spielEnde) {
		this.spielEnde = spielEnde;
	}
}