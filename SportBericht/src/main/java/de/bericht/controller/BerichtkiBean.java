package de.bericht.controller;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.provider.SpielergebnisFactory;
import de.bericht.provider.SpielergebnisProvider;
import de.bericht.service.Bilanz;
import de.bericht.service.BilanzService;
import de.bericht.service.DatabaseService;
import de.bericht.service.KiZusammenfassenText;
import de.bericht.service.Mannschaft;
import de.bericht.service.Spiel;
import de.bericht.service.SpielplanService;
import de.bericht.service.Tabelle;
import de.bericht.service.TabelleService;
import de.bericht.util.ApiKIChatGPT;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.OpenAIModelFetcher;
import de.bericht.util.SpielMapped;
import de.bericht.util.Spielbericht;
import de.bericht.util.StilGenerator;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class BerichtkiBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String heim;
	private String gast;
	private String datum;
	private String ergebnis;
	private String ergebnisLink;
	private List<Spielbericht> berichtText = new ArrayList<>();
	private List<KiZusammenfassenText> zusammenfassenTexte = new ArrayList<>();
	private String besondereVorkommnisse;
	private int anzahlKi;
	private String selectedModel;
	private String selectedThinking = "medium";
	private double temperatur = 0.5;
	private double frequencyPenalty = 0.5;
	private double presencePenalty = 0.5;
	private boolean spielplan = false;
	private String liga;
	private String uuid;
	private String ersetzungen;
	private List<String> wirkungen = new ArrayList<>();
	private List<String> berichte = new ArrayList<>();
	private String kiRueckgabe;
	private String berichtMannschaft;
	private String freierText = "Wert spielt keine Rolle";
	private int berichtIndex;
	private Mannschaft selectedMannschaft;

	private String frage;
	private String frageAusgabe;
	private String prettyJson;
	private String prettyJsonTabelle;
	private String prettyJsonSpielplan;
	private String prettyJsonBilanz;

	private String freiPrompt;
	private String zusammenfassenPrompt;
	private String vereinnr;
	private String freiMannschaft;
	private String gruppeUrl;
	private String tabelleUrl;
	private String bilanzUrl;
	boolean istHeim;
	private boolean panelCollapsed = true; // Standard: zugeklappt
	private boolean zusaetzlicheJSON = false;
	private boolean mehrereBerichte = false;
	private NamensSpeicher namensSpeicher = new NamensSpeicher();

	private int berichtgroesse = 150; // Standardwert
	private DatabaseService dbService = new DatabaseService();
	OpenAIModelFetcher modelle;

	public BerichtkiBean() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		this.vereinnr = params.get("vereinnr");
		this.heim = params.get("heim");
		this.gast = params.get("gast");
		this.datum = params.get("datum");
		this.ergebnis = params.get("ergebnis");
		this.ergebnisLink = params.get("ergebnisLink");
		this.besondereVorkommnisse = params.get("besondereVorkommnisse");
		if (params.get("berichtText") != null && !params.get("berichtText").isEmpty()) {
			Spielbericht sb = new Spielbericht();
			sb.setText(params.get("berichtText"));
			sb.setStilversion("");
			sb.setVariante("");
			berichtText.add(sb);
		}
		this.anzahlKi = dbService.anzahlKI(vereinnr, ergebnisLink, "generiert");
		this.liga = params.get("liga");
		this.uuid = params.get("uuid");
		modelle = new OpenAIModelFetcher(ConfigManager.getChatGptPasswort(vereinnr));
		selectedModel = ConfigManager.getConfigValue(vereinnr, "bericht.ki.model");
		if (!isHttpLink()) {
			this.berichtMannschaft = freierText;
		} else if (heim.contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
			this.berichtMannschaft = heim;
			this.istHeim = true;
		} else if (gast.contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
			this.berichtMannschaft = gast;
			this.istHeim = false;
		} else if (gast.contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Ort"))) {
			this.berichtMannschaft = gast;
			this.istHeim = false;
		} else if (heim.contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Ort"))) {
			this.berichtMannschaft = heim;
			this.istHeim = true;
		} else {
			this.berichtMannschaft = null;
		}
		wirkungen.add("Ideal für Zeitungen, wenig emotional, aber fundiert");

		if (isHttpLink()) {
			String spielEntscheidung = BerichtHelper.spielEntscheidung(ergebnis, istHeim);
			if (spielEntscheidung.equals("unentschieden")) {
				wirkungen.add("Variante für einen überraschenden Punktgewinn.");
				wirkungen.add("Variante für einen enttäuschenden Punktverlust.");
			} else if (spielEntscheidung.equals("gewonnen")) {
				wirkungen.add("Variante für einen erwarteten Sieg.");
				wirkungen.add("Variante für einen unerwarteten Sieg.");
			} else {
				wirkungen.add("Variante für eine erwartete Niederlage.");
				wirkungen.add("Variante für eine unerwartete Niederlage.");

			}
		}
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
		}
	}

	public List<String> getModelle() {
		try {
			return modelle.getModelNames();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<String> getThinking() {
		return List.of("none", "low", "medium", "high", "max");
	}

	public void setBerichtIndex(int berichtIndex) {

		this.berichtIndex = berichtIndex;
	}

	public int getBerichtIndex() {
		return berichtIndex;
	}

	public void speichern(int index) {
		// Speichere in der DB: Berichtstext, ergebnisLink und das Bild (als Byte[])

		String raw = berichtText.get(berichtText.size() - index - 1).getText();

		// ANSI-Steuerzeichen entfernen
		String cleaned = raw.replaceAll("\u001B\\[[;\\d]*m", "");

		String html = "<p>" + cleaned.replaceAll("\\r?\\n\\s*\\r?\\n", "</p><p> </p><p>") // leere Zeilen → neuer Absatz
				.replaceAll("\\r?\\n", " ") // einfache Zeilenumbrüche → Leerzeichen
				.trim() + "</p>";

		dbService.saveBerichtData(vereinnr, ergebnisLink, html);
		dbService.saveLogData(vereinnr, ergebnisLink, "KI", "KI-Bericht gespeichert", "");
	}

	public void generieren() {
		if (wirkungen.isEmpty()) {

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Kein Schreibstil ausgewählt"));
			oeffnePanel();
			return;
		}
		if (wirkungen.size() > 3) {

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Zu viele Schreibstile ausgewählt"));
			oeffnePanel();
			return;
		}

		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		String prompt = params.get("prompt");
		SpielergebnisProvider provider = null;
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
			try {
				provider = SpielergebnisFactory.create(vereinnr, ergebnisLink, namensSpeicher);
				// namensSpeicher.fuelleNamensspeicher(vereinnr, provider, namensSpeicher);

				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Erfolgreich", "Spielbericht -> Json - Erfolgreich"));
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Fehler", "Spielbericht konnte nicht gelesen werden " + e.getMessage()));
			}
		}

		String jsonSpielplan = "";
		String prettyJsonSpielplan = "";
		String besondereSonder = "";
		if (zusaetzlicheJSON && tabelleUrl != null && tabelleUrl.startsWith("http")) {
			try {

				SpielplanService service = new SpielplanService(vereinnr, tabelleUrl);
				List<Spiel> spiele = service.getSpielplan();

				List<SpielMapped> mappedList = spiele.stream().map(SpielMapped::new).collect(Collectors.toList());

				ObjectMapper objectMapper = new ObjectMapper();
				jsonSpielplan = objectMapper.writeValueAsString(mappedList);
				ObjectMapper mapper = new ObjectMapper();
				prettyJsonSpielplan = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(mapper.readValue(jsonSpielplan, Object.class));
				prettyJsonSpielplan.replace("\n", "<br/>").replace(" ", "&nbsp;");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolgreich", "Spielplan -> Json - Erfolgreich"));
				if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
					besondereSonder = "- Berücksichtige im Bericht die kommenden Spiele. " + besondereSonder;
				}
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Fehler", "Spielplan konnte nicht gelesen werden " + e.getMessage()));
			}
		}

		String jsonBilanz = "";
		String prettyJsonBilanz = "";
		if (zusaetzlicheJSON && bilanzUrl != null && bilanzUrl.startsWith("http")) {
			try {
				BilanzService service = new BilanzService();
				List<Bilanz> bil = service.getBilanz(vereinnr, bilanzUrl, this.namensSpeicher);
				ObjectMapper objectMapper = new ObjectMapper();
				jsonBilanz = objectMapper.writeValueAsString(bil);
				ObjectMapper mapper = new ObjectMapper();
				prettyJsonBilanz = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(mapper.readValue(jsonBilanz, Object.class));
				prettyJsonBilanz.replace("\n", "<br/>").replace(" ", "&nbsp;");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolgreich", "Bilanzen -> Json - Erfolgreich"));
				if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
					besondereSonder = "- Berücksichtige im Bericht aussergewöhnlich gute Bilanzen. " + besondereSonder;
				}
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Fehler", "Bilanzen konnte nicht gelesen werden " + e.getMessage()));
			}
		}
		String jsonTabelle = "";
		String prettyJsonTabelle = "";
		if (zusaetzlicheJSON && tabelleUrl != null && tabelleUrl.startsWith("http")) {
			try {
				TabelleService service = new TabelleService();
				List<Tabelle> tab = service.getTabelle(tabelleUrl);
				ObjectMapper objectMapper = new ObjectMapper();
				jsonTabelle = objectMapper.writeValueAsString(tab);
				ObjectMapper mapper = new ObjectMapper();
				prettyJsonTabelle = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(mapper.readValue(jsonTabelle, Object.class));
				prettyJsonTabelle.replace("\n", "<br/>").replace(" ", "&nbsp;");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolgreich", "Tabelle -> Json - Erfolgreich"));
				if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
					besondereSonder = "- Berücksichtige im Bericht die Tabellenposition und die Position der kommenden Gegner. "
							+ besondereSonder;
				}
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Fehler", "Tabelle konnte nicht gelesen werden " + e.getMessage()));
			}
		}

		if (mehrereBerichte) {

		}
		String json = null;
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http") && provider != null) {
			try {
				json = provider.summaryToJson();
				ObjectMapper mapper = new ObjectMapper();
				Object json2 = mapper.readValue(json, Object.class);
				prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json2);
				prettyJson.replace("\n", "<br/>").replace(" ", "&nbsp;");
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Parsen Spielplan " + e.getMessage()));
			}
		} else {
			json = "";
			prettyJson = "";
		}

		if (!"".equals(jsonSpielplan)) {
			json = json + " Hier ist der Spielplan: " + jsonSpielplan;
			prettyJson = prettyJson + "\n Hier ist der Spielplan: \n" + prettyJsonSpielplan;
		}

		if (!"".equals(jsonTabelle)) {
			json = json + " Hier ist die Tabelle: " + jsonTabelle;
			prettyJson = prettyJson + "\n Hier ist die Tabelle: \n" + prettyJsonTabelle;
		}

		if (!"".equals(jsonBilanz)) {
			json = json + " Hier die Bilanzen: " + jsonBilanz;
			prettyJson = prettyJson + " \n Hier die Bilanzen: \n " + prettyJsonBilanz;
		}

		String besondere;
		if (besondereSonder.isBlank()) {
			besondere = "Neben dem Spielbericht brauchen keine besondere Vorkommnisse erwähnt werden.";
		} else {
			besondere = besondereSonder;
		}
		if (besondereVorkommnisse != null && !besondereVorkommnisse.isEmpty()) {
			if (namensSpeicher == null) {
				besondere = besondereSonder + " " + besondereVorkommnisse;

			} else {
				besondere = besondereSonder + " " + namensSpeicher.anonymisiereText(besondereVorkommnisse);
			}
		}

		StilGenerator stil = new StilGenerator();
		String stilrichtung = "";
		try {
			stilrichtung = stil.stilvariationen(vereinnr, wirkungen);
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Stilvariationen to Json " + e.getMessage()));
		}

		if (berichtgroesse > 500) {
			berichtgroesse = 500;
		}
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {
			this.frage = ConfigManager.erstellenPrompt(vereinnr, berichtMannschaft, String.valueOf(berichtgroesse),
					besondere, json, stilrichtung, wirkungen.size());
			this.frageAusgabe = ConfigManager.erstellenPrompt(vereinnr, berichtMannschaft,
					String.valueOf(berichtgroesse), besondere, "", stilrichtung, wirkungen.size());
		} else if (zusaetzlicheJSON) {
			this.frage = ConfigManager.erstellenFreiPrompt(vereinnr, freiMannschaft, freiPrompt,
					String.valueOf(berichtgroesse), besondere, json, stilrichtung, wirkungen.size());
			this.frageAusgabe = ConfigManager.erstellenFreiPrompt(vereinnr, freiMannschaft, freiPrompt,
					String.valueOf(berichtgroesse), besondere, "", stilrichtung, wirkungen.size());
		} else if (mehrereBerichte) {
			this.frage = ConfigManager.erstellenFreiPrompt(vereinnr, freiMannschaft, freiPrompt,
					String.valueOf(berichtgroesse), besondere, getFormatBerichte(), stilrichtung, wirkungen.size());
			this.frageAusgabe = ConfigManager.erstellenFreiPrompt(vereinnr, "", zusammenfassenPrompt,
					String.valueOf(berichtgroesse), besondere, getFormatBerichte(), stilrichtung, wirkungen.size());
		} else {
			this.frage = ConfigManager.erstellenPromptohneSpielbericht(vereinnr, heim, String.valueOf(berichtgroesse),
					besondere, json, stilrichtung, wirkungen.size());
			this.frageAusgabe = ConfigManager.erstellenPromptohneSpielbericht(vereinnr, heim,
					String.valueOf(berichtgroesse), besondere, "", stilrichtung, wirkungen.size());
		}
		anzahlKi = dbService.anzahlKI(vereinnr, ergebnisLink, "generiert");
		String antworten = "";
		try {
			if (anzahlKi <= 5 && !this.frage.isEmpty() && "false".equals(prompt)) {

				JSONObject schema = new JSONObject().put("type", "object")
						.put("properties", new JSONObject().put("Varianten",
								new JSONObject().put("type", "array").put("minItems", 1).put("maxItems", 3).put("items",
										new JSONObject().put("type", "object").put("properties", new JSONObject()
												.put("Variante",
														new JSONObject().put("type", "string").put("description",
																"Genau der Wert aus dem Feld 'variante' im Prompt"))
												.put("Stilversion",
														new JSONObject().put("type", "string").put("description",
																"Kombination aller Stilattribute als ein String"))
												.put("Text",
														new JSONObject().put("type", "string").put("description",
																"Der generierte Text")))
												.put("required",
														new JSONArray().put("Variante").put("Stilversion")
																.put("Text")))))
						.put("required", new JSONArray().put("Varianten"));
				ApiKIChatGPT ki = new ApiKIChatGPT(vereinnr, frage, selectedModel, selectedThinking, temperatur,
						frequencyPenalty, presencePenalty, schema);

				antworten = ki.getResponse();
				dbService.saveLogData(vereinnr, ergebnisLink, "KI", "KI-Bericht generiert", "", besondere);
				anzahlKi = dbService.anzahlKI(vereinnr, ergebnisLink, "generiert");
			} else {
				antworten = "Keine KI-Generierung mehr Möglich. Zu viele Anfragen für diesen Bericht oder es wurde an die KI keine Frage gestellt";
				dbService.saveLogData(vereinnr, ergebnisLink, "KI", "KI-Bericht erfolglos", "", frage);
			}
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolgreich", "Aufruf ChatGPT "));
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Aufruf ChatGPT " + e.getMessage()));
		}
		// Antworten abrufen und ausgeben

		if (namensSpeicher == null) {
			this.berichtText.addAll(parsenSpielberichte(antworten));
			ersetzungen = "";
		} else {
			// Antworten abrufen und ausgeben
			this.berichtText.addAll(parsenSpielberichte(namensSpeicher.rueckuebersetzen(antworten)));
		}
		ersetzungen = namensSpeicher.zeigeAlle();
		wirkungen.clear();

	}

	public void onSlide() {
		// Kann für Logging oder zusätzliche Verarbeitung genutzt werden
	}

	// Getter und Setter
	public String getHeim() {

		return decodeUrl(heim);
	}

	public String getGast() {
		return decodeUrl(gast);
	}

	public int getAnzahlKi() {
		return anzahlKi;
	}

	public String getDatum() {
		return datum;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public List<Spielbericht> getBerichtText() {

		List<Spielbericht> reversed = new ArrayList<>(berichtText);
		Collections.reverse(reversed);
		return reversed;
	}

	public void setBerichtText(List<Spielbericht> berichtText) {
		this.berichtText = berichtText;
	}

	public int getBerichtgroesse() {
		return berichtgroesse;
	}

	public void setBerichtgroesse(int berichtgroesse) {
		this.berichtgroesse = berichtgroesse;
	}

	public String getBesondereVorkommnisse() {
		return besondereVorkommnisse;
	}

	public void setBesondereVorkommnisse(String besondereVorkommnisse) {
		this.besondereVorkommnisse = besondereVorkommnisse;
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

	public static boolean enthaeltUmlaute(String text) {
		String umlaute = "äöüÄÖÜß";
		for (int i = 0; i < text.length(); i++) {
			if (umlaute.indexOf(text.charAt(i)) != -1) {
				return true;
			}
		}
		return false; // Kein Umlaut gefunden
	}

	public String getSelectedModel() {
		return selectedModel;
	}

	public void setSelectedModel(String selectedModel) {
		this.selectedModel = selectedModel;
	}

	public double getTemperatur() {
		return temperatur;
	}

	public void setTemperatur(double temperatur) {
		this.temperatur = temperatur;
	}

	public double getFrequencyPenalty() {
		return frequencyPenalty;
	}

	public double getPresencePenalty() {
		return presencePenalty;
	}

	public void setFrequencyPenalty(double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	public void setPresencePenalty(double presencePenalty) {
		this.presencePenalty = presencePenalty;
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

	public String getErsetzungen() {
		return (ersetzungen == null) ? " " : ersetzungen;
	}

	public String getInfo() {
		StringBuilder info = new StringBuilder();

		if (isHttpLink()) {
			info.append("<h2>Hinweis zur automatisierten Textgenerierung (mit Spielbericht)</h2>");
			info.append(
					"<p>Auch wenn besondere Anmerkungen nicht zwingend erforderlich sind, helfen sie dabei, den Spielbericht persönlicher, spannender und interessanter zu gestalten. ");
			info.append(
					"Nutze deshalb unbedingt die Möglichkeit, alles aufzuschreiben, was dir zum Spiel einfällt und im Bericht erwähnt werden sollte – egal ob Highlights, Stimmungen oder besondere Szenen. ");
			info.append(
					"Du musst deine Gedanken nicht ausformulieren. Stichworte oder kurze Notizen reichen völlig aus und dienen der KI als wertvolle Anhaltspunkte für die Ausarbeitung des Textes. ");
			info.append(
					"Der Spielbericht wird <strong>automatisch übernommen</strong> deshalb brauchen keine Spielergebnisse angegeben werden.</p>");
			info.append(
					"<p>Unter <strong>&quot;Erweiterte Einstellungen für den Bericht&quot;</strong> kannst du die gewünschte Länge des Textes (in Wörtern) sowie den Schreibstil festlegen. Die Voreinstellungen sind bereits auf einen typischen Zeitungsbericht abgestimmt, lassen sich aber bei Bedarf individuell anpassen. Es können bis zu drei Stilarten aus der Tabelle ausgewählt werden. Mehrere Schreibstile können mit der Maus ausgewählt werden, während die Strg-Taste gedrückt wird. </p>");
			info.append(
					"<p>Unter <strong>&quot;Optionale Standard KI-Übergaben&quot;</strong> kannst du noch den Spielplan mit Tabelle und die bisherigen Bilanzen übergeben. Damit könnte die KI zb. das nächste Spiel erwähnen, oder einen Sieg gegen den aktuellen Tabellenführer.</p>");
			info.append(
					"<p>Weitere Optionen findest du unter <strong>&quot;Erweiterte Einstellungen des KI-Moduls&quot;</strong>. Auch diese sind bereits sinnvoll vorkonfiguriert, können jedoch nach Wunsch angepasst werden.</p>");
			info.append("<br />");

			info.append("<h2>Hinweis zur Qualitätssicherung des Spielberichts</h2>");
			info.append("Bitte lies den Text vor der Veröffentlichung sorgfältig durch und prüfe ihn auf ");
			info.append("stilistische, sachliche und inhaltliche Korrektheit. Nimm bei Bedarf entsprechende ");
			info.append("Anpassungen oder Ergänzungen vor, um eine authentische und präzise Darstellung ");
			info.append("des Spielverlaufs zu gewährleisten.");
			info.append("<br />");
			info.append("<h2>Wichtiger Hinweis zum Datenschutz</h2>");
			info.append("Vor der Übergabe an die KI wurden alle Klarnamen ");
			info.append("automatisiert anonymisiert, um den Datenschutz zu gewährleisten. ");
			info.append("Nach der Textgenerierung wurden die Platzhalter wieder durch die ");
			info.append("korrekten Namen ersetzt. In seltenen Ausnahmefällen kann es ");
			info.append("vorkommen, dass die Rückersetzung unvollständig oder fehlerhaft ");
			info.append("ist. Bitte prüfe daher sorgfältig, ob alle Namen korrekt ");
			info.append("wiederhergestellt wurden, und nimm gegebenenfalls manuelle ");
			info.append("Anpassungen vor.");
			info.append("<br />");
			info.append("<h2>Verwendete Namensersetzungen:</h2>");
			info.append(getErsetzungen());

		} else {

			info.append("<h2>Hinweis zur automatisierten Textgenerierung (ohne Spielbericht)</h2>");
			info.append("Der folgende Text wird mit Unterstützung der ");
			info.append("Künstlichen Intelligenz ChatGPT erstellt. ");
			info.append("Da kein offizieller Spielbericht vorliegt, ist es wichtig, ");
			info.append("der Künstlichen Intelligenz ausreichend Informationen ");
			info.append("bereitzustellen. Dazu gehören beispielsweise der Ablauf des Spiels bzw. ");
			info.append("Turniers, beteiligte Teams oder Personen, besondere Vorkommnisse ");
			info.append("sowie weitere relevante Details.  ");
			info.append("Deshalb solltest du unbedingt alles aufschreiben, was dir einfällt ");
			info.append("und was du gerne im Bericht erwähnt haben möchtest. Dabei ist es nicht notwendig, ");
			info.append("die Angaben vollständig auszuformulieren. Es reicht vollkommen aus, wenn du ");
			info.append("einige Stichwörter oder kurze Notizen einträgst, die als Anhaltspunkte für die ");
			info.append("weitere Ausarbeitung durch die KI dienen können. ");
			info.append("Nur mit einer möglichst vollständigen Beschreibung kann ein ");
			info.append("sinnvoller und stimmiger Textvorschlag erstellt werden. ");
			info.append("<br />");
			info.append(
					"<p>Unter <strong>&quot;Erweiterte Einstellungen für den Bericht&quot;</strong> kannst du die gewünschte Länge des Textes (in Wörtern) sowie den Schreibstil festlegen. Die Voreinstellungen sind bereits auf einen typischen Zeitungsbericht abgestimmt, lassen sich aber bei Bedarf individuell anpassen. Es können bis zu drei Stilarten aus der Tabelle ausgewählt werden. Mehrere Schreibstile können mit der Maus ausgewählt werden, während die Strg-Taste gedrückt wird. </p>");
			info.append(
					"<p>Unter <strong>&quot;Optionale KI-Übergaben bei einem Freier Bericht:&quot;</strong> kannst du den Spielplan mit Tabelle und die Bilanzen übergeben. Damit kann zb. ein Saisonrückblick erstellt werden.</p>");
			info.append(
					"<p>Weitere Optionen findest du unter <strong>&quot;Erweiterte Einstellungen des KI-Moduls&quot;</strong>. Auch diese sind bereits sinnvoll vorkonfiguriert, können jedoch nach Wunsch angepasst werden.</p>");
			info.append("<br />");

			info.append("<h2>Hinweis zur Qualitätssicherung des Spielberichts</h2>");
			info.append("Bitte lies den Text vor der Veröffentlichung sorgfältig durch und prüfe ihn auf ");
			info.append("stilistische, sachliche und inhaltliche Korrektheit. Nimm bei Bedarf entsprechende ");
			info.append("Anpassungen oder Ergänzungen vor, um eine authentische und präzise Darstellung des ");
			info.append(heim + " zu gewährleisten.");
			info.append("<h2>Verwendete Namensersetzungen:</h2>");
			info.append(getErsetzungen());

		}

		return info.toString();
	}

	public String getFrage() {
		return (frage == null) ? " " : frage;
	}

	public String getFrageAusgabe() {
		return frageAusgabe;
	}

	public void setFrageAusgabe(String frageAusgabe) {
		this.frageAusgabe = frageAusgabe;
	}

	public void updBearbeitung() {
		dbService.verarbeiteEintrag(vereinnr, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
	}

	public String getWirkungBeispiel() {
		String beispielText = "";
		for (String wirkung : wirkungen) {
			beispielText += "<br /> <br /> <b>" + wirkung + " </b><br />"
					+ dbService.leseWirkungBeispiel(vereinnr, wirkung);
		}
		return beispielText;
	}

	public void setWirkung(List<String> wirkung) {
		this.wirkungen = wirkung;
	}

	public void setBericht(List<String> bericht) {
		this.berichte = bericht;
	}

	public List<KiZusammenfassenText> getBerichtBeispiel() {

		// Schritt 1: Alle vorhandenen Überschriften sammeln
		Set<String> vorhandeneUeberschriften = zusammenfassenTexte.stream().map(KiZusammenfassenText::getUeberschrift)
				.collect(Collectors.toSet());

		// Schritt 2: Fehlende Überschriften aus "berichte" hinzufügen
		for (String ueberschrift : berichte) {
			if (!vorhandeneUeberschriften.contains(ueberschrift)) {
				String text = dbService.loadUeberschrift(vereinnr, ueberschrift);
				zusammenfassenTexte.add(new KiZusammenfassenText(ueberschrift, text));
			}
		}

		// Schritt 3: Überschriften entfernen, die nicht mehr in "berichte" existieren
		Set<String> berichtUeberschriften = new HashSet<>(berichte);

		zusammenfassenTexte.removeIf(zt -> !berichtUeberschriften.contains(zt.getUeberschrift()));

		return zusammenfassenTexte;
	}

	public String getFormatBerichte() {

		getBerichtBeispiel();

		if (zusammenfassenTexte == null || zusammenfassenTexte.isEmpty()) {
			return "Keine Berichte ausgewählt";
		}

		StringBuilder sb = new StringBuilder();
		int berichtNr = 1;

		for (KiZusammenfassenText bericht : zusammenfassenTexte) {
			sb.append("Bericht ").append(berichtNr++).append(":\n").append(bericht.getUeberschrift()).append("\n")
					.append(bericht.getText()).append("\n\n");
		}

		return sb.toString();
	}

	// Diese Methode wird durch den "Speichern"-Button aufgerufen.
	public void zurueck() {
		dbService.deleteUUID(vereinnr, ergebnisLink, uuid);
	}

	public String getKiRueckgabe() {
		return kiRueckgabe;
	}

	public void setKiRueckgabe(String kiRueckgabe) {
		this.kiRueckgabe = kiRueckgabe;
	}

	public boolean isHttpLink() {
		return ergebnisLink != null && ergebnisLink.startsWith("http");
	}

	public String getBerichtMannschaft() {
		return berichtMannschaft;
	}

	public void setBerichtMannschaft(String berichtMannschaft) {
		this.berichtMannschaft = berichtMannschaft;
	}

	public List<String> getSpielPaarung() {
		if (isHttpLink()) {
			String[] werteArray = { heim, gast };
			return Arrays.asList(werteArray);
		} else {
			String[] werteArray = { freierText };
			return Arrays.asList(werteArray);
		}

	}

	public List<String> getWirkungen() {
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {

			String ergebnisText = BerichtHelper.spielEntscheidung(ergebnis, istHeim);

			if (ergebnisText.contains("gewonnen")) {
				return dbService.listeWirkung(vereinnr, "Sieg");
			} else if (ergebnisText.contains("unentschieden")) {
				return dbService.listeWirkung(vereinnr, "Unentschieden");
			} else {
				return dbService.listeWirkung(vereinnr, "Niederlage");
			}
		} else {
			return dbService.listeWirkung(vereinnr, "alle");
		}
	}

	public List<String> getBerichte() {
		int configTage = Integer.parseInt(ConfigManager.getConfigValue(vereinnr, "freierBericht.rueckschau.tage"));
		return dbService.listeUeberschriften(vereinnr, configTage);
	}

	public List<String> getWirkung() {
		return this.wirkungen;
	}

	public List<String> getBericht() {
		return this.berichte;
	}

	public String getPrettyJson() {
		return prettyJson;
	}

	public void setPrettyJson(String prettyJson) {
		this.prettyJson = prettyJson;
	}

	public boolean getFreierBericht() {
		if (ergebnisLink.startsWith("http")) {
			return false;
		}
		return true;
	}

	public String getFreiPrompt() {
		if (freiPrompt == null || "".equals(freiPrompt)) {
			freiPrompt = ConfigManager.getConfigValue(vereinnr, "bericht.ki.freierPrompt");
		}
		return freiPrompt;
	}

	public String getZusammenfassenPrompt() {

		if (zusammenfassenPrompt == null || "".equals(zusammenfassenPrompt)) {
			zusammenfassenPrompt = ConfigManager.getConfigValue(vereinnr, "bericht.ki.zusammenfassen");
		}
		return zusammenfassenPrompt;
	}

	public void setFreiPrompt(String freiPrompt) {
		this.freiPrompt = freiPrompt;
	}

	public void setZusammenfassenPrompt(String zusammenfassenPrompt) {
		this.zusammenfassenPrompt = zusammenfassenPrompt;
	}

	public String getFreiMannschaft() {
		if (freiMannschaft == null || "".equals(freiMannschaft)) {
			freiMannschaft = ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
		}
		return freiMannschaft;
	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public String getTabelleUrl() {
		return tabelleUrl;
	}

	public String getBilanzUrl() {
		return bilanzUrl;
	}

	public void setFreiMannschaft(String freiMannschaft) {
		this.freiMannschaft = freiMannschaft;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}

	public void setTabelleUrl(String tabelleUrl) {
		this.tabelleUrl = tabelleUrl;
	}

	public void setBilanzUrl(String bilanzUrl) {
		this.bilanzUrl = bilanzUrl;
	}

	public String getPrettyJsonTabelle() {
		return prettyJsonTabelle;
	}

	public String getPrettyJsonSpielplan() {
		return prettyJsonSpielplan;
	}

	public String getPrettyJsonBilanz() {
		return prettyJsonBilanz;
	}

	public void setPrettyJsonTabelle(String prettyJsonTabelle) {
		this.prettyJsonTabelle = prettyJsonTabelle;
	}

	public void setPrettyJsonSpielplan(String prettyJsonSpielplan) {
		this.prettyJsonSpielplan = prettyJsonSpielplan;
	}

	public void setPrettyJsonBilanz(String prettyJsonBilanz) {
		this.prettyJsonBilanz = prettyJsonBilanz;
	}

	public boolean isZusaetzlicheJSON() {
		return zusaetzlicheJSON;
	}

	public boolean isMehrereBerichte() {
		return mehrereBerichte;
	}

	public void setZusaetzlicheJSON(boolean zusaetzlicheJSON) {
		this.zusaetzlicheJSON = zusaetzlicheJSON;
	}

	public void setMehrereBerichte(boolean mehrereBerichte) {
		this.mehrereBerichte = mehrereBerichte;
	}

	public List<Spielbericht> parsenSpielberichte(String rawJson) {

		// 1) Trim + Code-Fences entfernen
		String cleaned = rawJson.trim().replaceAll("```[a-zA-Z0-9]*", "").replaceAll("```", "").trim();

		// 2) JSON-Struktur extrahieren
		cleaned = extractJson(cleaned);
		if (cleaned == null || cleaned.isEmpty()) {
			return fallback(rawJson);
		}

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

		try {
			List<Spielbericht> result = new ArrayList<>();

			// Wurzel ist jetzt immer ein Objekt mit "Varianten"
			JsonNode rootNode = mapper.readTree(cleaned);
			JsonNode variantenNode = rootNode.get("Varianten");

			if (variantenNode != null && variantenNode.isArray()) {
				for (JsonNode node : variantenNode) {
					Spielbericht bericht = mapper.treeToValue(node, Spielbericht.class);
					result.add(bericht);
				}
			} else {
				// Fallback: evtl. Einzelobjekt statt Array
				Spielbericht einzel = mapper.treeToValue(rootNode, Spielbericht.class);
				result.add(einzel);
			}

			// Reihenfolge invertieren
			Collections.reverse(result);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return fallback(rawJson);
		}
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

	private List<Spielbericht> fallback(String originalJson) {
		List<Spielbericht> result = new ArrayList<>();
		Spielbericht fallback = new Spielbericht();
		fallback.setText(originalJson);
		fallback.setVariante("");
		fallback.setStilversion("");
		result.add(fallback);
		return result;
	}

	public void spielplanAnzeige() throws IOException {
		if (spielplan) {
			spielplan = false;
		} else {
			spielplan = true;
		}
	}

	public boolean isSpielplan() {
		return spielplan;
	}

	public void setSpielplan(boolean spielplan) {
		this.spielplan = spielplan;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public boolean isPanelCollapsed() {
		return panelCollapsed;
	}

	public void setPanelCollapsed(boolean panelCollapsed) {
		this.panelCollapsed = panelCollapsed;
	}

	// Irgendeine Methode, um es per Java zu öffnen
	public void oeffnePanel() {
		panelCollapsed = false; // false = aufgeklappt
	}

	public void updateTabelleUrl() {
		if (selectedMannschaft != null) {
			tabelleUrl = selectedMannschaft.getSpielplanUrl();
			bilanzUrl = selectedMannschaft.getBilanzenUrl();
		}
	}

	public String getSelectedThinking() {
		return selectedThinking;
	}

	public void setSelectedThinking(String selectedThinking) {
		this.selectedThinking = selectedThinking;
	}

	public List<KiZusammenfassenText> getZusammenfassenTexte() {
		getBerichtBeispiel();
		return zusammenfassenTexte;
	}

	public void setZusammenfassenTexte(List<KiZusammenfassenText> zusammenfassenTexte) {
		this.zusammenfassenTexte = zusammenfassenTexte;
	}
}
