package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.bericht.provider.SpielergebnisFactory;
import de.bericht.provider.SpielergebnisProvider;
import de.bericht.util.ConfigManager;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.enums.HeimGastArt;

class SpielergebnisServiceTest {

	ConfigManager config;
	String ergebnis;
	String vereinnr = "13014"; // <-- passe an, falls nötig

	String url = "https://www.mytischtennis.de/click-tt/TTBW/25--26/ligen/E_KL_A/gruppe/494540/spielbericht/15741747/TSG_Upfingen_II-vs-TSG_Upfingen";
	SpielergebnisProvider provider;
	NamensSpeicher ns = new NamensSpeicher();

	// @Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		config = ConfigManager.getInstance();
		try {
			provider = SpielergebnisFactory.create(vereinnr, HeimGastArt.MANNSCHAFT, "TSG Upfingen II", url, ns, false);
			// Hier stehen die von dir gewünschten Zeilen am Anfang
			ergebnis = provider.getSpielErgebnis();
			System.out.println(ergebnis);
			System.out.println(provider.summaryToJson());
			System.out.println(ns.ausgabe());
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
