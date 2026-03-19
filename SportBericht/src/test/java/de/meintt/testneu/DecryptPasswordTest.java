package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.util.ConfigManager;

class DecryptPasswordTest {

	String vereinnr = "13014"; // <-- passe an, falls nötig
	String passwort;
	String hash = "WsBZ5l9Q10bNlWnenMXbUgoQMJayEamdRuhz2oqDBMdGbDD3JvUXrOxZ/CibAMvAGSWutY/Ww0Md99tFOtq/uQ==";

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		try {
			ConfigManager c = new ConfigManager();
//			passwort = c.getMailPassword(vereinnr);
//			System.out.println(passwort);
//			
			String decrypted = c.decryptPassword(vereinnr, hash);
			System.out.println(decrypted);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(passwort, "passwort darf nicht null sein");
		assertFalse(passwort.isEmpty(), "passwort sollte mindestens ein Spiel enthalten");
	}

}
