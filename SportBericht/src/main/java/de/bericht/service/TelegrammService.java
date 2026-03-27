package de.bericht.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import de.bericht.util.ConfigManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TelegrammService {

	private String botToken;
	private static final OkHttpClient client = new OkHttpClient();

	public int sendTelegramm(String vereinnr, String messange, byte[] attachment, String bildUnterschrift)
			throws Exception {
		ConfigManager config = ConfigManager.getInstance();
		String botToken = config.getTelegrammToken(vereinnr);
		if (botToken == null || botToken.isEmpty()) {
			return -1;
		}

		String chatId = getLatestChatId(vereinnr, botToken);

		if (chatId != null) {
			if (attachment == null) {
				sendMessage(chatId, messange, botToken);
			} else {
				// Bild mit kurzer Caption senden (z.B. nur "Bild")
				sendPhotoWithCaption(chatId, attachment, bildUnterschrift, botToken);
				// Danach Text separat senden
				sendMessage(chatId, messange, botToken);
			}
			return 1;
		} else {
			return -1;
		}
	}

	// Holt die letzte chat_id aus getUpdates
	public static String getLatestChatId(String vereinnr, String botToken) throws Exception {

		String chat_Id = ConfigManager.getConfigValue(vereinnr, "telegram.chat_id");
		if (chat_Id != null && chat_Id.length() > 0 && !"N".equals(chat_Id)) {
			return chat_Id;
		}

		String urlString = String.format("https://api.telegram.org/bot%s/getUpdates", botToken);

		HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
		conn.setRequestMethod("GET");

		InputStream is = conn.getInputStream();
		Scanner scanner = new Scanner(is).useDelimiter("\\A");
		String response = scanner.hasNext() ? scanner.next() : "";

		JSONObject json = new JSONObject(response);
		JSONArray result = json.getJSONArray("result");

		if (result.length() > 0) {
			// Holt die letzte Nachricht
			JSONObject lastUpdate = result.getJSONObject(result.length() - 1);
			JSONObject message = lastUpdate.getJSONObject("message");
			JSONObject chat = message.getJSONObject("chat");

			DatabaseService dbService = new DatabaseService(vereinnr);
			chat_Id = chat.get("id").toString();
			dbService.insertOrUpdateConfigEintrag(vereinnr, "telegram.chat_id", chat_Id);

			return chat_Id;
		}

		return null;
	}

	// Sendet eine Nachricht an eine bestimmte chat_id (Text)
	public static void sendMessage(String chatId, String message, String botToken) throws Exception {
		String encodedMessage = URLEncoder.encode(message, "UTF-8");
		String urlString = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s", botToken,
				chatId, encodedMessage);

		HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
		conn.setRequestMethod("GET");

		int responseCode = conn.getResponseCode();

		Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
	}

	public static void sendPhotoWithCaption(String chatId, byte[] attachment, String caption, String botToken)
			throws IOException {
		// Telegram API URL
		String url = "https://api.telegram.org/bot" + botToken + "/sendPhoto";

		// MultipartBody bauen
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				// chat_id
				.addFormDataPart("chat_id", chatId)
				// caption
				.addFormDataPart("caption", caption)
				// photo mit Datei-Inhalt
				.addFormDataPart("photo", "image.jpg", RequestBody.create(attachment, MediaType.parse("image/jpeg")))
				.build();

		// Request bauen
		Request request = new Request.Builder().url(url).post(requestBody).build();

		// Request ausführen und Antwort lesen
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unerwarteter HTTP-Code " + response.code() + ": " + response.body().string());
			}
		}
	}
}
