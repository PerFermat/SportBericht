package de.bericht.provider;

import java.io.IOException;

import org.json.JSONObject;

import de.bericht.util.ApiKIChatGPT;
import de.bericht.util.ApiKIClaude;
import de.bericht.util.ApiKIDeepSeek;
import de.bericht.util.ApiKIGemini;

public class KiProviderFactory {

	private KiProviderFactory() {
	}

	public static KiProvider create(String vereinnr, String frage, String model, String thinking, double temperatur,
			double frequencyPenalty, double presencePenalty, JSONObject jsonSchema) throws IOException {

		String normalizedModel = model == null ? "" : model.trim().toLowerCase();

		if (normalizedModel.startsWith("deepseek")) {
			return new ApiKIDeepSeek(vereinnr, frage, model, temperatur, frequencyPenalty, presencePenalty, jsonSchema);
		}

		if (normalizedModel.startsWith("claude")) {
			return new ApiKIClaude(vereinnr, frage, model, thinking, temperatur, frequencyPenalty, presencePenalty,
					jsonSchema);
		}

		if (normalizedModel.startsWith("gemini")) {
			return new ApiKIGemini(vereinnr, frage, model, thinking, temperatur, frequencyPenalty, presencePenalty,
					jsonSchema);
		}

		// Default: OpenAI / ChatGPT
		return new ApiKIChatGPT(vereinnr, frage, model, thinking, temperatur, frequencyPenalty, presencePenalty,
				jsonSchema);
	}
}