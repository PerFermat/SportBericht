package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.service.Bilanz;
import de.bericht.service.BilanzService;
import de.bericht.util.NamensSpeicher;

class BilanzServiceTest {

	BilanzService service = new BilanzService();
	List<Bilanz> bilanz;
	NamensSpeicher namen = new NamensSpeicher();
	String vereinnr = "13014"; // <-- passe an, falls nötig
	String url = "https://www.mytischtennis.de/click-tt/TTBW/25--26/ligen/Erwachsene_Landesklasse_Gr._4/gruppe/494856/mannschaft/2961581/Erwachsene/spielerbilanzen/gesamt";

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		// Hier stehen die von dir gewünschten Zeilen am Anfang
		bilanz = service.getBilanz(vereinnr, url, namen, false);
		System.out.println(service.ausgabe(bilanz));
	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(bilanz, "bilanz darf nicht null sein");
		assertFalse(bilanz.isEmpty(), "bilanz sollte mindestens ein Spiel enthalten");

		Bilanz s = bilanz.get(0);
		assertNotNull(s.getName(), "Mannschaft darf nicht null sein");
		assertNotNull(s.getPosition(), "Position darf nicht null sein");
	}

}
