package de.bericht.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import de.bericht.provider.KiProvider;

public class ApiKIGemini implements KiProvider {

	private String responses = "";

	public ApiKIGemini(String vereinnr, String frage, String selectedModel, String thinking, double temperatur,
			double frequencyPenalty, double presencePenalty, JSONObject jsonSchema) throws IOException {

		System.out.println("===== GEMINI DEBUG START =====");

		ConfigManager config = ConfigManager.getInstance();
		String apiKey = config.getGeminiApiKey(vereinnr);

		System.out.println("[Gemini] API-Key geladen: " + (apiKey != null && !apiKey.isBlank()));

		if (apiKey == null || apiKey.isEmpty()) {
			responses = "Fehler: Kein Gemini API-Key gesetzt.";
			System.out.println("[Gemini] FEHLER: Kein API-Key!");
			return;
		}

		// -----------------------------
		// Model
		// -----------------------------
		String model = (selectedModel == null || selectedModel.isBlank()) ? "gemini-1.5-flash" : selectedModel;

		String urlString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key="
				+ apiKey;

		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(60000);

		// -----------------------------
		// Prompt
		// -----------------------------
		String finalPrompt = frage;

		System.out.println("[Gemini] Frage: " + frage);

		if (jsonSchema != null) {
			finalPrompt += "\n\nACHTUNG: Antworte ausschließlich als gültiges JSON.\n" + jsonSchema.toString(2);
		}

		// -----------------------------
		// Payload
		// -----------------------------
		JSONObject payload = new JSONObject();

		JSONArray contents = new JSONArray();

		JSONObject userMessage = new JSONObject().put("role", "user").put("parts",
				new JSONArray().put(new JSONObject().put("text", finalPrompt)));

		contents.put(userMessage);
		payload.put("contents", contents);

		JSONObject generationConfig = new JSONObject();
		generationConfig.put("temperature", Math.max(0.0, Math.min(2.0, temperatur)));
		payload.put("generationConfig", generationConfig);

		// -----------------------------
		// Request senden
		// -----------------------------
		try (OutputStream os = connection.getOutputStream()) {

			byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);

			os.write(input);
			os.flush();
		}

		// -----------------------------
		// Response Code
		// -----------------------------
		int statusCode = connection.getResponseCode();

		// -----------------------------
		// Fehlerfall
		// -----------------------------
		if (statusCode != 200) {
			StringBuilder errorResponse = new StringBuilder();

			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {

				String line;
				while ((line = br.readLine()) != null) {
					errorResponse.append(line);
				}
			}

			responses = "Fehler: API Status " + statusCode
					+ (errorResponse.length() > 0 ? " Details: " + errorResponse : "");

			return;
		}

		// -----------------------------
		// Response lesen
		// -----------------------------
		StringBuilder responseBuilder = new StringBuilder();

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

			String line;
			while ((line = br.readLine()) != null) {
				responseBuilder.append(line.trim());
			}
		}

		// -----------------------------
		// JSON parse
		// -----------------------------
		try {
			JSONObject json = new JSONObject(responseBuilder.toString());

			if (json.has("error")) {
				System.out.println("[Gemini] API ERROR JSON: " + json);
				responses = "API-Fehler: " + json.getJSONObject("error").getString("message");
				return;
			}

			JSONArray candidates = json.optJSONArray("candidates");

			if (candidates == null || candidates.length() == 0) {
				System.out.println("[Gemini] Keine candidates im Response!");
				responses = "Keine Antwort erhalten.";
				return;
			}

			JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
			JSONArray parts = content.getJSONArray("parts");

			StringBuilder text = new StringBuilder();

			for (int i = 0; i < parts.length(); i++) {
				String partText = parts.getJSONObject(i).optString("text");
				System.out.println("[Gemini] Part " + i + ": " + partText);
				text.append(partText);
			}

			responses = text.toString().trim();

		} catch (Exception e) {
			System.out.println("[Gemini] PARSE ERROR: " + e.getMessage());
			e.printStackTrace();

			responses = "Fehler beim Verarbeiten: " + e.getMessage();
		}

	}

	@Override
	public String getResponse() {
		return responses;
	}

	@Override
	public boolean isSuccessful() {
		return !responses.startsWith("Fehler");
	}
}