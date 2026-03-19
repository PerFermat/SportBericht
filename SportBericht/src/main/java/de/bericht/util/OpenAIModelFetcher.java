package de.bericht.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OpenAIModelFetcher {
	private static final String API_URL = "https://api.openai.com/v1/models";
	private List<String> modelNames = new ArrayList<>();
	private final String apiKey;

	public OpenAIModelFetcher(String apiKey) {
		this.apiKey = apiKey;
		try {
			setModelNames();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setModelNames() throws IOException {
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder().url(API_URL).header("Authorization", "Bearer " + apiKey).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("API-Fehler: " + response.code() + " - " + response.message());
			}

			String jsonData = response.body().string();
			JSONObject jsonObject = new JSONObject(jsonData);
			JSONArray models = jsonObject.getJSONArray("data");

			for (int i = 0; i < models.length(); i++) {
				JSONObject model = models.getJSONObject(i);
				modelNames.add(model.getString("id"));
			}
		}

	}

	public List<String> getModelNames() throws IOException {
		return modelNames;
	}
}
