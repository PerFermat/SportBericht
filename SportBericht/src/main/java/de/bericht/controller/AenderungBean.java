
// Lombok is not used in this file. If you have Lombok annotations (like @Getter, @Setter, @Data, etc.), remove them to avoid class loader errors.
// This file does not use Lombok, so no changes are needed here.
package de.bericht.controller;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.service.DatabaseService;
import de.bericht.service.KiAenderung;
import de.bericht.util.ApiKIChatGPT;
import de.bericht.util.BerichtData;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.Spielbericht;
import de.bericht.util.StilGenerator;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class AenderungBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private String ueberschrift;
	private String heim;
	private String gast;
	private String datum;
	private String ergebnis;
	private String ergebnisLink;
	private String berichtMannschaft;
	private String liga;
	private String ligaSpiel;	
	private String vereinnr;
	private String uuid;
	private String berichtText;
	private Spielbericht berichtTextNeu = new Spielbericht();
	private String spielErgebnis; // Für die Anzeige der Spielergebnisse
	private boolean istHeim;
	private DatabaseService dbService = new DatabaseService();
	private List<KiAenderung> kiAenderungen = new ArrayList<>();
	private boolean spielplan = false;
	private int anzahlKi;
	private String frageAusgabe;
	private String gruppeUrl;

	public AenderungBean() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		this.vereinnr = params.get("vereinnr");
		this.heim = params.get("heim");
		this.gast = params.get("gast");
		this.datum = params.get("datum");
		this.ergebnis = params.get("ergebnis");
		this.ergebnisLink = params.get("ergebnisLink");
		this.liga = params.get("liga");
		this.ligaSpiel = params.get("ligaSpiel");		
		this.uuid = params.get("uuid");
		this.gruppeUrl = params.get("gruppeUrl");
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
		}
		// Bericht und Bild aus DB laden, falls vorhanden
		if (ergebnisLink != null && !ergebnisLink.isEmpty()) {
			BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);
			this.ueberschrift = data.getUeberschrift() != null ? data.getUeberschrift() : heim;
			this.berichtText = data.getBerichtText() != null ? data.getBerichtText() : "";
		} else {
			this.berichtText = "";
		}
		this.spielErgebnis = dbService.loadSpielstatistik(vereinnr, ergebnisLink);
		this.anzahlKi = dbService.anzahlKI(vereinnr, ergebnisLink, "geändert");

		if (!isHttpLink()) {
			this.berichtMannschaft = "";
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

		kiAenderungen.add(new KiAenderung("Ungefähre Anzahl der Wörter im Bericht:", "Zahl", 50, 10, 500, null,
				countWordsRoundedTo10(berichtText), ""));
		if (!isHttpLink()) {
			kiAenderungen.add(new KiAenderung("Ändere die Überschrift:", "Text", 50, 10, 500, null, 0,
					"Wie soll die Überschrift angepasst werden"));
		}
		kiAenderungen.add(new KiAenderung("Hervorhebung einzelner Spieler:", "Text", 0, 0, 0, "", 0,
				"Wer soll aus welchen Grund hervorgehoben werden"));
		kiAenderungen.add(new KiAenderung("Zusätzliche Inhalte einbauen:", "Text", 0, 0, 0, "", 0,
				"Zusätzliche Inhalte angeben"));
		kiAenderungen.add(new KiAenderung("Allgemeine Änderungen im Text:", "Text", 0, 0, 0, "", 0,
				"Folgende Änderungen sollen gemacht werden"));
		kiAenderungen.add(new KiAenderung("Folgende Inhalte aufjedenfall Beibehalten:", "Text", 0, 0, 0, "", 0,
				"Was soll beibehalten werden"));
		kiAenderungen.add(new KiAenderung("Entferne folgende Inhalte:", "Text", 0, 0, 0, "", 0,
				"Folgende Inhalte sollen gelöscht werden"));
		kiAenderungen.add(new KiAenderung("Schreibe den Anfang des Berichts um:", "Text", 0, 0, 0, "", 0,
				"Wie soll der Anfang des Berichts aussehen"));
		kiAenderungen.add(new KiAenderung("Schreibe das Ende des Berichts um:", "Text", 0, 0, 0, "", 0,
				"Wie soll das Ende des Berichts aussehen"));
		kiAenderungen.add(new KiAenderung("Stilrichtung:", "Auswahl", 0, 0, 0, "Keine Änderung des Schreibstils", 0,
				"stilrichtungen"));
	}

	/**
	 * Liefert den Namen der Heimmannschaft.
	 *
	 * @return Name der Heimmannschaft
	 */
	public String getHeim() {
		return heim;
	}

	/**
	 * Liefert den Namen der Gastmannschaft.
	 *
	 * @return Name der Gastmannschaft
	 */
	public String getGast() {
		return gast;
	}

	/**
	 * Liefert das Datum des Spiels/Berichts.
	 *
	 * @return Datum als String
	 */
	public String getDatum() {
		return datum;
	}

	/**
	 * Liefert das Ergebnis des Spiels.
	 *
	 * @return Ergebnis als String
	 */
	public String getErgebnis() {
		return ergebnis;
	}

	/**
	 * Liefert den Link zum Spiel-/Ergebnisdatensatz.
	 *
	 * @return URL oder Identifier des Ergebnisses
	 */
	public String getErgebnisLink() {
		return ergebnisLink;
	}

	/**
	 * Liefert die Mannschaft, für die der Bericht gilt.
	 *
	 * @return Mannschaftsname oder null
	 */
	public String getBerichtMannschaft() {
		return berichtMannschaft;
	}

	/**
	 * Liefert die Liga/Staffel des Spiels.
	 *
	 * @return Liga als String
	 */
	public String getLiga() {
		return liga;
	}

	/**
	 * Liefert die Vereinsnummer.
	 *
	 * @return Vereinsnummer als String
	 */
	public String getVereinnr() {
		return vereinnr;
	}

	/**
	 * Liefert die UUID für die aktuelle Bearbeitung.
	 *
	 * @return UUID als String
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Setzt den Namen der Heimmannschaft.
	 *
	 * @param heim Name der Heimmannschaft
	 */
	public void setHeim(String heim) {
		this.heim = heim;
	}

	/**
	 * Setzt den Namen der Gastmannschaft.
	 *
	 * @param gast Name der Gastmannschaft
	 */
	public void setGast(String gast) {
		this.gast = gast;
	}

	/**
	 * Setzt das Datum des Spiels/Berichts.
	 *
	 * @param datum Datum als String
	 */
	public void setDatum(String datum) {
		this.datum = datum;
	}

	/**
	 * Setzt das Ergebnis des Spiels.
	 *
	 * @param ergebnis Ergebnis als String
	 */
	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
	}

	/**
	 * Setzt den Link/Identifier zum Ergebnisdatensatz.
	 *
	 * @param ergebnisLink URL oder Identifier
	 */
	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	/**
	 * Setzt die Mannschaft, für die der Bericht gedacht ist.
	 *
	 * @param berichtMannschaft Mannschaftsname
	 */
	public void setBerichtMannschaft(String berichtMannschaft) {
		this.berichtMannschaft = berichtMannschaft;
	}

	/**
	 * Setzt die Liga/Staffel des Spiels.
	 *
	 * @param liga Liga als String
	 */
	public void setLiga(String liga) {
		this.liga = liga;
	}

	/**
	 * Setzt die Vereinsnummer.
	 *
	 * @param vereinnr Vereinsnummer als String
	 */
	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	/**
	 * Setzt die UUID für diese Bearbeitung.
	 *
	 * @param uuid UUID als String
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Prüft, ob es sich um einen freien Bericht ohne externen Link handelt.
	 *
	 * @return true, wenn kein HTTP-Link vorhanden ist
	 */
	public boolean getFreierBericht() {
		if (ergebnisLink.startsWith("http")) {
			return false;
		}
		return true;
	}

	/**
	 * Liefert die Überschrift des Berichts.
	 *
	 * @return Überschrift als String
	 */
	public String getUeberschrift() {
		return ueberschrift;
	}

	/**
	 * Setzt die Überschrift und dekodiert sie falls nötig.
	 *
	 * @param ueberschrift Überschrift als String
	 */
	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = decodeUrl(ueberschrift);
	}

	/**
	 * Liefert den aktuellen Berichtstext (HTML/Raw).
	 *
	 * @return Berichtstext
	 */
	public String getBerichtText() {
		return berichtText;
	}

	/**
	 * Setzt den Berichtstext und dekodiert ihn falls nötig.
	 *
	 * @param berichtText Berichtstext als String
	 */
	public void setBerichtText(String berichtText) {
		this.berichtText = decodeUrl(berichtText);

	}

	/**
	 * Dekodiert einen übergebenen String von ISO-8859-1 nach UTF-8, sofern keine
	 * Umlaute vorhanden sind.
	 *
	 * @param url Eingabe-String
	 * @return Dekodierter String oder null bei null-Eingabe
	 */
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

	/**
	 * Prüft, ob der übergebene Text deutsche Umlaute oder ß enthält.
	 *
	 * @param text Zu prüfender Text
	 * @return true, falls Umlaute vorhanden sind
	 */
	public static boolean enthaeltUmlaute(String text) {
		String umlaute = "äöüÄÖÜß";
		for (int i = 0; i < text.length(); i++) {
			if (umlaute.indexOf(text.charAt(i)) != -1) {
				return true;
			}
		}
		return false; // Kein Umlaut gefunden
	}

	/**
	 * Liefert die formatierte Spielausgabe für die Anzeige.
	 *
	 * @return Spielausgabe als String
	 */
	public String getSpielErgebnis() {
		return spielErgebnis;
	}

	/**
	 * Setzt die Spielausgabe für die Anzeige.
	 *
	 * @param spielErgebnis Spielausgabe als String
	 */
	public void setSpielErgebnis(String spielErgebnis) {
		this.spielErgebnis = spielErgebnis;
	}

	/**
	 * Prüft, ob der Ergebnislink ein HTTP-Link ist.
	 *
	 * @return true, wenn `ergebnisLink` mit "http" beginnt
	 */
	public boolean isHttpLink() {
		return ergebnisLink != null && ergebnisLink.startsWith("http");
	}

	/**
	 * Gibt zurück, ob das Team Heim ist.
	 *
	 * @return true, wenn Heimteam
	 */
	public boolean isIstHeim() {
		return istHeim;
	}

	/**
	 * Setzt den Heim-Flag.
	 *
	 * @param istHeim true, falls Heimteam
	 */
	public void setIstHeim(boolean istHeim) {
		this.istHeim = istHeim;
	}

	/**
	 * Liefert die Liste der KI-Änderungsoptionen.
	 *
	 * @return Liste von `KiAenderung`
	 */
	public List<KiAenderung> getKiAenderungen() {
		return kiAenderungen;
	}

	/**
	 * Liefert eine HTML-formatierte Hilfebeschreibung für die Änderungsoptionen.
	 *
	 * @return HTML-String mit Hilfetext
	 */
	public String getInfo() {
		StringBuilder info = new StringBuilder();

		info.append("<h2>Hinweis zur automatisierten Textanpassung</h2>");

		info.append(
				"<p>Auf dieser Seite kannst du gezielt festlegen, wie der aktuelle Bericht angepasst werden soll. ");
		info.append(
				"Alle Angaben sind optional – je genauer du sie machst, desto besser kann der Text an deine Wünsche angepasst werden.</p>");

		info.append("<h3>Mögliche Änderungsoptionen</h3>");

		info.append("<ul>");

		info.append("<li><b>Ungefähre Anzahl der Wörter im Bericht</b><br/>");
		info.append("Hier kannst du angeben, wie lang der Bericht ungefähr sein soll. ");
		info.append("Die aktuelle Wortanzahl ist als Vorbelegung angegeben. ");
		info.append("Die KI orientiert sich an dieser Wortanzahl, ohne sie zwangsläufig exakt einzuhalten.</li>");

		if (!isHttpLink()) {
			info.append("<li><b>Überschrift ändern</b><br/>");
			info.append("Falls du eine andere Überschrift wünschst, kannst du hier angeben, ");
			info.append("wie die Überschrift formuliert oder angepasst werden soll.</li>");
		}

		info.append("<li><b>Zusätzliche Inhalte einbauen</b><br/>");
		info.append("Hier kannst du Aspekte ergänzen, die im bisherigen Text fehlen, z. B. besondere Spielszenen, ");
		info.append("Auffälligkeiten oder persönliche Eindrücke.</li>");

		info.append("<li><b>Allgemeine Änderungen im Text</b><br/>");
		info.append(
				"Nutze dieses Feld für übergreifende Anpassungen, die sich nicht eindeutig einem einzelnen Abschnitt ");
		info.append("zuordnen lassen.</li>");

		info.append("<li><b>Inhalte entfernen</b><br/>");
		info.append("Wenn bestimmte Passagen oder Aussagen nicht im Bericht erscheinen sollen, ");
		info.append("kannst du sie hier benennen.</li>");

		info.append("<li><b>Anfang des Berichts umschreiben</b><br/>");
		info.append("Hier kannst du vorgeben, wie der Einstieg in den Bericht gestaltet werden soll, ");
		info.append("z. B. sachlicher, emotionaler oder spannender.</li>");

		info.append("<li><b>Ende des Berichts umschreiben</b><br/>");
		info.append("Falls du einen anderen Abschluss wünschst, kannst du hier beschreiben, ");
		info.append("wie der Bericht enden soll.</li>");

		info.append("<li><b>Stilrichtung</b><br/>");
		info.append("Mit der Stilrichtung kannst du festlegen, wie der Bericht sprachlich gestaltet sein soll, ");
		info.append("z. B. neutral, lebendig oder besonders locker. ");
		info.append("Wenn du keine Auswahl triffst, bleibt der bisherige Schreibstil erhalten.</li>");

		info.append("</ul>");

		info.append("<p><b>Hinweis:</b> Nicht ausgefüllte Felder werden ignoriert. ");
		info.append("Es werden nur die Änderungen übernommen, die du explizit angibst.</p>");

		return info.toString();
	}

	/**
	 * Erzeugt per KI angepasste Berichtsversionen basierend auf den konfigurierten
	 * `KiAenderung`-Optionen und setzt `berichtTextNeu`.
	 */
	public void generieren() {
		String antwort = null;
		StringBuilder sb = new StringBuilder();

		sb.append("Verändere den Orginaltext wie folgt:");
		sb.append("<br>");
		for (KiAenderung kiAenderung : kiAenderungen) {
			if ("Zahl".equals(kiAenderung.getTyp())) {
				if (kiAenderung.getWert_zahl() > 0) {
					sb.append(kiAenderung.getBeschreibung());
					sb.append(" ");
					sb.append(" " + kiAenderung.getWert_zahl());
					sb.append("<br>");
				}
			} else if ("Auswahl".equals(kiAenderung.getTyp())
					&& "stilrichtungen".equals(kiAenderung.getPlatzhalter())) {
				if (kiAenderung.getWert_text().length() > 0
						&& !"Keine Änderung des Schreibstils".equals(kiAenderung.getWert_text())) {
					StilGenerator stil = new StilGenerator();
					String stilrichtung = "";
					List<String> wirkungen = new ArrayList<>();
					wirkungen.add(kiAenderung.getWert_text());
					try {
						stilrichtung = stil.stilvariationen(vereinnr, wirkungen);
					} catch (Exception e) {
						FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
								"Fehler", "Stilvariationen to Json " + e.getMessage()));
					}
					sb.append(kiAenderung.getBeschreibung());
					sb.append(" ");
					sb.append(" " + stilrichtung);
					sb.append("<br>");
				}
			} else {
				if (kiAenderung.getWert_text().length() > 0) {
					sb.append(kiAenderung.getBeschreibung());
					sb.append(" ");
					sb.append(" " + kiAenderung.getWert_text());
					sb.append("<br>");
				}
			}
		}
		sb.append("Ausgabe ausschließlich neuer Bericht ohne sonstige Erklärungen");
		sb.append("<br>");
		sb.append("Ab hier beginnt der Orginaltext:");
		sb.append(berichtText);
		String frage = sb.toString();
		frageAusgabe = frage;
		this.anzahlKi = dbService.anzahlKI(vereinnr, ergebnisLink, "geändert");

		try {
			if (anzahlKi <= 5 && !frage.isEmpty()) {
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
												.put("Erklaerung", new JSONObject().put("type", "string").put(
														"description",
														"Kurze Erklärung der vorgenommenen Änderungen oder des Stils"))
												.put("Text",
														new JSONObject().put("type", "string").put("description",
																"Der neu generierte Berichtstext"))
												.put("Ueberschrift",
														new JSONObject().put("type", "string").put("description",
																"Die neue Überschrift")))
												.put("required",
														new JSONArray().put("Variante").put("Stilversion")
																.put("Erklaerung").put("Bericht")))))
						.put("required", new JSONArray().put("Varianten"));

				ApiKIChatGPT ki = new ApiKIChatGPT(vereinnr, frage,
						ConfigManager.getConfigValue(vereinnr, "bericht.kikorrektur.model"), "none", 0.5, 0.5, 0.5,
						schema);

				antwort = ki.getResponse();
				dbService.saveLogData(vereinnr, ergebnisLink, "KI", "KI-Bericht geändert", "", frage);
				anzahlKi = dbService.anzahlKI(vereinnr, ergebnisLink, "geändert");
			}

		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Aufruf ChatGPT " + e.getMessage()));
		}
		this.berichtTextNeu = parsenSpielbericht(antwort);
	}

	/**
	 * Parst die rohe KI-Antwort (JSON/Code-Fences) und liefert ein
	 * `Spielbericht`-Objekt.
	 *
	 * @param rawJson Roher JSON-String oder Text vom KI-Service
	 * @return Geparster `Spielbericht` oder Fallback bei Fehlern
	 */
	public Spielbericht parsenSpielbericht(String rawJson) {

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
			JsonNode rootNode = mapper.readTree(cleaned);

			// Erwartet: Objekt mit Feld "Varianten"
			JsonNode variantenNode = rootNode.get("Varianten");

			if (variantenNode != null && variantenNode.isArray() && variantenNode.size() > 0) {
				// Nimm IMMER den ersten Spielbericht
				return mapper.treeToValue(variantenNode.get(0), Spielbericht.class);
			}

			// Fallback: falls direkt ein einzelnes Objekt geliefert wird
			return mapper.treeToValue(rootNode, Spielbericht.class);

		} catch (Exception e) {
			e.printStackTrace();
			return fallback(rawJson);
		}
	}

	/**
	 * Extrahiert den ersten JSON-Block aus dem Eingabetext (Objekt oder Array).
	 *
	 * @param input Eingabetext
	 * @return Substring ab der JSON-Startposition oder null
	 */
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

	/**
	 * Fallback, falls das Parsen fehlschlägt: legt ein `Spielbericht` mit dem
	 * Originaltext an.
	 *
	 * @param originalJson Originaltext
	 * @return `Spielbericht`-Fallback
	 */
	private Spielbericht fallback(String originalJson) {
		Spielbericht result = null;
		Spielbericht fallback = new Spielbericht();
		fallback.setText(originalJson);
		fallback.setVariante("");
		fallback.setStilversion("");
		return result;
	}

	/**
	 * Schaltet die Anzeige des Spielplans um.
	 *
	 * @throws IOException Bei IO-Fehlern
	 */
	public void spielplanAnzeige() throws IOException {
		if (spielplan) {
			spielplan = false;
		} else {
			spielplan = true;
		}
	}

	/**
	 * Liefert, ob die Spielplananzeige aktiv ist.
	 *
	 * @return true, wenn Spielplan angezeigt wird
	 */
	public boolean isSpielplan() {
		return spielplan;
	}

	/**
	 * Aktualisiert die Bearbeitungseinträge in der Datenbank (fügt ggf. neuen
	 * Eintrag hinzu).
	 */
	public void updBearbeitung() {
		dbService.verarbeiteEintrag(vereinnr, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
	}

	/**
	 * Liefert den neu generierten `Spielbericht`.
	 *
	 * @return `Spielbericht` mit generiertem Text
	 */
	public Spielbericht getBerichtTextNeu() {
		return berichtTextNeu;
	}

	/**
	 * Setzt den generierten `Spielbericht`.
	 *
	 * @param berichtTextNeu `Spielbericht`-Objekt
	 */
	public void setBerichtTextNeu(Spielbericht berichtTextNeu) {
		this.berichtTextNeu = berichtTextNeu;
	}

	/**
	 * Zählt Wörter und rundet auf das nächste Vielfache von 10 auf.
	 *
	 * @param text Eingabetext
	 * @return Aufgerundete Wortanzahl (Vielfaches von 10)
	 */
	public static int countWordsRoundedTo10(String text) {
		if (text == null || text.trim().isEmpty()) {
			return 0;
		}

		// Wörter zählen (ein oder mehrere Whitespaces als Trennung)
		int wordCount = text.trim().split("\\s+").length;

		// Aufrunden auf das nächste Vielfache von 10
		return ((wordCount + 9) / 10) * 10;
	}

	/**
	 * Liefert die verfügbaren Stilrichtungen / Wirkungen für die KI-Anpassung.
	 *
	 * @return Liste von Stilrichtungen
	 */
	public List<String> stilrichtungen() {
		List<String> wirkung = new ArrayList<String>();
		wirkung.add("Keine Änderung des Schreibstils");
		if (ergebnisLink != null && !ergebnisLink.isEmpty() && ergebnisLink.startsWith("http")) {

			String ergebnisText = BerichtHelper.spielEntscheidung(ergebnis, istHeim);

			if (ergebnisText.contains("gewonnen")) {
				wirkung.addAll(dbService.listeWirkung(vereinnr, "Sieg"));
			} else if (ergebnisText.contains("unentschieden")) {
				wirkung.addAll(dbService.listeWirkung(vereinnr, "Unentschieden"));
			} else {
				wirkung.addAll(dbService.listeWirkung(vereinnr, "Niederlage"));
			}
		} else {
			wirkung.addAll(dbService.listeWirkung(vereinnr, "alle"));
		}
		return wirkung;
	}

	/**
	 * Speichert den generierten Bericht in der Datenbank (bereinigt und formatiert
	 * als HTML).
	 */
	public void speichern() {
		String cleaned = berichtTextNeu.getText().replaceAll("\u001B\\[[;\\d]*m", "");

		String html = "<p>" + cleaned.replaceAll("\\r?\\n\\s*\\r?\\n", "</p><p> </p><p>") // leere Zeilen → neuer Absatz
				.replaceAll("\\r?\\n", " ") // einfache Zeilenumbrüche → Leerzeichen
				.trim() + "</p>";

		dbService.saveBerichtData(vereinnr, ergebnisLink, html, berichtTextNeu.getUeberschrift());
		dbService.saveLogData(vereinnr, ergebnisLink, "KI", "KI-Bericht gespeichert", "");
	}

	/**
	 * Entfernt die aktuelle UUID aus der Datenbank und kehrt zurück.
	 */
	public void zurueck() {
		dbService.deleteUUID(vereinnr, ergebnisLink, uuid);
	}

	/**
	 * Liefert die Anzahl der bisher genutzten KI-Anfragen für diesen Bericht.
	 *
	 * @return Anzahl KI-Anfragen
	 */
	public int getAnzahlKi() {
		return anzahlKi;
	}

	/**
	 * Liefert den formatierten Prompt/Frage, die an die KI gesendet wurde.
	 *
	 * @return Prompt-String
	 */
	public String getFrageAusgabe() {
		return frageAusgabe;
	}

	/**
	 * Setzt den Prompt/Frage-Text zur Anzeige oder Weiterverarbeitung.
	 *
	 * @param frageAusgabe Prompt-String
	 */
	public void setFrageAusgabe(String frageAusgabe) {
		this.frageAusgabe = frageAusgabe;
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}

	public String getLigaSpiel() {
		return ligaSpiel;
	}

	public void setLigaSpiel(String ligaSpiel) {
		this.ligaSpiel = ligaSpiel;
	}

}
