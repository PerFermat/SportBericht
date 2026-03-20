package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.bericht.provider.SpielergebnisFactory;
import de.bericht.provider.SpielergebnisProvider;
import de.bericht.util.ConfigManager;
import de.bericht.util.NamensSpeicher;

class SpielergebnisServiceTest {

	ConfigManager config;
	String ergebnis;
	String vereinnr = "20233"; // <-- passe an, falls nötig

	String url = "https://www.wtb-tennis.de/spielbetrieb/winter2025-2026/jugend/gruppe/spielbericht.html?tx_nuportalrs_meeting%5BmeetingId%5D=12520281&cHash=e381cddcfb22aaf56d26b4325a5cf059";
	SpielergebnisProvider provider;
	NamensSpeicher ns = new NamensSpeicher();

	// @Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		config = ConfigManager.getInstance();
		try {
			provider = SpielergebnisFactory.create(vereinnr, url, ns, true);
			// Hier stehen die von dir gewünschten Zeilen am Anfang
			ergebnis = provider.getSpielErgebnis();
			System.out.println(ergebnis);
			// System.out.println(provider.summaryToJson());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// @Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(ergebnis, "Spielplan darf nicht null sein");
		assertFalse(ergebnis.isEmpty(), "Spielplan sollte mindestens ein Spiel enthalten");
	}

}
