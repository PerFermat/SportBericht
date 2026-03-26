package de.bericht.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.service.Bilddaten;
import de.bericht.service.WordpressMedia;
import de.bericht.util.enums.WordpressBeitragsbildOption;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public class WordPressAPIClient {

	private String domain;
	private String encodedAuth;
	private String name;
	private int fehler = 0;
	private HttpClient client;

	/**
	 * Konstruktor für den WordPress API-Client.
	 *
	 * @param domain   Die Domain deines WordPress (z.B. "http://deinedomain.com")
	 * @param username Dein WordPress-Benutzername
	 * @param passwort Dein Anwendungspasswort
	 */
	public WordPressAPIClient(String vereinnr, String username, String passwort, String name) {
		if (name == null) {
			evaluateResponseStatus(-1, "WordPressAPIClient");
			return;
		}
		this.name = name;
		if (ConfigManager.getWordpressValue(vereinnr, name, "domain") != null) {
			ParameterWordPressAPIClient(ConfigManager.getWordpressValue(vereinnr, name, "domain"), username, passwort);
		}
	}

	public void ParameterWordPressAPIClient(String domain, String username, String passwort) {
		this.domain = domain;
		this.encodedAuth = Base64.getEncoder().encodeToString((username + ":" + passwort).getBytes());
		this.client = HttpClient.newHttpClient();
	}

	/**
	 * Ermittelt anhand des Kategorienamens die Kategorie-ID.
	 *
	 * @param categoryName Der Name der Kategorie (z.B. "allgemein")
	 * @return Die Kategorie-ID oder 0, wenn keine passende Kategorie gefunden
	 *         wurde.
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String getCategoryIdByName(List<String> categoryName)
			throws IOException, InterruptedException, URISyntaxException {
		List<Integer> catId = new ArrayList<>();
		for (String kategorie : categoryName) {
			String encodedCategory = URLEncoder.encode(kategorie, "UTF-8");
			String url = this.domain + "/wp-json/wp/v2/categories?search=" + encodedCategory;
			HttpRequest request = HttpRequest.newBuilder().uri(new URI(url))
					.header("Authorization", "Basic " + encodedAuth).GET().build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JSONArray jsonArray = new JSONArray(sanitizeJsonResponse(response.body()));
			if (jsonArray.length() > 0) {
				JSONObject category = jsonArray.getJSONObject(0);
				catId.add(category.getInt("id"));
			}
		}
		return "[" + catId.stream().map(String::valueOf).collect(Collectors.joining(" , ")) + "]";
	}

	/**
	 * Erstellt einen neuen Beitrag in WordPress. Baut Bilder als
	 * Gutenberg-kompatible Blöcke ein.
	 */
	public int createPost(String vereinnr, String ueberschrift, String inhalt, List<Bilddaten> bildArray,
			List<String> categoryName, String datum) throws IOException, InterruptedException, URISyntaxException {

		int i = 0;
		for (Bilddaten bild : bildArray) {
			WordpressMedia media = bild.getMediaId();
			if (media != null) {
				String altText = (bild.getBildUnterschrift() != null && !bild.getBildUnterschrift().isEmpty())
						? " alt=\"" + escapeHtml(bild.getBildUnterschrift()) + "\""
						: "";

				String figcaption = (bild.getBildUnterschrift() != null && !bild.getBildUnterschrift().isEmpty())
						? "<figcaption style=\"text-align: center; font-style: italic; font-size: 0.8em;\">"
								+ escapeHtml(bild.getBildUnterschrift()) + "</figcaption>"
						: "";

				// ✅ Gutenberg-konformer Block:
				// - kein "align":"center" mehr im JSON
				// - figure-Klasse enthält "aligncenter size-large"
				String imageBlock = "<!-- wp:image {\"id\":" + media.getMediaId()
						+ ",\"sizeSlug\":\"large\",\"linkDestination\":\"none\"} -->"
						+ "<figure class=\"wp-block-image size-large aligncenter\">" + "<img src=\""
						+ escapeHtml(media.getUrl()) + "\"" + altText + " />" + figcaption
						+ "</figure><!-- /wp:image -->";

				inhalt = inhalt.replace("[BILD" + i + "]", imageBlock);
				inhalt = inhalt.replace("[UNTERSCHRIFT" + i + "]", escapeHtml(bild.getBildUnterschrift()));
			}
			i++;
		}

		int mediaId = -1;
		if (!bildArray.isEmpty() && bildArray.get(0).getMediaId() != null) {
			mediaId = bildArray.get(0).getMediaId().getMediaId();
		}

		return createPost(vereinnr, ueberschrift, inhalt, mediaId, categoryName, datum, null);
	}

	public int createPost(String vereinnr, String ueberschrift, String inhalt, int mediaId, List<String> categoryName,
			String datum, String mediaUrl) throws IOException, InterruptedException, URISyntaxException {

		if (categoryName == null || categoryName.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Keine Kategorie ausgewählt "));
			return -1;
		}
		String catId = getCategoryIdByName(categoryName);

		String url = this.domain + "/wp-json/wp/v2/posts";
		WordpressBeitragsbildOption bildVariante = WordpressBeitragsbildOption
				.fromConfig(ConfigManager.getWordpressValue(vereinnr, name, "beitragsbild"));


		String escapedUeberschrift = ueberschrift.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
		String escapedInhalt = BerichtHelper.convertQuillClassesToInlineStyles(inhalt);
		;

		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{").append("\"title\":\"").append(escapedUeberschrift).append("\",")
				.append("\"content\":\"").append(escapedInhalt).append("\",").append("\"status\":\"publish\",")
				.append("\"date\":\"").append(datum).append("\",").append("\"categories\":").append(catId);

		if (mediaId > 0) {
			if (WordpressBeitragsbildOption.IMMER_STANDARD == bildVariante) {
				int pos = ConfigManager.findePosition(ConfigManager.getWordpressValue(vereinnr, name, "filter"),
						categoryName.get(0));
				if (!"-1".equals(ConfigManager.getWordpressValue(vereinnr, name, "symbol", pos))) {
					jsonBuilder.append(", \"featured_media\":")
							.append(ConfigManager.getWordpressValue(vereinnr, name, "symbol", pos));
				}
			} else {
				jsonBuilder.append(", \"featured_media\":").append(mediaId);
			}
		} else {
			int pos = ConfigManager.findePosition(ConfigManager.getWordpressValue(vereinnr, name, "filter"),
					categoryName.get(0));
			if (!"-1".equals(ConfigManager.getWordpressValue(vereinnr, name, "symbol", pos))) {
				jsonBuilder.append(", \"featured_media\":")
						.append(ConfigManager.getWordpressValue(vereinnr, name, "symbol", pos));
			}
		}
		jsonBuilder.append("}");

		String json = jsonBuilder.toString();

		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).header("Content-Type", "application/json")
				.header("Authorization", "Basic " + encodedAuth).POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 201) {
			JSONObject jsonObj = new JSONObject(sanitizeJsonResponse(response.body()));
			evaluateResponseStatus(response.statusCode(), "createPost");
			return jsonObj.getInt("id");
		} else {
			evaluateResponseStatus(response.statusCode(), "createPost");
			return -1;
		}
	}

	public List<String> getCategoryNames(String vereinnr) throws IOException, InterruptedException, URISyntaxException {
		List<Map.Entry<String, Integer>> eintraege = new ArrayList<>();

		String url = this.domain + "/wp-json/wp/v2/categories?per_page=100";
		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		String body = response.body().trim();

		if (body.startsWith("\uFEFF")) {
			body = body.substring(1);
		}

		try {
			JSONArray categories = new JSONArray(body);

			List<JSONObject> allCategories = new ArrayList<>();
			// Alle Kategorien speichern
			for (int i = 0; i < categories.length(); i++) {
				allCategories.add(categories.getJSONObject(i));
			}

			// Jetzt alle Kategorien durchgehen und auch Unterkategorien hinzufügen
			for (JSONObject category : allCategories) {
				String katname = category.getString("slug");
				int parentId = category.getInt("parent"); // 0 = Oberkategorie
				int pos = ConfigManager.findePosition(ConfigManager.getWordpressValue(vereinnr, name, "filter"),
						katname);
				if (pos > 0) {
					eintraege.add(new AbstractMap.SimpleEntry<>(katname, pos));
				}
			}

			// Sortieren nach der Zahl (also nach dem Integer-Wert)
			eintraege.sort(Comparator.comparing(Map.Entry::getValue));

			List<String> categoryNames = new ArrayList<>();
			// Jetzt nur die Strings extrahieren in sortierter Reihenfolge
			for (Map.Entry<String, Integer> eintrag : eintraege) {
				categoryNames.add(eintrag.getKey());
			}

			return categoryNames;

		} catch (Exception e) {
			// Fehler beim Parsen oder bei fehlendem JSON
			return Collections.singletonList("Keine Kategorie gefunden, oder keine Wordpress-Seite");
		}
	}

	/**
	 * Lädt ein Bild (als BLOB) in die Medienbibliothek hoch und gibt die Medien-ID
	 * zurück.
	 *
	 * @param imageData Das Bild als Byte-Array.
	 * @param filename  Der Dateiname des Bildes (z.B. "bild.jpg").
	 * @param mimeType  Der MIME-Typ des Bildes (z.B. "image/jpeg").
	 * @return Die Medien-ID oder -1 bei Fehler/nicht vorhandenen Bilddaten.
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public WordpressMedia uploadMedia(byte[] imageData, String filename, String mimeType)
			throws URISyntaxException, IOException, InterruptedException {
		if (imageData == null || imageData.length == 0) {
			return null; // Kein Bild vorhanden
		}

		String url = this.domain + "/wp-json/wp/v2/media";
		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).header("Content-Type", mimeType)
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.header("Authorization", "Basic " + encodedAuth).POST(HttpRequest.BodyPublishers.ofByteArray(imageData))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		JSONObject jsonObj = new JSONObject(sanitizeJsonResponse(response.body()));
		evaluateResponseStatus(response.statusCode(), "uploadMedia");

		if (fehler == 0) {
			int mediaId = jsonObj.getInt("id");
			String mediaUrl = jsonObj.getString("source_url");
			return new WordpressMedia(mediaId, mediaUrl);
		} else {
			return null;
		}
	}

	public static String sanitizeJsonResponse(String raw) {
		if (raw == null) {
			return "{}"; // Fallback: leeres JSON
		}

		String cleaned = raw;

		// 1. BOM (Byte Order Mark) am Anfang entfernen
		cleaned = cleaned.replaceAll("^\uFEFF", "");

		// 2. Führende und nachfolgende Whitespaces/Zeilenumbrüche entfernen
		cleaned = cleaned.trim();

		// 3. Manche Server geben HTML-Fehlerseiten zurück – die fangen mit "<" an
		// -> hier einfach durch ein leeres JSON ersetzen
		if (cleaned.startsWith("<")) {
			System.err.println("WARNUNG: JSON-Antwort enthält HTML (wahrscheinlich WP-Fehlerseite).");
			return "{}";
		}

		// 4. Sicherstellen, dass es wirklich mit { oder [ anfängt
		if (!(cleaned.startsWith("{") || cleaned.startsWith("["))) {
			System.err.println("WARNUNG: JSON-Antwort beginnt nicht mit { oder [ -> korrigiert.");
			// Versuch: ersten gültigen Block suchen
			int objIndex = cleaned.indexOf("{");
			int arrIndex = cleaned.indexOf("[");
			int startIndex = -1;

			if (objIndex >= 0 && arrIndex >= 0) {
				startIndex = Math.min(objIndex, arrIndex);
			} else if (objIndex >= 0) {
				startIndex = objIndex;
			} else if (arrIndex >= 0) {
				startIndex = arrIndex;
			}

			if (startIndex >= 0) {
				cleaned = cleaned.substring(startIndex);
			} else {
				return "{}"; // nichts gefunden -> leeres JSON
			}
		}

		return cleaned;
	}

	public WordpressMedia uploadMediaAndInsertIntoPost(byte[] imageData, String filename, String mimeType,
			String altText, String caption) throws URISyntaxException, IOException, InterruptedException {
		WordpressMedia media = uploadMedia(imageData, filename, mimeType);
		if (media == null || media.getMediaId() == -1) {
			return new WordpressMedia(-1, "", "");
		}

		try {
			updateMediaMetadata(media.getMediaId(), filename, caption, altText);

			// Gutenberg Block statt normalem <div>

			String altTextTag = (altText != null && !altText.isEmpty()) ? " alt=\"" + escapeHtml(altText) + "\"" : "";

			String figcaption = (caption != null && !caption.isEmpty())
					? "<figcaption style=\"text-align: center; font-style: italic; font-size: 0.8em;\">"
							+ escapeHtml(caption) + "</figcaption>"
					: "";

			String imageBlock = "<!-- wp:image {\"id\":" + media.getMediaId()
					+ ",\"sizeSlug\":\"large\",\"linkDestination\":\"none\",\"align\":\"center\"} -->"
					+ "<figure class=\"wp-block-image size-large aligncenter\">" + "<img src=\""
					+ escapeHtml(media.getUrl()) + "\"" + altTextTag + "/>" + figcaption + "</figure>"
					+ "<!-- /wp:image -->";

			return new WordpressMedia(media.getMediaId(), media.getUrl(), imageBlock);

		} catch (Exception e) {
			e.printStackTrace();
			return new WordpressMedia(-1, "", "");
		}
	}

	private void updateMediaMetadata(int mediaId, String title, String caption, String altText)
			throws URISyntaxException, IOException, InterruptedException {
		String url = this.domain + "/wp-json/wp/v2/media/" + mediaId;

		JSONObject jsonBody = new JSONObject();
		jsonBody.put("title", title);
		jsonBody.put("caption", caption);
		jsonBody.put("description", caption);
		jsonBody.put("alt_text", altText);

		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).header("Content-Type", "application/json")
				.header("Authorization", "Basic " + encodedAuth)
				.method("POST", HttpRequest.BodyPublishers.ofString(jsonBody.toString())).build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		evaluateResponseStatus(response.statusCode(), "updateMediaMetadata");
	}

	private String getMediaUrl(int mediaId) throws Exception {
		String url = this.domain + "/wp-json/wp/v2/media/" + mediaId;
		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).header("Authorization", "Basic " + encodedAuth)
				.GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		JSONObject jsonObj = new JSONObject(sanitizeJsonResponse(response.body()));
		return jsonObj.getString("source_url");
	}

	public List<WpComment> fetchAllComments() throws Exception {
		List<WpComment> allComments = new ArrayList<>();
		allComments.addAll(fetchCommentsByStatus("any"));
		return allComments;
	}

	public List<WpComment> fetchCommentsByStatus(String status) throws Exception {
		HttpClient client = HttpClient.newHttpClient();

		String url = domain + "/wp-json/wp/v2/comments?per_page=100&orderby=date&order=desc&status=" + status;
		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).header("Authorization", "Basic " + encodedAuth)
				.GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		evaluateResponseStatus(response.statusCode(), "fetchCommentsByStatus " + status);
		if (fehler != 0) {
			return null;
		}

		// JSON parsen
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(response.body());
		List<WpComment> comments = new ArrayList<>();

		for (JsonNode node : root) {
			WpComment comment = new WpComment();
			comment.setId(node.get("id").asInt());
			comment.setDate(node.get("date").asText());
			comment.setPost(node.get("post").asInt());
			comment.setAuthorName(node.get("author_name").asText());
			comment.setContent(node.get("content").get("rendered").asText());
			comment.setStatus(node.get("status").asText());
			comment.setPostId(node.get("post").asLong());
			comments.add(comment);
		}

		return comments;
	}

	public void trashComment(WpComment comment) throws Exception {
		updateCommentStatus(comment.getId(), "trash");
	}

	public void approveComment(WpComment comment) throws Exception {
		updateCommentStatus(comment.getId(), "approve");
	}

	public void markCommentAsSpam(WpComment comment) throws Exception {
		updateCommentStatus(comment.getId(), "spam");
	}

	private void updateCommentStatus(int commentId, String newStatus) throws Exception {
		String url = domain + "/wp-json/wp/v2/comments/" + commentId;

		// JSON-Payload mit neuem Status
		String jsonPayload = "{\"status\": \"" + newStatus + "\"}";

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.header("Authorization", "Basic " + encodedAuth).header("Content-Type", "application/json")
				.method("PUT", HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		evaluateResponseStatus(response.statusCode(), "updateCommentStatus");
	}

	private void evaluateResponseStatus(int statusCode, String methode) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		fehler = -1;
		switch (statusCode) {
		case -1:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Homepage, Userdaten oder Passwort fehlt " + methode, null));
			break;
		case 200:
			ctx.addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Kommentare erfolgreich geladen." + methode, null));
			fehler = 0;
			break;
		case 201:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Es wurde ein Blog-Eintrag auf " + this.domain + " erfolgreich erstellt" + methode, null));
			fehler = 0;
			break;

		case 204:
			ctx.addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Keine Kommentare gefunden." + methode, null));
			break;

		case 400:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Ungültige Anfrage. Bitte überprüfen Sie Ihre Eingaben." + methode, null));
			break;

		case 401:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Nicht autorisiert. Bitte überprüfen Sie Benutzernamen und Passwort." + methode, null));
			break;

		case 403:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Zugriff verweigert. Sie haben keine Berechtigung." + methode, null));
			break;

		case 404:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Dienst nicht gefunden. Bitte prüfen Sie die API-URL." + methode, null));
			break;

		case 408:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
					"Zeitüberschreitung beim Verbinden mit dem Server." + methode, null));
			break;

		case 500:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL,
					"Interner Serverfehler. Bitte versuchen Sie es später erneut." + methode, null));
			break;

		case 502:
		case 503:
		case 504:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL,
					"Der Server ist derzeit nicht erreichbar. Bitte versuchen Sie es später erneut." + methode, null));
			break;

		default:
			ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Unbekannter Fehler beim Laden der Kommentare (Code: " + statusCode + ")" + methode, null));
			break;
		}

	}

	public int getFehler() {
		return fehler;
	}

	public void setFehler(int fehler) {
		this.fehler = fehler;
	}

	/**
	 * Einfaches HTML-escaping (sehr leichtgewichtig, für Alt-Text/Caption/URL)
	 */
	private String escapeHtml(String input) {
		if (input == null) {
			return "";
		}
		String result = input;
		result = result.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		result = result.replace("\"", "&quot;");
		result = result.replace("'", "&#x27;");
		return result;
	}
}
