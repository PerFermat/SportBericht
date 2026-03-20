package de.bericht.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.bericht.service.TennisDoppelErgebnis;
import de.bericht.service.TennisEinzelErgebnis;
import de.bericht.service.TennisSpielerInfo;

@JsonIgnoreProperties({ "Sportart", "Saison", "Liga", "Ergebnis", "spiel_Beginn", "spiel_Ende", "Spiele" })
@JsonPropertyOrder({ "Bezirk", "Klasse", "Heimmannschaft", "Gastmannschaft", "Gesamt-Sätze", "Gesamt-Games",
		"Gesamt-Ergebnis", "berichtMannschaft", "berichtMannschaft_ist_Heim", "punkte_Heimmannschaft",
		"punkte_Gastmannschaft", "punkte_berichtMannschaft", "punkte_Gegner", "spielentscheidung", "Einzel", "Doppel" })
public class TennisMatchSummary extends MatchSummary {

	@JsonProperty("Klasse")
	private String klasse;

	@JsonProperty("Gesamt-Sätze")
	private String gesamtSaetze;

	@JsonProperty("Gesamt-Games")
	private String gesamtGames;

	@JsonProperty("Gesamt-Ergebnis")
	private String gesamtErgebnis;

	public TennisMatchSummary(String berichtMannschaft, String heimmannschaft, String gastmannschaft, String bezirk,
			String saison, String liga, String klasse, String ergebnis, String spielBeginn, String spielEnde,
			String gesamtSaetze, String gesamtGames) {
		super("Tennis", berichtMannschaft, heimmannschaft, gastmannschaft, bezirk, saison, liga, ergebnis, spielBeginn,
				spielEnde);
		this.klasse = klasse;
		this.gesamtErgebnis = ergebnis;
		this.gesamtSaetze = gesamtSaetze;
		this.gesamtGames = gesamtGames;
	}

	public String getKlasse() {
		return klasse;
	}

	public String getGesamtSaetze() {
		return gesamtSaetze;
	}

	public String getGesamtGames() {
		return gesamtGames;
	}

	public String getGesamtErgebnis() {
		return gesamtErgebnis;
	}

	@JsonProperty("spielentscheidung")
	public String getSpielEntscheidungTennis() {
		String entscheidung = super.getSpielEntscheidung();
		if ("Sieg_Gegner".equals(entscheidung)) {
			return "Niederlage_BerichtMannschaft";
		}
		return entscheidung;
	}

	@JsonProperty("Einzel")
	public List<TennisEinzelJson> getEinzel() {
		List<TennisEinzelJson> list = new ArrayList<>();
		if (getSpiele() == null) {
			return list;
		}

		boolean berichtIstHeim = isBerichtMannschaftIstHeim();
		for (SpielDetail detail : getSpiele()) {
			if (detail instanceof TennisEinzelErgebnis e) {
				list.add(TennisEinzelJson.from(e, berichtIstHeim));
			}
		}
		return list;
	}

	@JsonProperty("Doppel")
	public List<TennisDoppelJson> getDoppel() {
		List<TennisDoppelJson> list = new ArrayList<>();
		if (getSpiele() == null) {
			return list;
		}

		boolean berichtIstHeim = isBerichtMannschaftIstHeim();
		for (SpielDetail detail : getSpiele()) {
			if (detail instanceof TennisDoppelErgebnis d) {
				list.add(TennisDoppelJson.from(d, berichtIstHeim));
			}
		}
		return list;
	}

	public void setGesamtSaetze(String gesamtSaetze) {
		this.gesamtSaetze = gesamtSaetze;
	}

	public void setGesamtGames(String gesamtGames) {
		this.gesamtGames = gesamtGames;
	}

	public void setKlasse(String klasse) {
		this.klasse = klasse;
	}

	private static String gewinnerAusMatches(String matches) {
		if (matches == null || !matches.contains(":")) {
			return "Unbekannt";
		}
		String[] teile = matches.split(":");
		if (teile.length != 2) {
			return "Unbekannt";
		}
		try {
			int heim = Integer.parseInt(teile[0].trim());
			int gast = Integer.parseInt(teile[1].trim());
			if (heim > gast) {
				return "Heim";
			}
			if (gast > heim) {
				return "Gast";
			}
			return "Unentschieden";
		} catch (NumberFormatException e) {
			return "Unbekannt";
		}
	}

	public static class TennisEinzelJson {
		@JsonProperty("BerichtSpieler")
		public TennisSpielerNameJson berichtSpieler;
		@JsonProperty("Heim Spieler")
		public TennisSpielerNameJson heimSpieler;
		@JsonProperty("Gast Spieler")
		public TennisSpielerNameJson gastSpieler;
		@JsonProperty("Satz 1")
		public String satz1;
		@JsonProperty("Satz 2")
		public String satz2;
		@JsonProperty("Satz 3")
		public String satz3;
		@JsonProperty("Matches")
		public String matches;
		@JsonProperty("Sätze")
		public String saetze;
		@JsonProperty("Games")
		public String games;
		@JsonProperty("Gewinner")
		public String gewinner;
		@JsonProperty("BerichtSpieler_hat_gewonnen")
		public boolean berichtSpielerHatGewonnen;

