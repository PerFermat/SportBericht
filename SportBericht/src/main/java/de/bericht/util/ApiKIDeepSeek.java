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
import org.json.JSONTokener;

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

		return (frage == null ? "" : frage)
				+ "\n\nACHTUNG: Antworte ausschließlich als gültiges JSON ohne Zusatztext:\n" + jsonSchema.toString(); // 🔥
																														// reduziert
																														// Payload
																														// (kein
																														// pretty
																														// print
																														// mehr)
	}

	private String normalizeAssistantContent(String content) {
		if (content == null) {
			return "";
		}

		String normalized = content.trim();

		normalized = normalized.replaceAll("```[a-zA-Z0-9]*", "").replace("```", "").trim();

		if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
			try {
				Object parsed = new JSONTokener(normalized).nextValue();
				if (parsed instanceof String) {
					normalized = ((String) parsed).trim();
				}
			} catch (Exception e) {
				debug("Decode-Fehler: " + e.getMessage());
			}
		}

		return normalized;
	}

	public ApiKIDeepSeek(String vereinnr, String frage, String selectedModel, double temperatur,
			double frequencyPenalty, double presencePenalty, JSONObject jsonSchema) throws IOException {

		long start = System.currentTimeMillis();

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

		// 🔥 WICHTIG: verhindert viele "hanging connections"
		connection.setRequestProperty("Connection", "close");

		connection.setDoOutput(true);
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(120000); // 🔥 etwas realistischer für LLMs

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

		debug("Sende Request. Prompt-Länge=" + finalPrompt.length() + ", Schema=" + (jsonSchema != null));

		try (OutputStream os = connection.getOutputStream()) {
			os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
		}

		int statusCode = connection.getResponseCode();
		debug("HTTP-Status=" + statusCode);

		if (statusCode != 200) {
			StringBuilder error = new StringBuilder();

			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {

				String line;
				while ((line = br.readLine()) != null) {
					error.append(line);
				}
			}

			response = "Fehler: HTTP " + statusCode + " " + error;
			debug("Fehlerantwort: " + response);
			return;
		}

		// 🔥 FIX: kein endlos blockierendes readLine() mehr bei Chunking-Problemen
		StringBuilder responseBuilder = new StringBuilder();

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

			int c;
			while ((c = br.read()) != -1) { // 🔥 robuster als readLine()
				responseBuilder.append((char) c);
			}
		}

		String rawResponse = responseBuilder.toString();

		debug("Rohantwort-Länge=" + rawResponse.length());

		JSONObject jsonResponse;
		try {
			jsonResponse = new JSONObject(rawResponse);
		} catch (Exception e) {
			response = "Fehler: Ungültige JSON-Antwort";
			debug("JSON Parse Fehler: " + e.getMessage());
			return;
		}

		if (jsonResponse.has("error")) {
			response = "API-Fehler: " + jsonResponse.getJSONObject("error").optString("message", "Unbekannt");
			return;
		}

		JSONArray choices = jsonResponse.optJSONArray("choices");

		if (choices == null || choices.isEmpty()) {
			response = "Keine Antwort erhalten.";
			return;
		}

		String content = choices.getJSONObject(0).getJSONObject("message").optString("content", "");

		response = normalizeAssistantContent(content);

		debug("Antwort-Länge=" + response.length());

		if (!response.isBlank()) {
			String preview = response.length() > 160 ? response.substring(0, 160) + "..." : response;

			debug("Antwort-Preview: " + preview.replace("\n", " "));
		}

		debug("Gesamtdauer ms=" + (System.currentTimeMillis() - start));
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