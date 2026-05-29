package de.bericht.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

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
			// OpenAI: Auth via "Authorization: Bearer …", Antwort: { data: [{id: "..."}] }
			allModels.addAll(fetchModelsBearer(OPENAI_API_URL, openAiApiKey));
		}

		if (isSet(deepSeekApiKey)) {
			// DeepSeek: gleiche Struktur wie OpenAI
			allModels.addAll(fetchModelsBearer(DEEPSEEK_API_URL, deepSeekApiKey));
		}

		if (isSet(claudeApiKey)) {
			// Anthropic: Auth via "x-api-key", zusätzlich "anthropic-version" Header
			// Antwort: { data: [{id: "claude-...", ...}] }
			allModels.addAll(fetchClaudeModels(claudeApiKey));
		}

		if (isSet(geminiApiKey)) {
			allModels.addAll(fetchGeminiModels(geminiApiKey));
		}
		modelNames.clear();
		modelNames.addAll(allModels);
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

	/** Für die Anthropic Claude API (x-api-key + anthropic-version Header) */
	private List<String> fetchClaudeModels(String api) throws IOException {
		List<String> names = new ArrayList<>();

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(CLAUDE_API_URL).header("x-api-key", api)
				.header("anthropic-version", ANTHROPIC_VERSION).get().build();

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
				JSONObject model = models.getJSONObject(i);
				// Anthropic liefert u.a. "id", "display_name", "created_at"
				String id = model.optString("id", null);
				if (id != null && !id.isBlank()) {
					names.add(id);
				}
			}
		}
		return names;
	}

	private List<String> fetchGeminiModels(String apiKey) throws IOException {
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

				if (!fullName.isBlank()) {
					names.add(fullName);
				}
			}
		}

		return names;
	}

	private static boolean isSet(String key) {
		return key != null && !key.isBlank();
	}

	public List<String> getModelNames() {
		return new ArrayList<>(modelNames);
	}
}