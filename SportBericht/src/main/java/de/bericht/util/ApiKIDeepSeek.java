package de.bericht.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiKIDeepSeek {

	String response;

	public String getResponse() {
		return response;
	}

	public ApiKIDeepSeek(String frage) throws IOException {
		// URL der API
		URL url = new URL("https://api.deepseek.com/chat/completions");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// HTTP-Methode und Header setzen
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer sk-f070c90696884ccfb4a86eab8d8bbb3e");
		connection.setDoOutput(true);
		String escapedString = frage.replace("\"", "\\\"");
		// JSON-Daten erstellen
		String jsonInputString = "{" + "\"model\": \"deepseek-chat\"," + "\"messages\": ["
				+ "    {\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},"
				+ "    {\"role\": \"user\", \"content\": \"" + escapedString + "\"}" // Frage wird hier eingefügt
				+ "]," + "\"stream\": false" + "}";

		// Daten senden
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		// Antwort lesen
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			// Erfolgreiche Antwort verarbeiten
			java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream()).useDelimiter("\\A");
			response = scanner.hasNext() ? scanner.next() : "";
		} else {
			// Fehlerbehandlung
			response = "Fehler: " + responseCode;
		}
	}

}
