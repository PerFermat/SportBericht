package de.bericht.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.service.DatabaseService;

public class StilGenerator {

	public String stilvariationen(String vereinnr, List<String> wirkungen) throws Exception {
		// Variante 1 – Standard
		List<Stilvariante> varianten = new ArrayList<>();

		DatabaseService dbService = new DatabaseService(vereinnr);
		int i = 0;
		for (String wirkung : wirkungen) {
			List<Stil> stile = new ArrayList<>();
			stile.add(dbService.leseWirkung(vereinnr, wirkung));
			varianten.add(new Stilvariante(wirkung, ++i, stile));
		}

		// JSON-Ausgabe
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(varianten);
	}

}
