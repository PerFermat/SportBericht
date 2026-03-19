package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;

import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.MatchErgebnis;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.TischtennisMatchSummary;
import de.bericht.util.WebCache;

public abstract class AbstractTischtennisSpielergebnisService extends AbstractSpielergebnisService {

	protected String ort;
	protected String vereinnr;

	protected AbstractTischtennisSpielergebnisService(String vereinnr, String url, NamensSpeicher ns,
			Boolean verschluesseln) {
		this(vereinnr, ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"), url, ns, verschluesseln);
	}

	protected AbstractTischtennisSpielergebnisService(String vereinnr, String berichtMannschaft, String url,
			NamensSpeicher ns, Boolean verschluesseln) {
		this.vereinnr = vereinnr;

		try {
			Document doc = WebCache.getPage(url);
			this.ort = ermittleOrt(berichtMannschaft, doc).toUpperCase();

			TischtennisMatchSummary ttSummary = parseSummary(vereinnr, berichtMannschaft, doc);
			List<MatchErgebnis> matchList = parseMatches(vereinnr, berichtMannschaft, doc, ns, verschluesseln,
					ttSummary);

			ttSummary.setSpiele(matchList);
			setSummary(ttSummary);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected abstract String ermittleOrt(String berichtMannschaft, Document doc);

	protected abstract TischtennisMatchSummary parseSummary(String vereinnr, String berichtMannschaft, Document doc);

	protected abstract List<MatchErgebnis> parseMatches(String vereinnr, String berichtMannschaft, Document doc,
			NamensSpeicher ns, Boolean verschluesseln, TischtennisMatchSummary summary);

	@Override
	public String getSpielErgebnis() {
		if (summary == null || summary.getSpiele() == null) {
			return "";
		}

		List<MatchErgebnis> spiele = cast(summary.getSpiele());

		List<TischtennisDoppelBilanz> doppel = new ArrayList<>();
		List<TischtennisEinzelBilanz> einzel = new ArrayList<>();

		for (MatchErgebnis row : spiele) {
			if (row.getPosition().contains("D")) {
				TischtennisDoppelBilanz neuesHeimDoppel = new TischtennisDoppelBilanz("HEIM", row.getPosition(),
						row.getHeim(), row.getSaetze());
				if (!neuesHeimDoppel.getName().contains("anwesend")) {
					hinzufuegen(doppel, neuesHeimDoppel);
				}

				TischtennisDoppelBilanz neuesGastDoppel = new TischtennisDoppelBilanz("GAST", row.getPosition(),
						row.getGast(), row.getSaetze());
				if (!neuesGastDoppel.getName().contains("anwesend")) {
					hinzufuegen(doppel, neuesGastDoppel);
				}
			} else {
				TischtennisEinzelBilanz neuesHeimEinzel = new TischtennisEinzelBilanz("HEIM", row.getPosition(),
						row.getHeim(), row.getSaetze());
				if (!neuesHeimEinzel.getName().contains("nicht anwesend")) {
					hinzufuegen(einzel, neuesHeimEinzel);
				}

				TischtennisEinzelBilanz neuesGastEinzel = new TischtennisEinzelBilanz("GAST", row.getPosition(),
						row.getGast(), row.getSaetze());
				if (!neuesGastEinzel.getName().contains("nicht anwesend")) {
					hinzufuegen(einzel, neuesGastEinzel);
				}
			}
		}

		doppel.sort(null);
		einzel.sort(null);

		String[] internort = { "HEIM", "GAST" };
		StringBuilder ergebnisString = new StringBuilder();

		for (int intern = 0; intern < 2; intern++) {
			ergebnisString.append("<strong>Für ").append(BerichtHelper.getOrt(vereinnr))
					.append(" spielten:<br></strong>");

			ergebnisString.append("<strong>Doppel: </strong>");
			int j = 0;
			for (TischtennisDoppelBilanz doppelPaarung : doppel) {
				if (doppelPaarung.getMannschaft().equals(ort)
						|| (doppelPaarung.getMannschaft().equals(internort[intern]) && ort.equals("HEIMGAST"))) {
					if (j > 0) {
						ergebnisString.append(", ");
					}
					j++;
					ergebnisString.append(doppelPaarung.getName()).append(" (").append(doppelPaarung.getSiege())
							.append(":").append(doppelPaarung.getNiederlagen()).append(")");
				}
			}

			ergebnisString.append("<br>");
			ergebnisString.append("<strong>Einzel: </strong>");
			j = 0;

			for (TischtennisEinzelBilanz einzelPaarung : einzel) {
				if (einzelPaarung.getMannschaft().equals(ort)
						|| (einzelPaarung.getMannschaft().equals(internort[intern]) && ort.equals("HEIMGAST"))) {
					if (j > 0) {
						ergebnisString.append(", ");
					}
					j++;
					ergebnisString.append(einzelPaarung.getName()).append(" (").append(einzelPaarung.getSiege())
							.append(":").append(einzelPaarung.getNiederlagen()).append(")");
				}
			}

			if (!ort.equals("HEIMGAST")) {
				intern = 2;
			} else {
				ergebnisString.append("<br><br>");
			}
		}

		return ergebnisString.toString();
	}

	@SuppressWarnings("unchecked")
	private List<MatchErgebnis> cast(List<?> spiele) {
		return (List<MatchErgebnis>) spiele;
	}

	protected void hinzufuegen(List<TischtennisDoppelBilanz> doppel, TischtennisDoppelBilanz neuesDoppel) {
		for (TischtennisDoppelBilanz vorhandenesDoppel : doppel) {
			if (vorhandenesDoppel.getName().equals(neuesDoppel.getName())) {
				vorhandenesDoppel.addSiege(neuesDoppel.getSiege());
				vorhandenesDoppel.addNiederlagen(neuesDoppel.getNiederlagen());
				return;
			}
		}
		doppel.add(neuesDoppel);
	}

	protected void hinzufuegen(List<TischtennisEinzelBilanz> einzel, TischtennisEinzelBilanz neuesEinzel) {
		for (TischtennisEinzelBilanz vorhandenesEinzel : einzel) {
			if (vorhandenesEinzel.getName().equals(neuesEinzel.getName())) {
				vorhandenesEinzel.addSiege(neuesEinzel.getSiege());
				vorhandenesEinzel.addNiederlagen(neuesEinzel.getNiederlagen());
				return;
			}
		}
		einzel.add(neuesEinzel);
	}
}