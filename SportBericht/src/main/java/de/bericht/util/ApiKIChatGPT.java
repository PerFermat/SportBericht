package de.bericht.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class ApiKIChatGPT {
	private String responses = "";

	// Modelle, die KNOWN-broken sind (Legacy)
	private static final Set<String> UNSUPPORTED_MODELS = Set.of("gpt-3.5-turbo", "gpt-3.5", "text-davinci-003",
			"gpt-4-vision-preview", "gpt-4-turbo-preview");

	public ApiKIChatGPT(String vereinnr, String frage, String selectedModel, String thinking, double temperatur,
			double frequencyPenalty, double presencePenalty, JSONObject jsonSchema // <<-- NEU: Dein gewünschtes
																					// JSON-Schema
	) throws IOException {

		ConfigManager config = ConfigManager.getInstance();
		String apiKey = config.getChatGptPasswort(vereinnr);

		if (apiKey == null || apiKey.isEmpty()) {
			responses = "Fehler: Kein API-Key gesetzt. Überprüfe die API-Key Konfiguration.";
			return;
		}

		// ---------------------------------------------
		// 1) Modell-Fähigkeiten abfragen
		// ---------------------------------------------
		JSONObject capabilities = fetchModelCapabilities(selectedModel, apiKey);

		boolean supportsTemperature = capabilities.optBoolean("temperature", false);
		boolean supportsFreqPenalty = capabilities.optBoolean("frequency_penalty", false);
		boolean supportsPresPenalty = capabilities.optBoolean("presence_penalty", false);

		boolean supportsJsonSchema = supportsResponseFormat(selectedModel);

		// -----------------------------
		// Thinking-Fähigkeit erkennen
		// -----------------------------
		boolean supportsThinking = supportsThinking(capabilities);

		// "thinking" Parameter normalisieren:
		// null / leer → nichts setzen
		String normalizedThinking = (thinking == null || thinking.isBlank()) ? null : thinking.trim().toLowerCase(); // z.B.
																														// "none",
																														// "low",
																														// "medium",
																														// "high"

		// ---------------------------------------------
		// 2) JSON-Schema Handling
		// ---------------------------------------------
		String finalPrompt = frage;

		if (jsonSchema != null && !supportsJsonSchema) {
			// Schema muss an die Eingabe angehängt werden
			finalPrompt += "\n\nACHTUNG: Du MUSST strikt im gültigen JSON antworten. "
					+ "Kein Text außerhalb des JSON-Objekts." + "Kein erklärender Text, keine Kommentare.:"
					+ jsonSchema.toString(2) + "\n";
		}

		// ---------------------------------------------
		// 3) HTTP Verbindung
		// ---------------------------------------------
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

		// ---------------------------------------------
		// 4) JSON Payload aufbauen
		// ---------------------------------------------
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

		// JSON-SCHEMA NUR EINBAUEN, WENN MODELL ES UNTERSTÜTZT
		if (jsonSchema != null && supportsJsonSchema) {

			JSONObject schemaWrapper = new JSONObject().put("name", "Antwort") // <-- DAS FEHLTE!
					.put("schema", jsonSchema);

			JSONObject responseFormat = new JSONObject().put("type", "json_schema").put("json_schema", schemaWrapper);

			jsonPayload.put("response_format", responseFormat);
		}

		// ---------------------------------------------
		// Thinking / Reasoning hinzufügen
		// ---------------------------------------------
		if (supportsThinking && normalizedThinking != null) {

			JSONObject reasoning = new JSONObject();
			reasoning.put("effort", normalizedThinking); // none | low | medium | high

			jsonPayload.put("reasoning", reasoning);
		}

		// ---------------------------------------------
		// 5) Messages
		// ---------------------------------------------
		JSONArray messages = new JSONArray();
		messages.put(new JSONObject().put("role", "user").put("content", finalPrompt));

		jsonPayload.put("messages", messages);

		// ---------------------------------------------
		// 6) Request senden
		// ---------------------------------------------
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = jsonPayload.toString().getBytes(StandardCharsets.UTF_8);
			os.write(input);
		}

		int statusCode = connection.getResponseCode();
		if (statusCode != 200) {
			StringBuilder errorResponse = new StringBuilder();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					errorResponse.append(line);
				}
			}
			responses = "Fehler: API antwortete mit Statuscode " + statusCode
					+ (errorResponse.length() > 0 ? ". Details: " + errorResponse : "");
			return;
		}

		// ---------------------------------------------
		// 7) Antwort lesen
		// ---------------------------------------------
		StringBuilder responseBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				responseBuilder.append(line.trim());
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
				JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
				responses = msg.getString("content").trim();
			}

		} catch (Exception e) {
			responses = "Fehler beim Verarbeiten der API-Antwort: " + e.getMessage();
		}
	}

	// --------------------------------------------------------
	// Modell-Fähigkeiten laden
	// --------------------------------------------------------
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
			JSONObject caps = json.optJSONObject("capabilities");
			if (caps == null) {
				caps = new JSONObject();
			}

			// Falls "reasoning_modes" im Root liegen (manche Modelle tun das)
			if (json.has("reasoning_modes") && !caps.has("reasoning_modes")) {
				caps.put("reasoning_modes", json.getJSONArray("reasoning_modes"));
			}

			return caps;

		} catch (Exception e) {
			return new JSONObject();
		}
	}

	/**
	 * /** Prüft, ob ein OpenAI-Modell das response_format Feature unterstützt.
	 *
	 * Logik: 1. Prüfe explizit unterstützte Modelle (positive Liste) 2. Prüfe
	 * explizit NICHT unterstützte Modelle (negative Liste) 3. Wenn Modell
	 * unbekannt: Annahme = true (neuere Modelle unterstützen es in der Regel)
	 *
	 * @param modelName Der Name des zu prüfenden Modells
	 * @return true wenn das Modell response_format unterstützt oder unbekannt ist,
	 *         sonst false
	 */
	public static boolean supportsResponseFormat(String modelName) {
		if (modelName == null || modelName.isBlank()) {
			return false; // oder true, je nach gewünschter Interpretation
		}

		if (modelName.contains("chat-latest")) {
			return false;
		}
		String normalized = modelName.trim().toLowerCase();

		if (modelName == null || modelName.isBlank()) {
			return false;
		}

		String m = modelName.trim().toLowerCase();

		// 1: Explizit nicht unterstützt
		if (UNSUPPORTED_MODELS.contains(m)) {
			return false;
		}

		// Fallback: Unbekannte Modelle → wir gehen von Support aus
		return true;
	}

	public String getResponse() {
		return responses;
	}

	public boolean isSuccessful() {
		return !responses.startsWith("Fehler:") && !responses.startsWith("API-Fehler:");
	}

	private boolean supportsThinking(JSONObject capabilities) {

		if (capabilities == null) {
			return false;
		}

		// Moderne Modelle haben ein Feld "reasoning"
		if (capabilities.has("reasoning")) {
			return capabilities.optBoolean("reasoning", false);
		}

		// Manche Modelle liefern ein Array "reasoning_modes"
		if (capabilities.has("reasoning_modes")) {
			return capabilities.getJSONArray("reasoning_modes").length() > 0;
		}

		return false;
	}
}
