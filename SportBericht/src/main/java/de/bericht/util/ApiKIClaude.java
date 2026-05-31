package de.bericht.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import de.bericht.provider.KiProvider;

public class ApiKIClaude implements KiProvider {

	private static final String API_URL = "https://api.anthropic.com/v1/messages";
	private static final String ANTHROPIC_VERSION = "2023-06-01";

	private static final Set<String> THINKING_MODELS = Set.of("claude-sonnet-4-6", "claude-haiku-4-5",
			"claude-haiku-4-5-20251001", "claude-sonnet-4-20250514", "claude-opus-4-20250514",
			"claude-3-7-sonnet-20250219");

	private String responses = "";

	public ApiKIClaude(String vereinnr, String frage, String selectedModel, String thinking, double temperatur,
			double frequencyPenalty, double presencePenalty, JSONObject jsonSchema) throws IOException {

		long startTime = System.currentTimeMillis();

		String apiKey = ConfigManager.getClaudeApi(vereinnr);

		if (apiKey == null || apiKey.isEmpty()) {
			System.err.println("❌ Kein API Key vorhanden!");
			responses = "Fehler: Kein Claude API-Key gesetzt.";
			return;
		}

		int thinkingBudget = 0;
		boolean supportsThinking = supportsThinking(selectedModel);

		if (supportsThinking && thinking != null && !thinking.isBlank()) {
			try {
				thinkingBudget = Integer.parseInt(thinking.trim());
			} catch (Exception e) {
				System.err.println("⚠️ Thinking konnte nicht geparst werden: " + thinking);
			}
		}

		String finalPrompt = frage;

		if (jsonSchema != null) {
			finalPrompt += "\n\nJSON Schema:\n" + jsonSchema.toString(2);
		}

		// Verbindung
		URL url = new URL(API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("x-api-key", apiKey);
		connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);
		connection.setRequestProperty("User-Agent", "Claude-Debug-Client/1.0");

		connection.setDoOutput(true);
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(120000);

		JSONObject payload = new JSONObject();
		payload.put("model", selectedModel);
		payload.put("max_tokens", 8192);

		double clampedTemp = Math.max(0.0, Math.min(1.0, temperatur));
		if (supportsTemperature(selectedModel)) {
			payload.put("temperature", clampedTemp);
		}

		if (thinkingBudget > 0) {
			JSONObject thinkingObj = new JSONObject();
			thinkingObj.put("type", "enabled");
			thinkingObj.put("budget_tokens", thinkingBudget);
			payload.put("thinking", thinkingObj);
			payload.put("temperature", 1.0);
		}

		JSONArray messages = new JSONArray();
		messages.put(new JSONObject().put("role", "user").put("content", finalPrompt));

		payload.put("messages", messages);

		// Senden
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
			os.write(input);
			os.flush();
		} catch (Exception e) {
			System.err.println("❌ Fehler beim Schreiben des Requests");
			e.printStackTrace();
			responses = "Fehler beim Senden: " + e.getMessage();
			return;
		}

		int statusCode = connection.getResponseCode();

		InputStream stream;

		if (statusCode >= 200 && statusCode < 300) {
			stream = connection.getInputStream();
		} else {
			System.err.println("❌ Fehlerstatus erhalten!");
			stream = connection.getErrorStream();
			if (stream == null) {
				System.err.println("⚠️ Kein Error-Stream vorhanden!");
				responses = "Fehler: Status " + statusCode + " ohne Body";
				return;
			}
		}

		StringBuilder responseBuilder = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				responseBuilder.append(line).append("\n");
			}
		}

		String rawResponse = responseBuilder.toString();

		if (statusCode != 200) {
			responses = "Fehler: HTTP " + statusCode + "\n" + rawResponse;
			return;
		}

		// JSON Parsing
		try {
			JSONObject jsonResponse = new JSONObject(rawResponse);

			if (jsonResponse.has("error")) {
				System.err.println("❌ API Error gefunden!");
				responses = jsonResponse.getJSONObject("error").optString("message", "Unbekannter API Fehler");
				return;
			}

			JSONArray content = jsonResponse.optJSONArray("content");

			if (content == null) {
				System.err.println("⚠️ Kein content Feld!");
				responses = "Keine Antwort (content fehlt)";
				return;
			}

			StringBuilder textBuilder = new StringBuilder();

			for (int i = 0; i < content.length(); i++) {
				JSONObject block = content.getJSONObject(i);

				System.out.println("Block " + i + ": " + block);

				if ("text".equals(block.optString("type"))) {
					textBuilder.append(block.optString("text")).append("\n");
				}
			}

			responses = textBuilder.toString().trim();

			if (responses.isEmpty()) {
				responses = "Keine Textantwort erhalten.";
			}

		} catch (Exception e) {
			System.err.println("❌ JSON Parsing Fehler");
			e.printStackTrace();
			responses = "Parsing Fehler: " + e.getMessage();
		}

		long duration = System.currentTimeMillis() - startTime;
		System.out.println("==== Claude API CALL END (" + duration + " ms) ====");
	}

	public static boolean supportsThinking(String modelName) {
		if (modelName == null) {
			return false;
		}
		String m = modelName.toLowerCase();
		return THINKING_MODELS.contains(m) || m.startsWith("claude-sonnet-4") || m.startsWith("claude-opus-4")
				|| m.equals("claude-3-7-sonnet-20250219");
	}

	@Override
	public String getResponse() {
		return responses;
	}

	@Override
	public boolean isSuccessful() {
		return !responses.startsWith("Fehler");
	}

	/**
	 * Nur ALTE Claude-Modelle unterstützen temperature. Neue Modelle (4.x und alles
	 * danach) NICHT mehr.
	 */
	private static boolean supportsTemperature(String model) {
		if (model == null) {
			return false;
		}

		String m = model.trim().toLowerCase();

		// ✅ Whitelist: bekannte alte Modelle
		return m.startsWith("claude-3") || m.startsWith("claude-2") || m.startsWith("claude-instant");
	}
}