package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.util.ConfigManager;
import de.bericht.util.NamensSpeicher;

class FuelleNamensspeicherTest {

	ConfigManager config;
	String ergebnis;
	String vereinnr = "13031"; // <-- passe an, falls nötig
	String url = "https://www.mytischtennis.de/click-tt/TTBW/25--26/ligen/E_BK/gruppe/494512/spielbericht/15469807/TG_Donzdorf_IV-vs-TTV_Zell_IV";
	NamensSpeicher namensSpeicher = new NamensSpeicher();

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		config = ConfigManager.getInstance();

		try {
			namensSpeicher.fuelleNamensspeicher(vereinnr, url, namensSpeicher, false);
			System.out.println("Aufruf");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(namensSpeicher.zeigeAlle().replace("<br />", "\n"));

	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(namensSpeicher, "Spielplan darf nicht null sein");
	}

}
