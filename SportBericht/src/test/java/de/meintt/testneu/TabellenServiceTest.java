package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.provider.TabellenFactory;
import de.bericht.provider.TabellenProvider;
import de.bericht.service.Tabelle;

class TabellenServiceTest {
	TabellenProvider service;
	String url = "https://www.mytischtennis.de/click-tt/TTBW/25--26/ligen/Erwachsene_Bezirksliga/gruppe/494841/mannschaft/2959879/Erwachsene/spielplan/gesamt";
	List<Tabelle> tabelle = new ArrayList<>();

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
		try {
			service = TabellenFactory.create(url);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Hier stehen die von dir gewünschten Zeilen am Anfang
		tabelle = service.getTabelle();
		System.out.println(service.ausgabe(tabelle));
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			System.out.println(objectMapper.writeValueAsString(tabelle).toString());
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
		assertNotNull(tabelle, "tabelle darf nicht null sein");
		assertFalse(tabelle.isEmpty(), "tabelle sollte mindestens ein Spiel enthalten");
	}

	@Disabled("temporär deaktiviert")
	@Test
	void erstes_spiel_hat_gueltige_felder() {
		Tabelle s = tabelle.get(0);
		assertNotNull(s.getMannschaft(), "Mannschaft darf nicht null sein");
		assertNotNull(s.getRang(), "Rang darf nicht null sein");
	}

}
