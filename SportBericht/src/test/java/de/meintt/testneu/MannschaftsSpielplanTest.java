package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.service.Mannschaft;
import de.bericht.service.MannschaftService;

class MannschaftsSpielplanTest {
	MannschaftService service = null;
	List<Mannschaft> mannschaft = null;
	String vereinnr = "13014"; // dein fester String

	@Disabled("Temporär deaktiviert wegen Refactoring")
	@BeforeEach
	void setUp() {
		service = new MannschaftService(vereinnr);

		mannschaft = service.getMannschaften();
		System.out.println(service.ausgabe(mannschaft));

	}

	@Disabled("Temporär deaktiviert wegen Refactoring")
	@Test
	void spielplan_darf_nicht_null_sein() {
		assertNotNull(mannschaft, "spiele darf nicht null sein");
	}

	@Disabled("Temporär deaktiviert wegen Refactoring")
	@Test
	void spielplan_sollte_spiele_enthalten() {
		assertFalse(mannschaft.isEmpty(), "spiele sollte mindestens ein Spiel enthalten");
	}
}
