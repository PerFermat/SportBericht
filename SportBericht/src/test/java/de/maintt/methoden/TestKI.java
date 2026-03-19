package de.maintt.methoden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TestKI {

	String vereinnr = "13014"; // dein fester String
	String frage = "Wie hoch ist der Eifelturm";
	String selectedModel = "gpt-4.1-mini";
	double temeratur = 0.5;
	double frequencyPenalty = 0.5;
	double presencePenalty = 0.5;

	@Disabled("temporär deaktiviert")
	@BeforeEach
	void setUp() {
//		ApiKIChatGPT api;
//		try {
//			api = new ApiKIChatGPT(vereinnr, frage, selectedModel, temeratur, frequencyPenalty, presencePenalty);
//			String ergebnisEntscheidung = api.getResponse();
//			System.out.println(ergebnisEntscheidung);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	@Disabled("temporär deaktiviert")
	@Test
	void spielplan_darf_nicht_null_und_soll_nicht_leer_sein() {
	}
}
