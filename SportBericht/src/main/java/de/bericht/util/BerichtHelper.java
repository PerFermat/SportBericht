package de.bericht.util;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import de.bericht.service.DatabaseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named("berichtHelper")
@ApplicationScoped
public class BerichtHelper {

	public static final PolicyFactory SAFE_HTML_POLICY = new HtmlPolicyBuilder()
			// Grundlegende Formatierungen
			.allowElements("b", "i", "u", "strong", "em", "s", "sub", "sup", "small")

			// Textstruktur
			.allowElements("p", "br", "div", "span", "blockquote", "pre", "code")
			.allowElements("h1", "h2", "h3", "h4", "h5", "h6").allowElements("ul", "ol", "li", "hr")

			// Links
			.allowElements("a").allowAttributes("href").matching(Pattern.compile("^(?i)(https?|mailto):.*"))
			.onElements("a").allowAttributes("title").onElements("a").allowAttributes("target")
			.matching(Pattern.compile("(?i)_(blank|self|parent|top)")).onElements("a").allowAttributes("rel")
			.onElements("a").requireRelNofollowOnLinks() // Füge rel="nofollow" automatisch hinzu
			.allowStandardUrlProtocols()

			// Bilder
			.allowElements("img").allowAttributes("src").matching(Pattern.compile("^(?i)(https?):.*")).onElements("img")
			.allowAttributes("alt", "title", "width", "height").onElements("img")

			// Style-Attribute (Textausrichtung, Farbe, Größe etc.)
			.allowAttributes("style").onElements("p", "div", "span", "h1", "h2", "h3", "h4", "h5", "h6", "img")
			.allowAttributes("class").onElements("p", "div", "span", "li", "img").allowStyling()

			// Erlaubte URL-Protokolle
			.allowStandardUrlProtocols()

			.allowElements("table", "thead", "tbody", "tfoot", "tr", "th", "td")
			.allowAttributes("border", "cellpadding", "cellspacing", "style").onElements("table", "td", "th", "tr")

			.toFactory();

	// Cache für die aktuelle Request: Schlüssel = ergebnisLink, Wert = geladenes
	// BerichtData

