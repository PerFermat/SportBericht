package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.provider.SpielplanFactory;
import de.bericht.provider.SpielplanProvider;
import de.bericht.service.Spiel;
import de.bericht.util.ConfigManager;

class SpielplanServiceTest {

	ConfigManager config;
	SpielplanProvider provider;
	List<Spiel> spiele;
	String vereinnr = "20233"; // <-- passe an, falls nötig

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {

		ConfigManager config = ConfigManager.getInstance();
		// Hier stehen die von dir gewünschten Zeilen am Anfang
		try {
			provider = SpielplanFactory.createForUrl(vereinnr,
					"https://www.wtb-tennis.de/spielbetrieb/vereine/verein/mannschaften/mannschaft/v/20233/m/3496261.html");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		spiele = provider.getSpielplan();
		System.out.println(provider.ausgabe(spiele));
	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(spiele, "Spielplan darf nicht null sein");
		assertFalse(spiele.isEmpty(), "Spielplan sollte mindestens ein Spiel enthalten");
	}

	@Disabled("temporär deaktiviert")
	@Test
	void erstes_spiel_hat_gueltige_felder() {
		Spiel s = spiele.get(0);
		assertNotNull(s.getDatumAnzeige(), "Datum darf nicht null sein");
		assertNotNull(s.getHeim(), "Heimmannschaft darf nicht null sein");
	}
}
