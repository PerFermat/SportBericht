package de.bericht.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import de.bericht.util.enums.KiSystem;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OpenAIModelFetcher {

	private static final String OPENAI_API_URL = "https://api.openai.com/v1/models";
	private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/models";
	private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/models";
	private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models";
	private static final String ANTHROPIC_VERSION = "2023-06-01";

	private final List<String> modelNames = new ArrayList<>();

	public OpenAIModelFetcher(String vereinnr) {

		try {
			loadModelNames(vereinnr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadModelNames(String vereinnr) throws IOException {
		String openAiApiKey = ConfigManager.getChatGptPasswort(vereinnr);
		String deepSeekApiKey = ConfigManager.getDeepSeekPasswort(vereinnr);
		String claudeApiKey = ConfigManager.getClaudeApi(vereinnr);
		String geminiApiKey = ConfigManager.getGeminiApiKey(vereinnr);

		Set<String> allModels = new LinkedHashSet<>();

		if (isSet(openAiApiKey)) {
			allModels.addAll(prefixModels(fetchModelsBearer(OPENAI_API_URL, openAiApiKey), KiSystem.CHATGPT));
		}

		if (isSet(deepSeekApiKey)) {
			allModels.addAll(prefixModels(fetchModelsBearer(DEEPSEEK_API_URL, deepSeekApiKey), KiSystem.DEEPSEEK));
		}

		if (isSet(claudeApiKey)) {
			allModels.addAll(prefixModels(fetchClaudeModels(claudeApiKey), KiSystem.CLAUDE));
		}

		if (isSet(geminiApiKey)) {
			allModels.addAll(prefixModels(fetchGeminiModels(vereinnr, geminiApiKey), KiSystem.GEMINI));
		}
		modelNames.clear();
		modelNames.addAll(allModels);
	}

	private List<String> prefixModels(List<String> models, KiSystem system) {
		List<String> result = new ArrayList<>();

		for (String model : models) {
			result.add(system.formatModel(model));
		}

		return result;
	}

	/** Für OpenAI-kompatible APIs (Authorization: Bearer …) */
	private List<String> fetchModelsBearer(String apiUrl, String apiKey) throws IOException {
		List<String> names = new ArrayList<>();
		if (!isSet(apiKey)) {
			return names;
		}

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(apiUrl).header("Authorization", "Bearer " + apiKey).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				return names;
			}

			JSONObject json = new JSONObject(response.body().string());
			JSONArray models = json.optJSONArray("data");
			if (models == null) {
				return names;
			}

			for (int i = 0; i < models.length(); i++) {
				names.add(models.getJSONObject(i).getString("id"));
			}
		}
		return names;
	}

	private List<String> fetchClaudeModels(String api) throws IOException {
		List<String> names = new ArrayList<>();

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(CLAUDE_API_URL).header("x-api-key", api)
				.header("anthropic-version", ANTHROPIC_VERSION).get().build();

		try (Response response = client.newCall(request).execute()) {

			if (!response.isSuccessful() || response.body() == null) {
				return names;
			}

			JSONObject json = new JSONObject(response.body().string());
			JSONArray models = json.optJSONArray("data");

			if (models == null) {
				return names;
			}

			for (int i = 0; i < models.length(); i++) {

				JSONObject model = models.getJSONObject(i);

				String id = model.optString("id", null);

				if (id != null && !id.isBlank() && isTextCapableModel(id)) {
					names.add(id);
				}
			}
		}

		return names;
	}

	private List<String> fetchGeminiModels(String vereinnr, String apiKey) throws IOException {
		List<String> names = new ArrayList<>();

		OkHttpClient client = new OkHttpClient();

		HttpUrl url = HttpUrl.parse(GEMINI_API_URL).newBuilder().addQueryParameter("key", apiKey).build();

		Request request = new Request.Builder().url(url).get().build();

		try (Response response = client.newCall(request).execute()) {

			if (!response.isSuccessful() || response.body() == null) {
				return names;
			}

			JSONObject json = new JSONObject(response.body().string());
			JSONArray models = json.optJSONArray("models");

			if (models == null) {
				return names;
			}

			for (int i = 0; i < models.length(); i++) {
				JSONObject model = models.getJSONObject(i);

				String fullName = model.optString("name", "");
				if (fullName.startsWith("models/")) {
					fullName = fullName.replace("models/", "");
				}

				// 👉 HIER: Filter anwenden
				if (!fullName.isBlank() && isFreeTierModel(vereinnr, fullName) && isTextCapableModel(fullName)) {
					names.add(fullName);
				}
			}
		}

		return names;
	}

	private boolean isTextCapableModel(String modelName) {

		String lower = modelName.toLowerCase();

		// =========================
		// ❌ HARTE AUSSCHLÜSSE
		// =========================

		if (lower.contains("antigravity")) {
			return false;
		}

		if (lower.toLowerCase().matches(".*(^|[-_])aqa([-_]|$).*")) {
			return false;
		}

		if (lower.contains("image") || lower.contains("imagen") || lower.contains("vision") || lower.contains("audio")
				|| lower.contains("speech") || lower.contains("tts") || lower.contains("embedding")
				|| lower.contains("rerank") || lower.contains("retrieval") || lower.contains("tool")) {
			return false;
		}

		// =========================
		// ✅ SONST ALLES TEXTFÄHIG
		// =========================

		return true;
	}

	private boolean isFreeTierModel(String vereinnr, String name) {

		String premium = ConfigManager.getConfigValue(vereinnr, "ki.gemini.premium");

		if (premium.equals("Ja")) {
			return true;
		}

		String lower = name.toLowerCase();

		// ❌ Harte Ausschlüsse (teuer / high-end)
		if (lower.contains("pro") || lower.contains("ultra")) {
			return false;
		}

		// ❌ bekannte problematische Varianten
		if (lower.contains("vision-pro")) {
			return false;
		}

		// ❌ bekannte problematische Varianten
		if (lower.contains("image")) {
			return false;
		}

		// ✅ bevorzugte kostenlose Modelle
		if (lower.contains("flash")) {
			return true;
		}

		if (lower.contains("lite")) {
			return true;
		}

		if (lower.contains("embedding")) {
			return true;
		}

		if (lower.contains("preview")) {
			return true;
		}

		return true;
	}

	private static boolean isSet(String key) {
		return key != null && !key.isBlank();
	}

	public List<String> getModelNames() {
		return new ArrayList<>(modelNames);
	}
}