	private static final Cache<String, ConcurrentHashMap<String, BerichtData>> cache = Caffeine.newBuilder()
			.maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(500_000)).build();

	/**
	 * Lädt (oder liefert aus dem Cache) den BerichtData für den gegebenen
	 * ergebnisLink.
	 */
	private static BerichtData getCachedBerichtData(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return null;
		}

		// Versuche, die Map für den Verein abzurufen
		ConcurrentHashMap<String, BerichtData> eintraege = cache.getIfPresent(vereinnr);

		// Wenn die Map vorhanden ist, direkt im inneren Cache nach dem ergebnisLink
		// suchen
		if (eintraege != null) {
			BerichtData data = eintraege.get(ergebnisLink);
			if (data != null) {
				return data;
			}
		} else {
			// Wenn noch keine Map existiert, lege eine neue an
			eintraege = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, BerichtData> existing = cache.asMap().putIfAbsent(vereinnr, eintraege);
			if (existing != null) {
				eintraege = existing;
			}
		}

		// Wenn noch nicht im Cache: Daten aus der Datenbank laden
		DatabaseService dbService = new DatabaseService(vereinnr);
		BerichtData data = dbService.loadBerichtData(vereinnr, ergebnisLink);

		// Daten im inneren Cache ablegen
		eintraege.put(ergebnisLink, data);

		return data;
	}

	public static void refreshCachedBerichtData(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return;
		}

		// Neue Daten aus der Datenbank laden
		DatabaseService dbService = new DatabaseService(vereinnr);
		BerichtData neueDaten = dbService.loadBerichtData(vereinnr, ergebnisLink);

		// Hol die Map für den Verein oder lege sie an, falls nicht vorhanden
		ConcurrentHashMap<String, BerichtData> eintraege = cache.getIfPresent(vereinnr);
		if (eintraege == null) {
			eintraege = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, BerichtData> existing = cache.asMap().putIfAbsent(vereinnr, eintraege);
			if (existing != null) {
				eintraege = existing;
			}
		}

		// Setze oder ersetze den Wert
		eintraege.put(ergebnisLink, neueDaten);
	}

	public static void clearCacheForVerein(String vereinnr) {
		cache.invalidate(vereinnr);
	}

	/**
	 * Lädt (oder liefert aus dem Cache) den BerichtData für den gegebenen
	 * ergebnisLink.
	 */
	private static int anzahlWordpress(String vereinnr, String ergebnisLink, String name) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return -1;
		}
		return ErgebnisCache.anzahl(vereinnr, "Wordpress", ergebnisLink, 6000, name);
	}

	private static int anzahlFreigabe(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return -1;
		}
		return ErgebnisCache.anzahl(vereinnr, "Freigabe", ergebnisLink, 6000);
	}

	private static int anzahlBlaettle(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return -1;
		}
		return ErgebnisCache.anzahl(vereinnr, "Blaettle", ergebnisLink, 6000);
	}

	/**
	 * Prüft, ob zu einem bestimmten ergebnisLink ein Bild in der Datenbank
	 * vorhanden ist. Ein Bild gilt als vorhanden, wenn: - Der ergebnisLink nicht
	 * leer ist - Es einen Datensatz für den ergebnisLink gibt und - Das Feld "bild"
	 * nicht null und nicht leer ist.
	 *
	 * @param ergebnisLink Der eindeutige Link zum Spiel/Ergebnis.
	 * @return true, wenn ein Bild vorhanden ist, sonst false.
	 */
	public static boolean hasBild(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return false;
		}
		BerichtData data = getCachedBerichtData(vereinnr, ergebnisLink);
		if (data != null && data.getBild() != null && data.getBild().length > 0) {
			// Konvertiere das Byte-Array in einen String (angenommen, es ist UTF-8 kodiert)
			String bildStr = new String(data.getBild(), StandardCharsets.UTF_8);
			return !bildStr.trim().isEmpty();
		}
		return false;
	}

	/**
	 * Prüft, ob zu einem bestimmten ergebnisLink ein Bild in der Datenbank
	 * vorhanden ist. Ein Bild gilt als vorhanden, wenn: - Der ergebnisLink nicht
	 * leer ist - Es einen Datensatz für den ergebnisLink gibt und - Das Feld "bild"
	 * nicht null und nicht leer ist.
	 *
	 * @param ergebnisLink Der eindeutige Link zum Spiel/Ergebnis.
	 * @return true, wenn ein Bild vorhanden ist, sonst false.
	 */
	public static boolean hasHomepage(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return false;
		}
		int data = anzahlWordpress(vereinnr, ergebnisLink, getHomepageStandardEinzel(vereinnr));
		if (data <= 0) {
			// Konvertiere das Byte-Array in einen String (angenommen, es ist UTF-8 kodiert)
			return false;
		}
		return true;
	}

	public static boolean hasFreigabe(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return false;
		}
		int data = anzahlFreigabe(vereinnr, ergebnisLink);
		if (data <= 0) {
			// Konvertiere das Byte-Array in einen String (angenommen, es ist UTF-8 kodiert)
			return false;
		}
		return true;
	}

	public static boolean hasBlaettle(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return false;
		}
		int data = anzahlBlaettle(vereinnr, ergebnisLink);
		if (data <= 0) {
			// Konvertiere das Byte-Array in einen String (angenommen, es ist UTF-8 kodiert)
			return false;
		}
		return true;
	}

	/**
	 * Prüft, ob zu einem bestimmten ergebnisLink ein Bericht vorhanden ist.
	 * EinrefreshCachedBerichtData Bericht gilt als vorhanden, wenn: - Der
	 * ergebnisLink nicht leer ist - Es einen Datensatz für den ergebnisLink gibt
	 * und - Das Feld "berichtText" nicht null und nicht leer ist.
	 *
	 * @param ergebnisLink Der eindeutige Link zum Spiel/Ergebnis.
	 * @return true, wenn ein Bericht vorhanden ist, sonst false.
	 */
	public static boolean hasBericht(String vereinnr, String ergebnisLink) {

		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return false;
		}
		BerichtData data = getCachedBerichtData(vereinnr, ergebnisLink);
		return (data != null && data.getBerichtText() != null && !data.getBerichtText().trim().isEmpty());
	}

	/**
	 * Liefert den Bild-Link als Data-URI, falls vorhanden.
	 *
	 * @param ergebnisLink Der eindeutige Link zum Spiel/Ergebnis.
	 * @return Data-URI-String, wenn Bild vorhanden, sonst null.
	 */
	public static String getBildUrl(String vereinnr, String ergebnisLink) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return null;
		}
		BerichtData data = getCachedBerichtData(vereinnr, ergebnisLink);
		if (data != null && data.getBild() != null && data.getBild().length > 0) {
			String bildStr = Base64.getEncoder().encodeToString(data.getBild());
			if (!bildStr.trim().isEmpty()) {
				// Hier nehmen wir an, dass in der DB der Base64-String (ohne Data-URI-Präfix)
				// gespeichert ist.
				return "data:image/jpeg;base64," + bildStr;
			}
		}
		return null;
	}

	public static String getHtmlText(String textMitUmbruechen) {
		// \n durch <br/> ersetzen
		return textMitUmbruechen.replaceAll("\n", "<br>");
	}

	public static String getLigaJugend(String gruppe) {
		if (gruppe.contains("J 19")) {
			return "Jugend U19";
		} else if (gruppe.contains("J 15")) {
			return "Jugend U15";
		} else if (gruppe.contains("M")) {
			return "Mädchen";
		} else {
			return "";
		}
	}

	public static String getOrt(String vereinnr) {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Ort");
	}

	public static String getVerein(String vereinnr) {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public static String getHomepage(String vereinnr) {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public static String getHomepageStandardEinzel(String vereinnr) {
		return ConfigManager.getConfigValue(vereinnr, "wordpress.domain.standardeinzel");
	}

	public static String getHomepageStandardZusammen(String vereinnr) {
		return ConfigManager.getConfigValue(vereinnr, "wordpress.domain.standardzusammen");
	}

	public static String getZeitungUrl(String vereinnr) {
		return ConfigManager.getConfigValue(vereinnr, "bericht.zeitung.url");
	}

	public static String getProgrammUrl(String vereinnr) {
		return ConfigManager.getConfigValue(vereinnr, "programm.URL");
	}

	public static String bestimmenVereinnr(String ort) {
		DatabaseService dbService = new DatabaseService();
		return dbService.bestimmenVereinnr(ort);
	}

	public static String vereinsnummer(String vereinnr, String mannschaft, String liga) {
		if (liga.startsWith("H") && mannschaft.equals(ConfigManager.getSpielplanVerein(vereinnr))) {
			return mannschaft += " I";
		} else if (liga.startsWith("E") && mannschaft.equals(ConfigManager.getSpielplanVerein(vereinnr))) {
			return mannschaft += " I";
		}
		return mannschaft;
	}

	public static String spielEntscheidung(String ergebnis, boolean istHeim) {

		// Zuerst prüfen, ob Text wie "NA" enthalten ist
		String text = "";
		if (ergebnis.toUpperCase().contains("NA")) {
			text = " (nicht angetreten)";
			ergebnis = ergebnis.replaceAll("(?i)NA", ""); // "NA" entfernen (case-insensitive)
		}

		// Nur die Ziffern rund um den Doppelpunkt extrahieren
		String[] teile = ergebnis.split(":");
		if (teile.length < 2) {
			return "ungültiges Ergebnis";
		}

		// Nur Ziffern extrahieren, falls noch andere Zeichen da sind
		String punkteHeimStr = teile[0].replaceAll("\\D", "");
		String punkteGastStr = teile[1].replaceAll("\\D", "");

		// Falls leer, 0 setzen
		int punkteHeim = punkteHeimStr.isEmpty() ? 0 : Integer.parseInt(punkteHeimStr);
		int punkteGast = punkteGastStr.isEmpty() ? 0 : Integer.parseInt(punkteGastStr);

		int eigenePunkte = istHeim ? punkteHeim : punkteGast;
		int gegnerPunkte = istHeim ? punkteGast : punkteHeim;

		boolean unentschieden = eigenePunkte == gegnerPunkte;
		boolean gewonnen = eigenePunkte > gegnerPunkte;

		String ergebnisText;
		if (unentschieden) {
			ergebnisText = "unentschieden";
		} else if (gewonnen) {
			ergebnisText = "gewonnen";
		} else {
			ergebnisText = "verloren";
		}

		return ergebnisText + text;
	}

	public static String convertQuillClassesToInlineStyles(String html) {
		if (html == null) {
			return null;
		}
		html = mergeParagraphsWithJsoup(html);
		// Text-Ausrichtung
		html = html.replaceAll("class=\"[^\"]*ql-align-center[^\"]*\"", "style=\"text-align:center\"");
		html = html.replaceAll("class=\"[^\"]*ql-align-right[^\"]*\"", "style=\"text-align:right\"");
		html = html.replaceAll("class=\"[^\"]*ql-align-justify[^\"]*\"", "style=\"text-align:justify\"");

		// Schriftgrößen
		html = html.replaceAll("class=\"[^\"]*ql-size-small[^\"]*\"", "style=\"font-size:0.8em\"");
		html = html.replaceAll("class=\"[^\"]*ql-size-large[^\"]*\"", "style=\"font-size:1.5em\"");
		html = html.replaceAll("class=\"[^\"]*ql-size-huge[^\"]*\"", "style=\"font-size:2em\"");

		// Falls mehrere Klassen im selben Tag vorkommen, evtl. zusammenführen
		html = html.replaceAll("\\s*class=\"[^\"]*\"", ""); // Restliche class-Attribute entfernen
		html = html.replaceAll("<p><br /></p>", "");
		html = html.replaceAll("><p> </p>", "");
		html = html.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("<br />", "<br>");
		html = html.replace("\\n <li>", "<li>").replace("</li>\\n", "</li>");

		return html;
	}

	public static String mergeParagraphsWithJsoup(String html) {
		Document doc = Jsoup.parseBodyFragment(html);
		StringBuilder result = new StringBuilder();

		StringBuilder currentBlock = new StringBuilder();
		String lastClass = null;
		String lastStyle = null;

		for (Node node : doc.body().childNodes()) {
			if (node instanceof Element) {
				Element el = (Element) node;

				if (el.tagName().equalsIgnoreCase("p")) {
					String text = el.html().trim();
					String cls = el.attr("class");
					String style = el.attr("style");

					// Leerer Absatz oder <br> nur als Blocktrenner
					if (text.isEmpty() || text.equals("<br>") || text.equals("<br />")) {
						if (currentBlock.length() > 0) {
							result.append("<p").append(formatAttributes(lastClass, lastStyle)).append(">")
									.append(currentBlock).append("</p>");
							currentBlock.setLength(0);
						}
						lastClass = null;
						lastStyle = null;
						continue;
					}

					if ((cls.equals(lastClass) || (cls.isEmpty() && lastClass == null))) {
						if (currentBlock.length() > 0) {
							currentBlock.append("<br>");
						}
						currentBlock.append(text);
					} else {
						if (currentBlock.length() > 0) {
							result.append("<p").append(formatAttributes(lastClass, lastStyle)).append(">")
									.append(currentBlock).append("</p>");
						}
						currentBlock.setLength(0);
						currentBlock.append(text);
						lastClass = cls;
						lastStyle = style;
					}

				} else {
					// Alle anderen Tags (<hr>, <ul>, <figure>, ...) direkt anhängen
					if (currentBlock.length() > 0) {
						result.append("<p").append(formatAttributes(lastClass, lastStyle)).append(">")
								.append(currentBlock).append("</p>");
						currentBlock.setLength(0);
						lastClass = null;
						lastStyle = null;
					}
					result.append(el.outerHtml());
				}
			}
		}

		// Letzten Block hinzufügen
		if (currentBlock.length() > 0) {
			result.append("<p").append(formatAttributes(lastClass, lastStyle)).append(">").append(currentBlock)
					.append("</p>");
		}

		return result.toString();
	}

	private static String formatAttributes(String cls, String style) {
		StringBuilder sb = new StringBuilder();
		if (cls != null && !cls.isEmpty()) {
			sb.append(" class=\"").append(cls).append("\"");
		}
		if (style != null && !style.isEmpty()) {
			sb.append(" style=\"").append(style).append("\"");
		}
		return sb.toString();
	}

}
