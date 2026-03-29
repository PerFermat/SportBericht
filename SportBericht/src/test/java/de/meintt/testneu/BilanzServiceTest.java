package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.provider.BilanzFactory;
import de.bericht.provider.BilanzProvider;
import de.bericht.service.Bilanz;
import de.bericht.util.NamensSpeicher;

class BilanzServiceTest {

	List<Bilanz> bil;
	NamensSpeicher namen = new NamensSpeicher();
	String vereinnr = "13014"; // <-- passe an, falls nötig
	String url = "https://www.wtb-tennis.de/spielbetrieb/vereine/verein/mannschaften/mannschaft/v/20233/m/3496326.html";
	NamensSpeicher namensSpeicher = new NamensSpeicher();

	// @Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		// Hier stehen die von dir gewünschten Zeilen am Anfang
		BilanzProvider service;
		try {
			service = BilanzFactory.create(vereinnr, url, namensSpeicher, false);
			bil = service.getBilanz();
			System.out.println(service.ausgabe(bil));

			ObjectMapper objectMapper = new ObjectMapper();
			try {
				System.out.println(objectMapper.writeValueAsString(bil).toString());
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(namensSpeicher.ausgabe());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(bil, "bilanz darf nicht null sein");
		assertFalse(bil.isEmpty(), "bilanz sollte mindestens ein Spiel enthalten");

		Bilanz s = bil.get(0);
		assertNotNull(s.getName(), "Mannschaft darf nicht null sein");
	}

}
