package de.maintt.methoden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.util.ConfigManager;
import de.bericht.util.NamensSpeicher;

class TestFormatName {
	ConfigManager config;
	String vereinnr = "13014"; // dein fester String
	String[] namensliste = { "Michael Spahr/Verona Maier Steb", "Spahr, Michael/Maier Steb, Verona", "Otto Krause",
			"Krause, Otto" };

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		config = ConfigManager.getInstance();
		NamensSpeicher ns = new NamensSpeicher();
		for (String name : namensliste) {
			System.out.println(ns.formatName(vereinnr, name, ns, false));
		}

	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
	}
}
