package de.bericht.controller;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.provider.SpielergebnisFactory;
import de.bericht.provider.SpielergebnisProvider;
import de.bericht.service.DatabaseService;
import de.bericht.service.EmailService;
import de.bericht.service.LogEntry;
import de.bericht.service.TelegrammService;
import de.bericht.util.ApiKIChatGPT;
import de.bericht.util.BerichtData;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.ErgebnisCache;
import de.bericht.util.Fehler;
import de.bericht.util.IgnorierteWoerte;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.SpielDetail;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

@Named
@ViewScoped
public class BerichtBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private String ueberschrift;
	private String heim;
	private String gast;
	private String datum;
	private String ergebnis;
	private String berichtText;
	private String vereinnr;
	private String ergebnisLink;
	private String name = "";
	private String liga;
	private boolean pruefKI;
	private boolean spielplan = true;
	private String spielErgebnis; // Für die Anzeige der Spielergebnisse
	private String kiRueckgabe = "";
	private String uuid;
	private List<String> kiSaetze = new ArrayList<>();
	private List<LogEntry> logEntries;
	private NamensSpeicher namensSpeicher = new NamensSpeicher();
	private boolean email = false;
	List<? extends SpielDetail> spiele;
	private String gruppeUrl;

	// Für den Bild-Upload: Wir verwenden den Part, den der Container liefert.
	private Part uploadedBild;
	// Hier wird das clientseitig verkleinerte Bild (Base64-String) gespeichert.
	private String resizedImage;
	// Hier speichern wir den Bild-Link, der aus der DB (bzw. nach dem Speichern)
	// kommt.
	private String bildLink;
	// Hier speichern wir das aktuell in der DB vorhandene Bild (als Byte[]), um es
	// als Fallback zu verwenden.
	private byte[] bildDaten;
	private String bildUnterschrift;
	private List<Fehler> fehlerListe = new ArrayList<>();

	private DatabaseService dbService = new DatabaseService();
	IgnorierteWoerte ignorieren;

	public BerichtBean() {
	}

	@PostConstruct
	public void init() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		this.vereinnr = params.get("vereinnr");

		ignorieren = new IgnorierteWoerte();
		String woerterIgnorieren = ConfigManager.getConfigValue(vereinnr, "bericht.pruefung.ok");
		// Aufteilen in ein Array
		String[] woerterArray = woerterIgnorieren.split(",");

		// Durchlaufen mit einer for-each-Schleife
		for (String wort : woerterArray) {
			ignorieren.hinzufuegen(wort);
		}

		try {
			if (params.get("heim") != null) {
				this.heim = URLDecoder.decode(params.get("heim"), "UTF-8");
				ignorieren.hinzufuegen(this.heim);
			}
			if (params.get("gast") != null) {
				this.gast = URLDecoder.decode(params.get("gast"), "UTF-8");
				ignorieren.hinzufuegen(this.gast);
			}
		} catch (UnsupportedEncodingException e) {
			this.heim = "Unbekannt";
			this.gast = "";
		}
		this.datum = params.get("datum");
		this.ergebnis = params.get("ergebnis");
		this.ergebnisLink = params.get("ergebnisLink");
		this.name = params.get("name");
		this.liga = params.get("liga");
		this.uuid = params.get("uuid");
		this.gruppeUrl = params.get("gruppeUrl");

		if (vereinnr == null) {
			vereinnr = "13014";
		}

		if (dbService.anzahlFreigabe(vereinnr, ergebnisLink) >= 1) {
			try {

				FacesContext.getCurrentInstance().getExternalContext().redirect(getGenerateBerichtUrlFreigegeben());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		// Bericht und Bild aus DB laden, falls vorhanden
		if (ergebnisLink != null && !ergebnisLink.isEmpty()) {
			BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);
			this.bildUnterschrift = data.getBildUnterschrift() != null ? data.getBildUnterschrift() : "";
			if (isHttpLink()) {
				this.ueberschrift = datum + " - " + heim + " - " + gast + "   " + ergebnis + " (" + liga + ")";
			} else {
				this.ueberschrift = data.getUeberschrift() != null ? data.getUeberschrift() : heim;
			}
			this.berichtText = data.getBerichtText() != null ? data.getBerichtText() : "";
			if (data.getBild() != null) {
				// Speichere das geladene Bild in der Hilfseigenschaft bildDaten
				this.bildDaten = data.getBild();
				// Erzeuge einen Data-URI-String zur Anzeige
				this.bildLink = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bildDaten);
			}
		} else {
			this.berichtText = "";
			this.bildUnterschrift = "";
		}

		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {

			this.spielErgebnis = dbService.loadSpielstatistik(vereinnr, ergebnisLink);

			if ("Keine Spielstatistik vorhanden".equals(this.spielErgebnis)) {
				spielHtmlLesen();
			}

		}
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
		}
		dbService.verarbeiteEintrag(vereinnr, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
		if (dbService.eMailVersand(vereinnr, ergebnisLink) > 0) {
			email = true;
		}

		pruefKI = dbService.isKI(vereinnr, ergebnisLink);

	}

	public String speichernUndWeiter() {
		speichern();
		return "aenderung.xhtml?faces-redirect=true";
	}

	// Diese Methode wird durch den "Speichern"-Button aufgerufen.
	public void speichern() {
		byte[] bildBytes = null;
		// Falls ein clientseitig verkleinertes Bild vorliegt (Base64 in resizedImage),
		// verwende dieses:
		if (resizedImage != null && !resizedImage.isEmpty()) {
			try {
				String base64Data = resizedImage.split(",")[1]; // Entfernt den Data-URI-Präfix
				bildBytes = Base64.getDecoder().decode(base64Data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Falls kein verkleinertes Bild vorliegt, aber ein neues Bild hochgeladen
		// wurde:
		else if (uploadedBild != null && uploadedBild.getSize() > 0) {
			try (InputStream input = uploadedBild.getInputStream();
					ByteArrayOutputStream output = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = input.read(buffer)) != -1) {
					output.write(buffer, 0, bytesRead);
				}
				bildBytes = output.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Falls weder resizedImage noch uploadedBild vorhanden sind, verwende das
		// bereits in der DB vorhandene Bild.
		else if (bildDaten != null && bildDaten.length > 0) {
			bildBytes = bildDaten;
		}

		berichtText = berichtText.replaceAll("<a\\s+target=\"_blank\"\\s*>(https?://[^<\\s]+)</a>",
				"<a href=\"$1\" target=\"_blank\">$1</a>");

		// Speichere in der DB: Berichtstext, ergebnisLink und das Bild (als Byte[])
		dbService.saveBerichtData(vereinnr, ergebnisLink, berichtText, bildBytes, bildUnterschrift, ueberschrift);
		dbService.saveLogData(vereinnr, ergebnisLink, name, "Speichern", "");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);

		// Nach dem Speichern neu laden:
		BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);
		this.berichtText = data.getBerichtText();
		this.bildUnterschrift = data.getBildUnterschrift();
		if (data.getBild() != null) {
			// Aktualisiere bildDaten und erzeugt den Data-URI-String
			this.bildDaten = data.getBild();
			this.bildLink = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(data.getBild());
		} else {
			this.bildDaten = null;
			this.bildLink = null;
		}

		// Optional: uploadedBild und resizedImage zurücksetzen, damit sie beim nächsten
		// Speichern nicht erneut verwendet werden.
		this.uploadedBild = null;
		this.resizedImage = null;
	}

	// Beispiel: E-Mail senden-Methode (bleibt unverändert)
	public void emailSenden() {

		String subject = heim + " - " + gast + "\t" + ergebnis;
		String bildname = datum + "_" + heim + "_" + gast.replace(" ", "_").replace(".", "-") + ".jpg";
		if (spielErgebnis == null) {
			spielErgebnis = "";
		}

		String body = "Bericht über diesen Link korrigieren und Freigeben: " + getGenerateBerichtUrl() + "<br><br>"
				+ subject + "<br>" + berichtText + "<br>" + spielErgebnis + "<br><br> Mit freundlichen Grüßen<br>"
				+ name + "<br><br>";

		byte[] attachment = null;
		if (bildBytesAvailable()) {
			attachment = getBildBytes();
			body = body + "Bildunterschrift:\n" + bildUnterschrift;
		}
		EmailService emailService = new EmailService(vereinnr, name);
		try {
			emailService.sendEmail(vereinnr, subject, body, attachment, bildname);
			if (emailService.getCcEmpfaenger() == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Erfolg", "E-Mail wurde erfolgreich versendet! \nTo:" + emailService.getRecipients()));
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
								"E-Mail wurde erfolgreich versendet! \nTo:" + emailService.getRecipients() + "\n CC:"
										+ emailService.getCcEmpfaenger()));
			}
			email = true;
			dbService.saveLogData(vereinnr, ergebnisLink, name,
					"Email: " + emailService.getRecipients() + " " + emailService.getCcEmpfaenger(), "J");
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
		} catch (Exception e) {
			dbService.saveLogData(vereinnr, ergebnisLink, name,
					"Email: " + emailService.getRecipients() + " " + emailService.getCcEmpfaenger(), "N");
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"E-Mail konnte nicht versendet werden: " + e.getMessage()));
			e.printStackTrace();
		}
		String telegramText = "Bericht über diesen Link korrigieren und Freigeben: " + getGenerateBerichtUrlLink()
				+ "\n\n\n" + subject + "\n\n" + " Mit freundlichen Grüßen\n" + name + "\n\n";
		TelegrammService telegramm = new TelegrammService();
		try {
			telegramm.sendTelegramm(vereinnr, telegramText, attachment, bildUnterschrift);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
					"Telegramm wurde erfolgreich versendet! \nTo:"));
			dbService.saveLogData(vereinnr, ergebnisLink, name, "Telegramm", "J");
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
		} catch (Exception e) {
			dbService.saveLogData(vereinnr, ergebnisLink, name, "Telegramm", "N");
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Telegramm konnte nicht versendet werden: " + e.getMessage()));
			e.printStackTrace();
		}

	}

	private boolean bildBytesAvailable() {
		return (resizedImage != null && !resizedImage.isEmpty()) || (bildDaten != null && bildDaten.length > 0);
	}

	private byte[] getBildBytes() {
		if (resizedImage != null && !resizedImage.isEmpty()) {
			try {
				String base64Data = resizedImage.split(",")[1];
				return Base64.getDecoder().decode(base64Data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (bildDaten != null && bildDaten.length > 0) {
			return bildDaten;
		}
		return null;
	}

	// Bild löschen-Methode
	public void bildLoeschen() {
		dbService.saveBerichtData(vereinnr, ergebnisLink, berichtText, null, null, heim);
		this.bildLink = null;
		this.bildDaten = null;
		dbService.saveLogData(vereinnr, ergebnisLink, name, "Bild gelöscht", "");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
	}

	public List<LogEntry> getLogEntries() {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return new ArrayList<>();
		}
		// Lese die Log-Einträge nur einmal
		if (logEntries == null) {
			logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
		}
		return logEntries;
	}

	public void spielplanAnzeige() throws IOException {
		if (spielplan) {
			spielplan = false;
		} else {
			spielplan = true;
		}
	}

	public void verbessernKI() throws IOException {
		spielplan = false;
		fehlerListe.clear(); // Alte Fehler entfernen
		String text;
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
			namensSpeicher.fuelleNamensspeicher(vereinnr, this.ergebnisLink, namensSpeicher);
			String anonym = namensSpeicher.anonymisiereText(berichtText);
			text = decodeUrl(anonym);
		} else {
			text = ueberschrift + "\n\n" + berichtText;
		}
		StringBuilder frage = new StringBuilder();
		frage.append(ConfigManager.getConfigValue(vereinnr, "bericht.kikorrektur.prompt"));
		if (!kiSaetze.isEmpty()) {
			frage.append(
					" Der vorliegende Zeitungsbericht wurde bereits geprüft. Gib daher nur noch wirkliche Fehler aus und extem schlechte Formulierungen. ");
		}
		frage.append("Ab hier beginnt der Zeitungsbericht: ");
		frage.append(text);

		int anzahlKi = dbService.anzahlKI(vereinnr, ergebnisLink, "korrigiert");
		if (anzahlKi <= 5 && !text.isEmpty()) {
			// temperatur, frequencyPenalty , presencePenalty für Rechtschreibprüfung sind
			// optimalerweise alle 0

			JSONObject schema = new JSONObject().put("type", "object")
					.put("properties",
							new JSONObject().put("Korrekturen",
									new JSONObject().put("type", "array").put("items",
											new JSONObject()
													.put("type",
															"object")
													.put("properties", new JSONObject()
															.put("Falsch", new JSONObject().put("type", "string"))
															.put("Begründung der Änderung",
																	new JSONObject().put("type", "string"))
															.put("Korrekturvorschlag",
																	new JSONObject().put("type", "string")))
													.put("required",
															new JSONArray().put("Falsch").put("Begründung der Änderung")
																	.put("Korrekturvorschlag")))))
					.put("required", new JSONArray().put("Korrekturen"));
			ApiKIChatGPT ki = new ApiKIChatGPT(vereinnr, frage.toString(),
					ConfigManager.getConfigValue(vereinnr, "bericht.kikorrektur.model"), "none", 0.0, 0.0, 0.0, schema);

			kiRueckgabe = "<strong>Frage:</strong> <br> " + frage + "<br><br><strong>Antwort:</strong><br>"
					+ ki.getResponse();
			fehlerListe = parseFehlerListe(ki.getResponse(), kiSaetze);
			dbService.saveLogData(vereinnr, ergebnisLink, "KI", "KI-Bericht korrigiert", "");
		} else {
			Fehler fehler = new Fehler("", "", "Keine Prüfung mehr möglich", false);
			fehlerListe.add(fehler);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Zu viele KI-Aufrufe"));
		}
		pruefKI = dbService.isKI(vereinnr, ergebnisLink);
	}

	public List<Fehler> parseFehlerListe(String berichtTextOrg, List<String> kiSaetze) {

		List<Fehler> fehlerListe = new ArrayList<>();

		try {
			// 1) JSON extrahieren
			String cleaned = berichtTextOrg.trim().replaceAll("```[a-zA-Z0-9]*", "").replaceAll("```", "").trim();

			// JSON-Anteil extrahieren (Object ODER Array)
			cleaned = extractJson(cleaned); // Falls du dieselbe extractJson()-Methode nutzt wie oben
			if (cleaned == null || cleaned.isEmpty()) {
				throw new IllegalArgumentException("Kein JSON gefunden");
			}

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(cleaned);

			JsonNode arrayNode = null;

			// ---------- FALL 1: GPT liefert direkt ein Array ----------
			if (root.isArray()) {
				arrayNode = root;
			}

			// ---------- FALL 2: GPT liefert Objekt mit "Korrekturen" ----------
			else if (root.isObject() && root.has("Korrekturen") && root.get("Korrekturen").isArray()) {
				arrayNode = root.get("Korrekturen");
			}

			// ---------- FALL 3: Ungültiges Format ----------
			if (arrayNode == null) {
				throw new IllegalArgumentException("Weder Array noch Objekt mit 'Korrekturen' gefunden.");
			}

			// ----------- Einträge verarbeiten -----------
			for (JsonNode node : arrayNode) {

				String falsch, begruendung, korrektur;

				if (namensSpeicher != null) {
					falsch = namensSpeicher.rueckuebersetzen(node.get("Falsch").asText());
					begruendung = namensSpeicher.rueckuebersetzen(node.get("Begründung der Änderung").asText());
					korrektur = namensSpeicher.rueckuebersetzen(node.get("Korrekturvorschlag").asText());
				} else {
					falsch = node.get("Falsch").asText();
					begruendung = node.get("Begründung der Änderung").asText();
					korrektur = node.get("Korrekturvorschlag").asText();
				}

				boolean button = falsch != null && !falsch.isEmpty();

				// Doppelungen vermeiden
				if (!(falsch.trim().length() > 0 && kiSaetze.contains(falsch))) {
					if (falsch.trim().length() > 0) {
						kiSaetze.add(korrektur);
					}
					Fehler fehler = new Fehler(falsch, korrektur, begruendung, button);
					fehlerListe.add(fehler);
				}
			}

		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Kein gültiger JSON-Array oder JSON-Objekt im Text gefunden."));

			// Fallback „alles korrekt“
			Fehler fallback = new Fehler("", "", "Text ist OK - Super gemacht", false);
			fehlerListe.add(fallback);
		}

		return fehlerListe;
	}

	private String extractJson(String input) {
		int startObj = input.indexOf('{');
		int startArr = input.indexOf('[');

		int start = -1;
		if (startObj >= 0 && startArr >= 0) {
			start = Math.min(startObj, startArr);
		} else if (startObj >= 0) {
			start = startObj;
		} else if (startArr >= 0) {
			start = startArr;
		}

		if (start < 0) {
			return null; // Kein JSON
		}

		return input.substring(start).trim();
	}

	public void pruefeRechtschreibung() {

		if ((spiele == null || spiele.isEmpty()) && isHttpLink()) {
			spielHtmlLesen();
		}

		spielplan = false;
		fehlerListe.clear(); // Alte Fehler entfernen
		String text = "";
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
			text = stripHtmlWithJsoup(decodeUrl(berichtText)); // Annahme: Der eingegebene Berichtstext
		} else {
			text = stripHtmlWithJsoup(decodeUrl(ueberschrift.trim() + ". " + berichtText.trim())); // Annahme: Der
																									// eingegebene
																									// Berichtstext
		}

		try {
			String apiUrl = "https://api.languagetool.org/v2/check";
			String params = "text=" + URLEncoder.encode(text, "UTF-8") + "&language=de-DE" + "&level=picky"
					+ "&enabledCategories=grammar,style,typography"
					+ "&enabledRules=GERMAN_COMMA_RULE,COMMA_PARENTHESIS_WHITESPACE,WHITESPACE_RULE";

			// HTTP Request an LanguageTool API senden
			HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setDoOutput(true);

			try (OutputStream os = conn.getOutputStream()) {
				os.write(params.getBytes(StandardCharsets.UTF_8));
			}

			// Antwort auslesen
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			String response = reader.lines().collect(Collectors.joining());
			reader.close();

			// JSON verarbeiten
			JSONObject responseJson = new JSONObject(response);
			JSONArray matches = responseJson.getJSONArray("matches");

			for (int i = 0; i < matches.length(); i++) {
				JSONObject match = matches.getJSONObject(i);
				String nachricht = match.getString("message");
				Boolean wortIgnorieren = false;
				int offset = 0;
				int length = 0;

				JSONObject context = match.getJSONObject("context");
				offset = context.getInt("offset"); // Hole das "offset"-Feld
				length = context.getInt("length"); // Hole das "offset"-Feld
				if ("Möglicher Tippfehler gefunden.".equals(nachricht)) {
					String textfehler = context.getString("text"); // Hole das "offset"-Feld
					wortIgnorieren = ignorieren.wortIgnorieren(textfehler.substring(offset, offset + length));
				}

				if (!wortIgnorieren) {
					// Alternativen auslesen
					JSONArray replacements = match.getJSONArray("replacements");
					StringBuilder vorschlaege = new StringBuilder();
					for (int j = 0; j < Math.min(replacements.length(), 5); j++) { // Max. 5 Vorschläge
						if (j > 0) {
							vorschlaege.append(", ");
						}
						vorschlaege.append(replacements.getJSONObject(j).getString("value"));
					}

					// Kontext extrahieren
					JSONObject contextObj = match.getJSONObject("context");
					String kontext = contextObj.getString("text");

					// Fehler zur Liste hinzufügen
					fehlerListe.add(new Fehler(nachricht, vorschlaege.toString(), markiereText(kontext, offset, length),
							false));
				}
			}
			if (fehlerListe.isEmpty()) {
				fehlerListe.add(new Fehler("Keine Fehler gefunden", "Super gemacht", "", false));
				dbService.saveLogData(vereinnr, ergebnisLink, name, "Rechtschreibprüfung ohne Fehler", "");
				logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
			} else {
				dbService.saveLogData(vereinnr, ergebnisLink, name, "Rechtschreibprüfung mit Fehler", "");
				logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
			}

		} catch (Exception e) {
			fehlerListe.clear(); // Alte Fehler entfernen
		}
	}

	public void spielHtmlLesen() {
		SpielergebnisProvider provider = SpielergebnisFactory.create(vereinnr, ergebnisLink, namensSpeicher, false);
		spiele = provider.getSummary().getSpiele();

		this.spielErgebnis = provider.getSpielErgebnis();
		dbService.saveOrUpdateSpielstatistik(vereinnr, ergebnisLink, this.spielErgebnis);

		for (SpielDetail spiel : spiele) {
			String spieler = spiel.getGast() + " " + spiel.getHeim();
			// Alle Sonderzeichen durch Leerzeichen ersetzen
			spieler = spieler.replaceAll("[^A-Za-zÄÖÜäöüß]", " ");

			// Mehrfache Leerzeichen entfernen und in Array umwandeln
			String[] namen = spieler.trim().split("\\s+");

			// Ausgabe (optional)
			for (String name : namen) {
				ignorieren.hinzufuegen(name);
			}

		}
	}

	public String stripHtmlWithJsoup(String html) {
		if (html == null) {
			return null;
		}

		// HTML parsen und reinen Text extrahieren
		String textOnly = Jsoup.parse(html).text();

		return textOnly;
	}

	public static String markiereText(String kontext, int offset, int length) {
		if (offset < 0 || length < 0 || offset + length > kontext.length()) {
			return kontext; // Fehlerbehandlung
		}

		String teil1 = kontext.substring(0, offset);
		String teil2 = kontext.substring(offset, offset + length);
		String teil3 = kontext.substring(offset + length);

		return teil1 + "<span style='color: #ffffff;background-color:red;' >" + teil2 + "</span>" + teil3;
	}

	public String navigateToBerichtKI() {
		return "berichtKI.xhtml?faces-redirect=true&ergebnisLink=" + ergebnisLink;
	}

	// GETTER & SETTER
	public List<Fehler> getFehlerListe() {
		return fehlerListe;
	}

	public void korrektur(String falsch, String korrekt) {
		String altBericht = berichtText;
		String altUeberschrift = ueberschrift;

		berichtText = berichtText.replaceFirst(falsch, korrekt);

		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
		} else {
			ueberschrift = ueberschrift.replaceFirst(falsch, korrekt);
		}

		if (altBericht.equals(berichtText) && altUeberschrift.equals(ueberschrift)) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Fehler", "Bericht wurde nicht geändert"));
		} else {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Korrektur", "Bericht wurde korrigiert"));
			fehlerListe.removeIf(f -> falsch.equals(f.getFehlerhaftesWort()));
		}

		return;
	}

	// Gibt den Bild-Link zurück, damit h:graphicImage das Bild anzeigen kann.
	public String getBildUrl() {
		return bildLink;
	}

	// Gibt den Spielergebnis-Text zurück (mit <br/> statt \n)
	public String getSpielErgebnis() {
		return spielErgebnis != null ? spielErgebnis.replace("\n", "<br>") : "";
	}

	// Getter und Setter
	public String getHeim() {

		return decodeUrl(heim);
	}

	public String getGast() {
		return decodeUrl(gast);
	}

	public String getDatum() {
		return datum;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public String getBerichtText() {
		return berichtText;
	}

	public void setBerichtText(String berichtText) {
		this.berichtText = decodeUrl(berichtText);

	}

	public void setName(String name) {
		this.name = name;
	}

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public String getName() {
		return name;
	}

	public Part getUploadedBild() {
		return uploadedBild;
	}

	public void setUploadedBild(Part uploadedBild) {
		this.uploadedBild = uploadedBild;
		dbService.saveLogData(vereinnr, ergebnisLink, name, "Bild ausgewählt", "");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
	}

	public String getResizedImage() {
		return resizedImage;
	}

	public void setResizedImage(String resizedImage) {
		this.resizedImage = resizedImage;
	}

	public String decodeUrl(String url) {
		if (url == null) {
			return null;
		}

		if (enthaeltUmlaute(url)) {
			return url;

		}
		String enc = new String(url.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		return enc;
	}

//	public static boolean enthaeltUmlaute(String text) {
//		
//		return text.matches(".*[äöüÄÖÜß].*");
//	}

	public static boolean enthaeltUmlaute(String text) {
		String umlaute = "äöüÄÖÜß";
		for (int i = 0; i < text.length(); i++) {
			if (umlaute.indexOf(text.charAt(i)) != -1) {
				return true;
			}
		}
		return false; // Kein Umlaut gefunden
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public boolean getInBearbeitung() {
		return dbService.inBearbeitung(vereinnr, ergebnisLink, uuid);
	}

	public void updBearbeitung() {
		dbService.verarbeiteEintrag(vereinnr, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
	}

	// Diese Methode wird durch den "Speichern"-Button aufgerufen.
	public void zurueck() {
		dbService.deleteUUID(vereinnr, ergebnisLink, uuid);
	}

	// Diese Methode wird durch den "Speichern"-Button aufgerufen.
	public boolean isAnzFreigabe() {
		int pos = ConfigManager.findePosition(ConfigManager.getConfigValue(vereinnr, "bericht.freigabe"), name);
		if (pos > 0) {
			return true;
		}
		return false;
	}

	// Bild löschen-Methode
	public void freigabe() {
		dbService.saveLogData(vereinnr, ergebnisLink, name, "Freigegeben", "");
		logEntries = dbService.getLogEntries(vereinnr, ergebnisLink);
		ErgebnisCache.setze(vereinnr, "Freigabe", dbService, ergebnisLink, "");
	}

	public String getBildUnterschrift() {
		return bildUnterschrift;
	}

	public void setBildUnterschrift(String bildUnterschrift) {
		this.bildUnterschrift = bildUnterschrift;
	}

	public boolean isSpielplan() {
		return spielplan;
	}

	public void setSpielplan(boolean spielplan) {
		this.spielplan = spielplan;
	}

	public String getKiRueckgabe() {
		return kiRueckgabe;
	}

	public void setKiRueckgabe(String kiRueckgabe) {
		this.kiRueckgabe = kiRueckgabe;
	}

	public String getGenerateBerichtUrl() {
		return "<a href=\"" + getGenerateBerichtUrlLink() + "\">Bericht ändern oder freigeben</a>";
	}

	public String getGenerateBerichtUrlFreigegeben() {
		String targetPage = BerichtHelper.getProgrammUrl(vereinnr) + "zusammen.xhtml" + "?heim=" + encodeUrl(heim)
				+ "&gast=" + encodeUrl(gast) + "&datum=" + encodeUrl(datum) + "&ergebnis=" + encodeUrl(ergebnis)
				+ "&ergebnisLink=" + encodeUrl(ergebnisLink) + "&liga=" + encodeUrl(liga) + "&vereinnr=" + vereinnr
				+ "&mail=Ja";

		return targetPage;
	}

	public String getGenerateBerichtUrlLink() {
		String baseUrl = BerichtHelper.getProgrammUrl(vereinnr);

		String targetPage = baseUrl + "l/"
				+ dbService.createShortLink("bericht.xhtml" + "?heim=" + encodeUrl(heim) + "&gast=" + encodeUrl(gast)
						+ "&datum=" + encodeUrl(datum) + "&ergebnis=" + encodeUrl(ergebnis) + "&ergebnisLink="
						+ encodeUrl(ergebnisLink) + "&liga=" + encodeUrl(liga) + "&vereinnr=" + encodeUrl(vereinnr));

		return targetPage;
	}

	public static String encodeUrl(String rawUrl) {
		try {
			return URLEncoder.encode(rawUrl, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			// Dies sollte mit StandardCharsets.UTF_8 nicht passieren, da UTF-8 immer
			// unterstützt wird.
			System.err.println("Fehler beim URL-Kodieren: " + e.getMessage());
			return rawUrl; // Fallback: unkodierte URL zurückgeben (potenziell problematisch)
		}
	}

	public List<String> getKiSaetze() {
		return kiSaetze;
	}

	public void setKiSaetze(List<String> kiSaetze) {
		this.kiSaetze = kiSaetze;
	}

	public boolean isHistorienTimestamps() {
		List<String> historienTimestamps = dbService.getHistorieTimestamps(vereinnr, this.ergebnisLink);
		if (historienTimestamps.isEmpty()) {
			return false;
		}
		return true;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getUeberschrift() {
		return ueberschrift;
	}

	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = decodeUrl(ueberschrift);
	}

	public String getToEmpfaenger() {
		if (name == null) {
			return "";
		}
		EmailService em = new EmailService(vereinnr, name);
		return "- " + BerichtHelper.SAFE_HTML_POLICY.sanitize(em.getRecipients().replace(",", "<br /> - "));
	}

	public String getCcEmpfaenger() {
		if (name == null) {
			return "";
		}
		EmailService em = new EmailService(vereinnr, name);
		if (em.getCcEmpfaenger() == null) {
			return "";
		}
		return "- " + BerichtHelper.SAFE_HTML_POLICY.sanitize(em.getCcEmpfaenger().replace(",", "<br /> - "));
	}

	public boolean isEmail() {
		return email;
	}

	public boolean isKi() {
		return pruefKI;
	}

	public void pruefeEmailStatus(ActionEvent event) {
		// Diese Methode wird nur aufgerufen, um den aktuellen Status zu prüfen
		// (damit JS weiß, welchen Dialog es anzeigen soll)
	}

	public boolean isHttpLink() {
		return ergebnisLink != null && ergebnisLink.startsWith("http");
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public String zurueckAction() {
		return isTennis() ? "spielplan.xhtml" : "spielplan.xhtml";
	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

}
