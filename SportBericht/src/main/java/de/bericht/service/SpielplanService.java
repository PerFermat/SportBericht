package de.bericht.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.provider.SpielplanProvider;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.WebCache;

public class SpielplanService implements SpielplanProvider {

	private static final List<String> INPUT_PATTERNS = List.of("EEE, MM/dd/yy", "EEE, MM/dd/yyyy", "MM/dd/yy",
			"EEE, dd.MM.yy", "EEE, dd.MM.yyyy", "dd.MM.yy", "MM/dd/yyyy");

	private static final DateTimeFormatter INPUT_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
	private static final DateTimeFormatter INPUT_DATE_UHR = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static final DateTimeFormatter OUTPUT_FORMAT_UHR = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY);
	private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private int fehlercode = 0;

	private ConfigManager config;
	private List<Spiel> spiele = new ArrayList<>();

	public SpielplanService(String vereinnr) throws IOException {
		config = ConfigManager.getInstance();
		String ligaVorhanden = config.getSpielplanLiga(vereinnr);
		String url = config.getSpielplanURL(vereinnr);
		generierenSpielplan(vereinnr, url, ligaVorhanden);
	}

	public SpielplanService(String vereinnr, String url) throws IOException {
		String ligaVorhanden = "Nein";
		generierenSpielplan(vereinnr, url, ligaVorhanden);
	}

	@Override
	public void generierenSpielplan(String vereinnr, String url, String ligaVorhanden)
			throws IOException, NullPointerException {
		fehlercode = -1;
		spiele.clear();

		config = ConfigManager.getInstance();
		int anzahl = 0;

		Document doc = WebCache.getPage(url);
		if (doc == null) {
			return;
		}

		Element table = doc.selectFirst("table");
		if (table == null) {
			return;
		}

		String thirdLine = "";
		if (!"Ja".equals(ligaVorhanden)) {
			thirdLine = ligaErmitteln(doc);
		}

		Elements rows = table.select("table tbody tr");
		String letztesDatum = "";

		int datumIndex = -1;
		int zeitIndex = -1;
		int heimIndex = -1;
		int gastIndex = -1;
		int ligaIndex = -1;
		int punkteIndex = -1;

		Elements headers = table.select("th");
		for (int i = 0; i < headers.size(); i++) {
			String headerText = headers.get(i).text();
			if ("Liga".equals(headerText)) {
				ligaIndex = i;
			} else if ("Datum".equals(headerText)) {
				datumIndex = i;
			} else if ("Zeit".equals(headerText)) {
				zeitIndex = i;
			} else if ("Spiele".equals(headerText)) {
				punkteIndex = i;
			} else if ("Heimmannschaft".equals(headerText)) {
				heimIndex = i;
			} else if ("Gastmannschaft".equals(headerText)) {
				gastIndex = i;
			}
		}

		if (ligaIndex == -1 && "unbekannt".equals(thirdLine)) {
			ligaIndex = heimIndex;
			heimIndex = gastIndex;
			gastIndex = punkteIndex;
			punkteIndex++;
		}

		for (Element row : rows) {
			Elements cols = row.select("td");

			if (cols.size() >= 7 && datumIndex >= 0 && zeitIndex >= 0 && heimIndex >= 0 && gastIndex >= 0
					&& punkteIndex >= 0 && datumIndex < cols.size() && zeitIndex < cols.size()
					&& heimIndex < cols.size() && gastIndex < cols.size() && punkteIndex < cols.size()) {

				String datumRaw = cols.get(datumIndex).text();
				if (datumRaw == null || datumRaw.isBlank()) {
					datumRaw = letztesDatum;
				} else {
					datumRaw = convert(datumRaw);
				}

				String zeit = cols.get(zeitIndex).text();
				String liga;
				if (ligaIndex == -1 || ligaIndex >= cols.size()) {
					liga = thirdLine;
				} else {
					liga = cols.get(ligaIndex).text();
				}

				String heim = BerichtHelper.vereinsnummer(vereinnr, cols.get(heimIndex).text(), liga);
				String gast = BerichtHelper.vereinsnummer(vereinnr, cols.get(gastIndex).text(), liga);
				String ergebnis = cols.get(punkteIndex).text();

				Element ergebnisElement = cols.get(punkteIndex).selectFirst("a");
				String ergebnisLink = (ergebnisElement != null) ? ergebnisElement.absUrl("href") : null;

				letztesDatum = datumRaw;
				fehlercode = 0;

				spiele.add(new TischtennisSpiel(vereinnr, datumRaw, zeit, liga, heim, gast, ergebnis, ergebnisLink));
			}

			if (anzahl++ > 99) {
				return;
			}
		}
	}

	@Override
	public List<Spiel> getSpielplan() {
		return spiele;
	}

	@Override
	public List<Spiel> getSpielplanFreigabe(String vereinnr) {
		List<Spiel> freigegeben = new ArrayList<>();

		for (Spiel spiel : spiele) {
			try {
				if (BerichtHelper.hasFreigabe(vereinnr, spiel.getErgebnisLink())) {
					freigegeben.add(spiel);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		java.util.Collections.sort(freigegeben);
		return freigegeben;
	}

	@Override
	public void generiereVorschauBericht(String vereinnr, String was) {
		String textBody = ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.text");
		StringBuffer sb = new StringBuffer();
		LocalDate heute = LocalDate.now();

		long tageLetzterSatz = 9999;
		int i = 0;
		String aktDatum = "";

		sb.append("<p>");

		for (Spiel spiel : spiele) {
			boolean spielDruck = false;

			if (!spiel.getErgebnis().isEmpty()) {
				spielDruck = false;
			} else if ("HEIM".equals(was)
					&& spiel.getHeim().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
				spielDruck = true;
			} else if ("GAST".equals(was)
					&& spiel.getGast().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
				spielDruck = true;
			} else if ("ALLE".equals(was)) {
				if (spiel.getGast().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
					spielDruck = true;
				} else if (spiel.getHeim().contains(ConfigManager.getConfigValue(vereinnr, "spielplan.Verein"))) {
					spielDruck = true;
				}
			}

			if (spielDruck) {
				String datum = spiel.getDatumAnzeige();
				long tage = tageBisDatum(datum, heute);
				long configTage = Long.parseLong(ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.tage"));

				if (tage <= configTage || tage <= tageLetzterSatz + 1 || i < 2 || aktDatum.equals(datum)) {
					if (!aktDatum.equals(datum)) {
						sb.append("<br><strong>").append(wochentag(datum)).append(", ").append(datum)
								.append(" </strong> <br> ");
					}

					i++;
					tageLetzterSatz = tage;

					sb.append("<strong>   - ").append(spiel.getZeitAnzeige()).append(": ")
							.append(klartextLiga(spiel.getLiga())).append(": </strong> ").append(spiel.getHeim())
							.append(" - ").append(spiel.getGast()).append("<br>");

					aktDatum = datum;
				}
			}
		}

		sb.append("</p>");

		DatabaseService dbService = new DatabaseService(vereinnr);
		dbService.saveBerichtDataOhneHist(vereinnr, "31.12.2999 - Vorschaubericht",
				textBody.replace("[spielliste]", sb.toString()), null, null,
				ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.ueberschrift"));
	}

	private static String klartextLiga(String ligatext) {
		if (ligatext == null || ligatext.isBlank()) {
			return "";
		}

		String[] liga = ligatext.split(" ");
		String ausgabeliga = "";
		int i = 0;

		if (liga.length == 0) {
			return "";
		}

		if ("H".equals(liga[0])) {
			i++;
			ausgabeliga = "Herren";
		} else if ("E".equals(liga[0])) {
			i++;
			ausgabeliga = "Erwachsene";
		} else if ("D".equals(liga[0])) {
			i++;
			ausgabeliga = "Damen";
		} else if ("S40".equals(liga[0])) {
			i++;
			ausgabeliga = "Senioren Ü40";
		} else if ("J".equals(liga[0]) && liga.length > 1) {
			i += 2;
			ausgabeliga = "Jungen U" + liga[1];
		} else if ("M".equals(liga[0]) && liga.length > 1) {
			i += 2;
			ausgabeliga = "Mädchen U" + liga[1];
		}

		if (i < liga.length) {
			if ("KL".equals(liga[i])) {
				ausgabeliga = ausgabeliga + " Kreisliga";
				i++;
			} else if ("BK".equals(liga[i])) {
				i++;
				ausgabeliga = ausgabeliga + " Bezirksklasse";
			} else if ("BL".equals(liga[i])) {
				i++;
				ausgabeliga = ausgabeliga + " Bezirksliga";
			} else if ("LK".equals(liga[i])) {
				i++;
				ausgabeliga = ausgabeliga + " Landesklasse";
			} else if ("LL".equals(liga[i])) {
				i++;
				ausgabeliga = ausgabeliga + " Landesliga";
			} else if ("VL".equals(liga[i])) {
				i++;
				ausgabeliga = ausgabeliga + " Verbandsliga";
			}
		}

		for (int j = i; j < liga.length; j++) {
			ausgabeliga = ausgabeliga + " " + liga[j];
		}

		return ausgabeliga.trim();
	}

	public static long tageBisDatum(String dateStr, LocalDate heute) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		try {
			LocalDate eingabeDatum = LocalDate.parse(dateStr, formatter);
			return ChronoUnit.DAYS.between(heute, eingabeDatum);
		} catch (Exception e) {
			System.err.println("Ungültiges Datumsformat. Bitte verwende TT.MM.JJJJ " + dateStr);
			return Long.MIN_VALUE;
		}
	}

	public static String wochentag(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate datum = LocalDate.parse(dateStr, formatter);
		return datum.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.GERMAN);
	}

	private String ligaErmitteln(Document doc) {
		Element titleElement = doc.selectFirst("dt");
		if (titleElement == null) {
			return "unbekannt";
		}
		return titleElement.text();
	}

	public static String convert(String input) {
		for (String pattern : INPUT_PATTERNS) {
			try {
				DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(pattern, Locale.GERMAN);
				LocalDate date = LocalDate.parse(input, inputFormatter);
				return date.format(OUTPUT_FORMAT);
			} catch (DateTimeParseException ignored) {
			}
		}
		return input;
	}

	public static String convertUhr(String dateStr, String timeStr) {
		LocalDate date = LocalDate.parse(dateStr, INPUT_DATE_UHR);
		LocalTime time = LocalTime.parse(timeStr, INPUT_TIME);
		LocalDateTime dateTime = LocalDateTime.of(date, time);
		ZonedDateTime sourceTime = dateTime.atZone(ZoneId.of("UTC"));
		ZonedDateTime germanTime = sourceTime.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		return germanTime.format(OUTPUT_FORMAT_UHR);
	}

	@Override
	public String ausgabe(List<Spiel> spiele) {
		StringBuilder spieleListe = new StringBuilder();

		for (Spiel spiel : spiele) {
			spieleListe.append(spiel.getDatumAnzeige()).append(" - ");
			spieleListe.append(spiel.getZeitAnzeige()).append(" - ");
			spieleListe.append(spiel.getLiga()).append(" - ");
			spieleListe.append(spiel.getHeim()).append(" - ");
			spieleListe.append(spiel.getGast()).append(" - ");
			spieleListe.append(spiel.getErgebnis()).append(" - ");
			spieleListe.append(spiel.getErgebnisLink()).append("\n");
		}

		return spieleListe.toString();
	}

	public int getFehlercode() {
		return fehlercode;
	}

	public void setFehlercode(int fehlercode) {
		this.fehlercode = fehlercode;
	}
}