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

public class GPTClient {
	private String responses = "";

	public GPTClient(String vereinnr, String frage, String selectedModel, double temperatur, double frequencyPenalty,
			double presencePenalty) throws IOException {

		ConfigManager config = ConfigManager.getInstance();
		String apiKey = config.getChatGptPasswort(vereinnr);
		if (apiKey == null || apiKey.isEmpty()) {
			responses = "Fehler: Kein API-Key gesetzt. Überprüfe die API-Key Konfiguration.";
			return;
		}

		//
		// 1) --- Modell-Fähigkeiten abfragen (WICHTIG) ---
		//
		JSONObject capabilities = fetchModelCapabilities(selectedModel, apiKey);
		boolean supportsTemperature = capabilities.optBoolean("temperature", false);
		boolean supportsFreqPenalty = capabilities.optBoolean("frequency_penalty", false);
		boolean supportsPresPenalty = capabilities.optBoolean("presence_penalty", false);

		//
		// 2) --- HTTP Anfrage vorbereiten ---
		//
		String urlString = "https://api.openai.com/v1/chat/completions";
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setRequestProperty("User-Agent", "ChatGPT-Client/1.0");
		connection.setDoOutput(true);

		connection.setConnectTimeout(30000);
		connection.setReadTimeout(60000);

		//
		// 3) --- Payload erstellen ---
		//
		JSONObject jsonPayload = new JSONObject();
		jsonPayload.put("model", selectedModel);
		jsonPayload.put("n", 1);
		jsonPayload.put("stream", false);

		if (supportsTemperature) {
			jsonPayload.put("temperature", Math.max(0.0, Math.min(2.0, temperatur)));
		}

		if (supportsFreqPenalty) {
			jsonPayload.put("frequency_penalty", Math.max(-2.0, Math.min(2.0, frequencyPenalty)));
		}

		if (supportsPresPenalty) {
			jsonPayload.put("presence_penalty", Math.max(-2.0, Math.min(2.0, presencePenalty)));
		}

		JSONArray messages = new JSONArray();
		JSONObject message = new JSONObject();
		message.put("role", "user");
		message.put("content", frage);
		messages.put(message);

		jsonPayload.put("messages", messages);

		//
		// 4) --- Request senden ---
		//
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = jsonPayload.toString().getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		int statusCode = connection.getResponseCode();
		if (statusCode != 200) {
			StringBuilder errorResponse = new StringBuilder();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
				String errorLine;
				while ((errorLine = br.readLine()) != null) {
					errorResponse.append(errorLine);
				}
			}
			responses = "Fehler: API antwortete mit Statuscode " + statusCode
					+ (errorResponse.length() > 0 ? ". Details: " + errorResponse.toString() : "");
			return;
		}

		//
		// 5) --- Antwort auslesen ---
		//
		StringBuilder responseBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			String responseLine;
			while ((responseLine = br.readLine()) != null) {
				responseBuilder.append(responseLine.trim());
			}
		}

		try {
			JSONObject jsonResponse = new JSONObject(responseBuilder.toString());

			if (jsonResponse.has("error")) {
				responses = "API-Fehler: " + jsonResponse.getJSONObject("error").getString("message");
				return;
			}

			JSONArray choices = jsonResponse.getJSONArray("choices");
			if (choices.length() == 0) {
				responses = "Keine Antwort erhalten.";
			} else {
				JSONObject choice = choices.getJSONObject(0);
				JSONObject messageObj = choice.getJSONObject("message");
				String content = messageObj.getString("content");
				responses = content.trim();
			}

		} catch (Exception e) {
			responses = "Fehler beim Verarbeiten der API-Antwort: " + e.getMessage();
		}
	}

	/**
	 * Hilfsfunktion: Fetch capabilities from the model metadata
	 */
	private JSONObject fetchModelCapabilities(String modelName, String apiKey) {
		try {
			URL url = new URL("https://api.openai.com/v1/models/" + modelName);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "Bearer " + apiKey);

			if (conn.getResponseCode() != 200) {
				return new JSONObject();
			}

			StringBuilder response = new StringBuilder();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					response.append(line);
				}
			}

			JSONObject json = new JSONObject(response.toString());
			// capabilities sind hier
			return json.optJSONObject("capabilities") != null ? json.getJSONObject("capabilities") : new JSONObject();

		} catch (Exception e) {
			return new JSONObject();
		}
	}

	public String getResponse() {
		return responses;
	}

	public boolean isSuccessful() {
		return !responses.startsWith("Fehler:") && !responses.startsWith("API-Fehler:");
	}
}
