package de.maintt.methoden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.util.BerichtHelper;

class TestSpielEntscheidung {

	String ergebnis = "1:2test";
	Boolean istHeim = true;

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		// Hier stehen die von dir gewünschten Zeilen am Anfang
		String ergebnisEntscheidung = BerichtHelper.spielEntscheidung(ergebnis, istHeim);
		System.out.println(ergebnisEntscheidung);
	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		String ergebnisEntscheidung = BerichtHelper.spielEntscheidung(ergebnis, istHeim);
		System.out.println(ergebnisEntscheidung);
	}
}
