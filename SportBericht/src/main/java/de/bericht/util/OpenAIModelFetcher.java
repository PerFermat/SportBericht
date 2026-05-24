package de.bericht.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OpenAIModelFetcher {
	private static final String OPENAI_API_URL = "https://api.openai.com/v1/models";
	private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/models";

	private final List<String> modelNames = new ArrayList<>();
	private final String openAiApiKey;
	private final String deepSeekApiKey;

	public OpenAIModelFetcher(String openAiApiKey) {
		this(openAiApiKey, null);
	}

	public OpenAIModelFetcher(String openAiApiKey, String deepSeekApiKey) {
		this.openAiApiKey = openAiApiKey;
		this.deepSeekApiKey = deepSeekApiKey;
		try {
			loadModelNames();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadModelNames() throws IOException {
		Set<String> allModels = new LinkedHashSet<>();
		allModels.addAll(fetchModels(OPENAI_API_URL, openAiApiKey));
		allModels.addAll(fetchModels(DEEPSEEK_API_URL, deepSeekApiKey));
		modelNames.clear();
		modelNames.addAll(allModels);
	}

	private List<String> fetchModels(String apiUrl, String apiKey) throws IOException {
		List<String> names = new ArrayList<>();
		if (apiKey == null || apiKey.isBlank()) {
			return names;
		}

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(apiUrl).header("Authorization", "Bearer " + apiKey).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				return names;
			}

			String jsonData = response.body().string();
			JSONObject jsonObject = new JSONObject(jsonData);
			JSONArray models = jsonObject.optJSONArray("data");
			if (models == null) {
				return names;
			}

			for (int i = 0; i < models.length(); i++) {
				JSONObject model = models.getJSONObject(i);
				names.add(model.getString("id"));
			}
		}
		return names;
	}

	public List<String> getModelNames() {
		return new ArrayList<>(modelNames);
	}
}