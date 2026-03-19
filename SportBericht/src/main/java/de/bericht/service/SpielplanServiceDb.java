package de.bericht.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.bericht.provider.SpielplanProvider;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;

public class SpielplanServiceDb implements SpielplanProvider {

	private final List<Spiel> spiele;
	private final String fallbackSourceUrl;

	public SpielplanServiceDb(String vereinnr, String fallbackSourceUrl) {
		this.fallbackSourceUrl = fallbackSourceUrl;
		DatabaseService dbService = new DatabaseService();

		List<TischtennisSpiel> geladeneSpiele = dbService.ladeSpielplanAusTabelle(vereinnr);
		this.spiele = new ArrayList<>(geladeneSpiele);
	}

	@Override
	public void generierenSpielplan(String vereinnr, String url, String ligaVorhanden) {
		// Daten kommen bereits aus der Datenbank und werden im Konstruktor geladen.
	}

	@Override
	public List<Spiel> getSpielplan() {
		return spiele;
	}

	@Override
	public List<Spiel> getSpielplanFreigabe(String vereinnr) {
		List<Spiel> freigegeben = new ArrayList<>();
		for (Spiel spiel : spiele) {
			try {
				if (BerichtHelper.hasFreigabe(vereinnr, spiel.getErgebnisLink())) {
					freigegeben.add(spiel);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		java.util.Collections.sort(freigegeben);
		return freigegeben;
	}

	@Override
	public void generiereVorschauBericht(String vereinnr, String was) {
		String textBody = ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.text");
		StringBuffer sb = new StringBuffer();
		LocalDate heute = LocalDate.now();

		long tageLetzterSatz = 9999;
		int i = 0;
		String aktDatum = "";
		sb.append("<p>");

		for (Spiel spiel : spiele) {
			boolean spielDruck = false;

			if (!spiel.getErgebnis().isEmpty()) {
				spielDruck = false;
			} else if ("HEIM".equals(was)
					&& spiel.getHeim().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
				spielDruck = true;
			} else if ("GAST".equals(was)
					&& spiel.getGast().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
				spielDruck = true;
			} else if ("ALLE".equals(was)) {
				if (spiel.getGast().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
					spielDruck = true;
				} else if (spiel.getHeim().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
					spielDruck = true;
				}
			}

			if (spielDruck) {
				String datum = spiel.getDatumAnzeige();
				long tage = SpielplanService.tageBisDatum(datum, heute);
				long configTage = Long.parseLong(ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.tage"));

				if (tage <= configTage || tage <= tageLetzterSatz + 1 || i < 2 || aktDatum.equals(datum)) {
					if (!aktDatum.equals(datum)) {
						sb.append("<br><strong>").append(SpielplanService.wochentag(datum)).append(", ").append(datum)
								.append(" </strong> <br> ");
					}
					i++;
					tageLetzterSatz = tage;

					sb.append("<strong>   - ").append(spiel.getZeitAnzeige()).append(": ").append(spiel.getLiga())
							.append(": </strong> ").append(spiel.getHeim()).append(" - ").append(spiel.getGast())
							.append("<br>");

					aktDatum = datum;
				}
			}
		}
		sb.append("</p>");

		DatabaseService dbService = new DatabaseService(vereinnr);
		dbService.saveBerichtDataOhneHist(vereinnr, "31.12.2999 - Vorschaubericht",
				textBody.replace("[spielliste]", sb.toString()), null, null,
				ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.ueberschrift"));
	}

	@Override
	public String ausgabe(List<Spiel> spiele) {
		StringBuilder spieleListe = new StringBuilder();
		for (Spiel spiel : spiele) {
			spieleListe.append(spiel.getDatumAnzeige()).append(" - ");
			spieleListe.append(spiel.getLiga()).append(" - ");
			spieleListe.append(spiel.getHeim()).append(" - ");
			spieleListe.append(spiel.getGast()).append(" - ");
			spieleListe.append(spiel.getErgebnis()).append(" - ");
			spieleListe.append(spiel.getErgebnisLink()).append("\n");
		}
		return spieleListe.toString();
	}

	@Override
	public boolean isFallbackSourceUsed() {
		return true;
	}

	@Override
	public String getFallbackSourceUrl() {
		return fallbackSourceUrl;
	}
}