package de.bericht.util;

public class TischtennisMatchSummary extends MatchSummary {

	public TischtennisMatchSummary(String berichtMannschaft, String heimmannschaft, String gastmannschaft,
			String bezirk, String saison, String liga, String ergebnis, String spielBeginn, String spielEnde) {
		super("Tischtennis", berichtMannschaft, heimmannschaft, gastmannschaft, bezirk, saison, liga, ergebnis,
				spielBeginn, spielEnde);
	}
}