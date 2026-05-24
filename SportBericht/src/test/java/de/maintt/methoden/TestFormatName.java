package de.maintt.methoden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.bericht.util.ConfigManager;
import de.bericht.util.NamePart;
import de.bericht.util.NamensSpeicher;

class TestFormatName {
	ConfigManager config;
	String vereinnr = "13014"; // dein fester String
	String[] namensliste = { "Michael Spahr/Verona Jutta Steb", "Spahr, Michael/Maier Steb, Verona", "Otto Krause",
			"Krause, Otto" };

	// @Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		config = ConfigManager.getInstance();
		NamensSpeicher ns = new NamensSpeicher();
		for (String name : namensliste) {
			System.out.println("Format" + ns.formatName(vereinnr, name, ns, false));
			if (name.contains("/")) {
				String[] parts = name.split("/");
				NamePart np1 = NamensSpeicher.splitName(parts.length > 0 ? parts[0] : "");
				NamePart np2 = NamensSpeicher.splitName(parts.length > 1 ? parts[1] : "");
				System.out.println("Vorname : " + np1.getVorname());
				System.out.println("Nachname: " + np1.getNachname());
				System.out.println("Vorname : " + np2.getVorname());
				System.out.println("Nachname: " + np2.getNachname());
			} else {
				NamePart np = NamensSpeicher.splitName(name);
				System.out.println("Vorname : " + np.getVorname());
				System.out.println("Nachname: " + np.getNachname());
			}

		}

	}

	// @Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
	}
}
