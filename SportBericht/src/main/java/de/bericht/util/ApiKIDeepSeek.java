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

public class ApiKIDeepSeek implements KiProvider {

	private String response = "";

	private void debug(String message) {
		System.out.println("[DeepSeek] " + message);
	}

	private String maskApiKey(String apiKey) {
		if (apiKey == null || apiKey.isBlank()) {
			return "<leer>";
		}
		if (apiKey.length() <= 8) {
			return "****";
		}
		return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
	}

	private String withSchemaPrompt(String frage, JSONObject jsonSchema) {
		if (jsonSchema == null) {
			return frage;
		}
		String base = frage == null ? "" : frage;
		return base
				+ "\n\nACHTUNG: Antworte ausschließlich als gültiges JSON ohne Zusatztext. Nutze exakt dieses Schema:\n"
				+ jsonSchema.toString(2);
	}

	public ApiKIDeepSeek(String vereinnr, String frage, String selectedModel, double temperatur,
			double frequencyPenalty, double presencePenalty, JSONObject jsonSchema) throws IOException {
		String apiKey = ConfigManager.getDeepSeekPasswort(vereinnr);
		debug("Aufruf gestartet. Verein=" + vereinnr + ", Modell=" + selectedModel + ", Key=" + maskApiKey(apiKey));
		if (apiKey == null || apiKey.isBlank()) {
			response = "Fehler: Kein DeepSeek API-Key gesetzt.";
			return;
		}

		URL url = new URL("https://api.deepseek.com/chat/completions");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setDoOutput(true);
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(60000);

		JSONObject payload = new JSONObject();
		payload.put("model", (selectedModel == null || selectedModel.isBlank()) ? "deepseek-chat" : selectedModel);
		payload.put("stream", false);
		payload.put("temperature", Math.max(0.0, Math.min(2.0, temperatur)));
		payload.put("frequency_penalty", Math.max(-2.0, Math.min(2.0, frequencyPenalty)));
		payload.put("presence_penalty", Math.max(-2.0, Math.min(2.0, presencePenalty)));

		String finalPrompt = withSchemaPrompt(frage, jsonSchema);
		JSONArray messages = new JSONArray();
		messages.put(new JSONObject().put("role", "user").put("content", finalPrompt));
		payload.put("messages", messages);

		debug("Sende Request an DeepSeek. Prompt-Länge=" + (finalPrompt == null ? 0 : finalPrompt.length())
				+ ", Schema=" + (jsonSchema != null));
		try (OutputStream os = connection.getOutputStream()) {
			os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
		}

		int statusCode = connection.getResponseCode();
		debug("HTTP-Status=" + statusCode);
		if (statusCode != 200) {
			StringBuilder errorResponse = new StringBuilder();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					errorResponse.append(line);
				}
			}
			debug("Fehlerantwort: " + errorResponse);
			response = "Fehler: API antwortete mit Statuscode " + statusCode
					+ (errorResponse.length() > 0 ? ". Details: " + errorResponse : "");
			return;
		}

		StringBuilder responseBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				responseBuilder.append(line);
			}
		}

		String rawResponse = responseBuilder.toString();
		debug("Rohantwort-Länge=" + rawResponse.length());
		JSONObject jsonResponse = new JSONObject(rawResponse);
		if (jsonResponse.has("error")) {
			response = "API-Fehler: " + jsonResponse.getJSONObject("error").optString("message", "Unbekannt");
			debug("API-Fehlerfeld empfangen: " + response);
			return;
		}
		JSONArray choices = jsonResponse.optJSONArray("choices");
		if (choices == null || choices.isEmpty()) {
			response = "Keine Antwort erhalten.";
			debug("Keine choices in der API-Antwort vorhanden.");
			return;
		}
		response = choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim();
		debug("Antwort extrahiert. Zeichen=" + response.length());
	}

	@Override
	public String getResponse() {
		return response;
	}

	@Override
	public boolean isSuccessful() {
		return !response.startsWith("Fehler:") && !response.startsWith("API-Fehler:");
	}
}