		static TennisEinzelJson from(TennisEinzelErgebnis e, boolean berichtIstHeim) {
			TennisEinzelJson json = new TennisEinzelJson();
			json.heimSpieler = TennisSpielerNameJson.from(e.getHeimSpieler());
			json.gastSpieler = TennisSpielerNameJson.from(e.getGastSpieler());

			json.berichtSpieler = berichtIstHeim ? json.heimSpieler : json.gastSpieler;
			json.satz1 = e.getSatz1();
			json.satz2 = e.getSatz2();
			json.satz3 = e.getSatz3();
			json.matches = e.getMatches();
			json.saetze = e.getSaetze();
			json.games = e.getGames();
			json.gewinner = gewinnerAusMatches(e.getMatches());
			json.berichtSpielerHatGewonnen = ("Heim".equals(json.gewinner) && berichtIstHeim)
					|| ("Gast".equals(json.gewinner) && !berichtIstHeim);
			return json;
		}
	}

	public static class TennisDoppelJson {
		@JsonProperty("BerichtSpieler")
		public String berichtSpieler;
		@JsonProperty("Heim - Doppelpartner 1")
		public TennisSpielerOhnePositionJson heim1;
		@JsonProperty("Heim - Doppelpartner 2")
		public TennisSpielerOhnePositionJson heim2;
		@JsonProperty("Gast - Doppelpartner 1")
		public TennisSpielerOhnePositionJson gast1;
		@JsonProperty("Gast - Doppelpartner 2")
		public TennisSpielerOhnePositionJson gast2;
		@JsonProperty("Satz 1")
		public String satz1;
		@JsonProperty("Satz 2")
		public String satz2;
		@JsonProperty("Satz 3")
		public String satz3;
		@JsonProperty("Matches")
		public String matches;
		@JsonProperty("Sätze")
		public String saetze;
		@JsonProperty("Games")
		public String games;
		@JsonProperty("Gewinner")
		public String gewinner;
		@JsonProperty("BerichtSpieler_hat_gewonnen")
		public boolean berichtSpielerHatGewonnen;

		static TennisDoppelJson from(TennisDoppelErgebnis d, boolean berichtIstHeim) {
			TennisDoppelJson json = new TennisDoppelJson();
			TennisSpielerInfo heim1 = d.getHeimSpieler1();
			TennisSpielerInfo heim2 = d.getHeimSpieler2();
			TennisSpielerInfo gast1 = d.getGastSpieler1();
			TennisSpielerInfo gast2 = d.getGastSpieler2();

			json.heim1 = TennisSpielerOhnePositionJson.from(heim1);
			json.heim2 = TennisSpielerOhnePositionJson.from(heim2);
			json.gast1 = TennisSpielerOhnePositionJson.from(gast1);
			json.gast2 = TennisSpielerOhnePositionJson.from(gast2);
			json.berichtSpieler = berichtIstHeim ? nachname(heim1) + "/" + nachname(heim2)
					: nachname(gast1) + "/" + nachname(gast2);

			json.satz1 = d.getSatz1();
			json.satz2 = d.getSatz2();
			json.satz3 = d.getSatz3();
			json.matches = d.getMatches();
			json.saetze = d.getSaetze();
			json.games = d.getGames();
			json.gewinner = gewinnerAusMatches(d.getMatches());
			json.berichtSpielerHatGewonnen = ("Heim".equals(json.gewinner) && berichtIstHeim)
					|| ("Gast".equals(json.gewinner) && !berichtIstHeim);
			return json;
		}
	}

	public static class TennisSpielerNameJson {
		@JsonProperty("name")
		public String name;

		static TennisSpielerNameJson from(TennisSpielerInfo info) {
			TennisSpielerNameJson json = new TennisSpielerNameJson();
			json.name = info.getName();
			return json;
		}
	}

	public static class TennisSpielerOhnePositionJson {
		@JsonProperty("name")
		public String name;
		@JsonProperty("meldeliste")
		public String meldeliste;
		@JsonProperty("leistungsklasse")
		public String leistungsklasse;

		static TennisSpielerOhnePositionJson from(TennisSpielerInfo info) {
			TennisSpielerOhnePositionJson json = new TennisSpielerOhnePositionJson();
			json.name = info.getName();
			json.meldeliste = info.getMeldeliste();
			json.leistungsklasse = info.getLeistungsklasse();
			return json;
		}
	}

	private static String nachname(TennisSpielerInfo info) {
		if (info == null || info.getName() == null || info.getName().isBlank()) {
			return "";
		}
		String[] teile = info.getName().trim().split("\\s+");
		return teile[teile.length - 1];
	}

}
