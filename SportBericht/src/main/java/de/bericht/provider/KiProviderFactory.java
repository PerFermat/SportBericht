package de.bericht.provider;

import java.io.IOException;

import org.json.JSONObject;

import de.bericht.util.ApiKIChatGPT;
import de.bericht.util.ApiKIClaude;
import de.bericht.util.ApiKIDeepSeek;
import de.bericht.util.ApiKIGemini;
import de.bericht.util.enums.KiSystem;

public class KiProviderFactory {

	private KiProviderFactory() {
	}

	public static KiProvider create(String vereinnr, String frage, String model, String thinking, double temperatur,
			double frequencyPenalty, double presencePenalty, JSONObject jsonSchema) throws IOException {

		KiSystem system = KiSystem.fromModel(model);
		String pureModel = KiSystem.extractPureModel(model);

		switch (system) {

		case DEEPSEEK:
			return new ApiKIDeepSeek(vereinnr, frage, pureModel, temperatur, frequencyPenalty, presencePenalty,
					jsonSchema);

		case CLAUDE:
			return new ApiKIClaude(vereinnr, frage, pureModel, thinking, temperatur, frequencyPenalty, presencePenalty,
					jsonSchema);

		case GEMINI:
			return new ApiKIGemini(vereinnr, frage, pureModel, thinking, temperatur, frequencyPenalty, presencePenalty,
					jsonSchema);

		case CHATGPT:
		default:
			return new ApiKIChatGPT(vereinnr, frage, pureModel, thinking, temperatur, frequencyPenalty, presencePenalty,
					jsonSchema);
		}
	}
}