package de.bericht.provider;

import java.util.List;

import de.bericht.service.Spiel;

public interface SpielplanProvider {

	void generierenSpielplan(String vereinnr, String url, String ligaVorhanden) throws Exception;

	List<Spiel> getSpielplan();

	List<Spiel> getSpielplanFreigabe(String vereinnr);

	void generiereVorschauBericht(String vereinnr, String was);

	String ausgabe(List<Spiel> spiele);

	default boolean isFallbackSourceUsed() {
		return false;
	}

	default String getFallbackSourceUrl() {
		return null;
	}
}