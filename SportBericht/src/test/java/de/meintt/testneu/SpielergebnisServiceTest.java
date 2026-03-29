package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.provider.SpielergebnisFactory;
import de.bericht.provider.SpielergebnisProvider;
import de.bericht.util.ConfigManager;
import de.bericht.util.NamensSpeicher;

class SpielergebnisServiceTest {

	ConfigManager config;
	String ergebnis;
	String vereinnr = "20233"; // <-- passe an, falls nötig

	String url = "https://ttvwh.click-tt.de/cgi-bin/WebObjects/nuLigaTTDE.woa/wa/clubMeetingReport?meeting=15834025&championship=Pokal+Bez.+SN+25%2F26&club=3947&group=513877";
	SpielergebnisProvider provider;
	NamensSpeicher ns = new NamensSpeicher();

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		config = ConfigManager.getInstance();
		try {
			provider = SpielergebnisFactory.create(vereinnr, url, ns, true);
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

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(ergebnis, "Spielplan darf nicht null sein");
		assertFalse(ergebnis.isEmpty(), "Spielplan sollte mindestens ein Spiel enthalten");
	}

}
