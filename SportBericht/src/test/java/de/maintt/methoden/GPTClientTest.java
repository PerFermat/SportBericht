package de.maintt.methoden;

import java.io.IOException;

import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.util.ApiKIChatGPT;

public class GPTClientTest {
	String vereinnr = "13014"; // dein fester String
	String frage = "Was ist die Hauptstadt Deutschland.";
	String selectedModel = "gpt-5.1";
	double temeratur = 0.5;
	double frequencyPenalty = 0.5;
	double presencePenalty = 0.5;

	@Disabled("temporär deaktiviert")
	@Test
	void testAskGpt() throws Exception {
		JSONObject jsonSchema = new JSONObject().put("$schema", "https://json-schema.org/draft/2020-12/schema")
				.put("title", "AntwortSchema").put("type", "object")
				.put("properties",
						new JSONObject()
								.put("Frage",
										new JSONObject().put("type", "string").put("description",
												"Die gestellte Frage"))
								.put("Kurzantwort",
										new JSONObject().put("type", "string").put("description",
												"Eine sehr kurze, prägnante Antwort"))
								.put("ausführlicheAntwort",
										new JSONObject().put("type", "string").put("description",
												"Eine ausführliche, detaillierte Antwort")))
				.put("required", new org.json.JSONArray().put("Frage").put("Kurzantwort").put("ausführlicheAntwort"));
		ApiKIChatGPT api;
		try {
			api = new ApiKIChatGPT(vereinnr, frage, selectedModel, "none", temeratur, frequencyPenalty, presencePenalty,
					jsonSchema);
			String ergebnisEntscheidung = api.getResponse();
			System.out.println(ergebnisEntscheidung);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
