package de.bericht.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.VerticalPositionMark;

import de.bericht.service.AnzeigeSpalte;
import de.bericht.service.DatabaseService;
import de.bericht.service.GesamtspielplanEintrag;
import de.bericht.service.SpielerRueckmeldung;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("gesamtspielplanBean")
@ViewScoped
public class GesamtspielplanBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static String verein_prefix;

	private String spalten_definition;
	private static final Pattern SPALTEN_PATTERN = Pattern.compile("^\\s*(sp\\d+)\\s*\\(([^)]*)\\)\\s*:\\s*(.+)$",
			Pattern.CASE_INSENSITIVE);
	private static List<SpaltenDefinition> fixe_spalten = new ArrayList<>();

	private final ConfigManager configManager = ConfigManager.getInstance();
	private final DatabaseService databaseService = new DatabaseService();
	private String vereinnr;
	private String halbserie;

	private final List<AnzeigeSpalte> spalten = new ArrayList<>();
	private final List<String> datumsListe = new ArrayList<>();
	private final Map<String, String> wochentagByDatum = new LinkedHashMap<>();
	private final Map<String, Map<String, List<GesamtspielplanEintrag>>> spieleByDatumUndSpalte = new LinkedHashMap<>();
	private StreamedContent downloadPdf;
	private StreamedContent downloadExcel;
	private final List<String> betreuerNamen = new ArrayList<>();
	private GesamtspielplanEintrag selectedEintrag;
	private String selectedBetreuer;
	private GesamtspielplanEintrag selectedVerfuegbarkeitEintrag;
	private String selectedVerfuegbarkeitSpalteKey;
	private final List<SpielerRueckmeldung> zugesagtSpieler = new ArrayList<>();
	private final List<SpielerRueckmeldung> abgesagtSpieler = new ArrayList<>();
	private final List<SpielerRueckmeldung> offeneSpieler = new ArrayList<>();
	private final List<SpielerRueckmeldung> zusatzZugesagtSpieler = new ArrayList<>();
	private final Map<String, VerfuegbarkeitsSnapshot> verfuegbarkeitBySpielKey = new HashMap<>();

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = "13014";
		}
		verein_prefix = ConfigManager.getSpielplanVerein(vereinnr);
		spalten_definition = ConfigManager.getConfigValue(vereinnr, "gesamtspielplan.spalten");
		fixe_spalten = parseSpaltenDefinition(spalten_definition);
		halbserie = defaultHalbserie();
		ladeGesamtspielplan();
	}

	public void ladeGesamtspielplan() {
		spalten.clear();
		datumsListe.clear();
		wochentagByDatum.clear();
		spieleByDatumUndSpalte.clear();

		Set<String> datumsSet = new LinkedHashSet<>();
		Map<String, BasisSpalte> basisSpalten = new LinkedHashMap<>();
		Map<String, Map<String, List<GesamtspielplanEintrag>>> basisEintraege = new LinkedHashMap<>();

		List<Map<String, String>> rows = databaseService.ladeGesamtspielplanRows(vereinnr, verein_prefix);
		for (Map<String, String> row : rows) {
			String datum = row.get("datum");
			String wochentag = row.get("wochentag");
			if (!passtZurHalbserie(datum)) {
				continue;
			}
			String heim = row.get("heim");
			String gast = row.get("gast");
			String liga = row.get("liga");
			if (heim == null || gast == null || liga == null) {
				continue;
			}

			boolean heimIstVerein = startsWithIgnoreCase(heim, verein_prefix);
			boolean gastIstVerein = startsWithIgnoreCase(gast, verein_prefix);
			if (!heimIstVerein && !gastIstVerein) {
				continue;
			}

			datumsSet.add(datum);
			wochentagByDatum.putIfAbsent(datum, wochentag == null ? "" : wochentag);

			if (heimIstVerein) {
				String basisKeyHeim = liga + "|" + heim;
				basisSpalten.putIfAbsent(basisKeyHeim, new BasisSpalte(basisKeyHeim, liga, heim));
				basisEintraege.computeIfAbsent(datum, d -> new LinkedHashMap<>())
						.computeIfAbsent(basisKeyHeim, s2 -> new ArrayList<>())
						.add(createEintrag(row, datum, liga, heim, gast, true));

			}

			if (gastIstVerein) {
				String basisKeyGast = liga + "|" + gast;
				basisSpalten.putIfAbsent(basisKeyGast, new BasisSpalte(basisKeyGast, liga, gast));
				basisEintraege.computeIfAbsent(datum, d -> new LinkedHashMap<>())
						.computeIfAbsent(basisKeyGast, s2 -> new ArrayList<>())
						.add(createEintrag(row, datum, liga, heim, gast, false));
			}
		}

		for (

		SpaltenDefinition def : fixe_spalten) {
			List<String> sourceKeys = new ArrayList<>();
			for (BasisSpalte basisSpalte : basisSpalten.values()) {
				for (QuellenDefinition quelle : def.quellen()) {
					if (quelle.matches(basisSpalte.liga(), basisSpalte.mannschaft())) {
						sourceKeys.add(basisSpalte.key());
						break;
					}
				}
			}
			spalten.add(new AnzeigeSpalte(def.key(), def.mannschaft(), def.liga(), def.jugend(), sourceKeys));
		}

		datumsListe.addAll(datumsSet);
		datumsListe.sort(Comparator.comparing(this::parseDatumSafe));

		for (String datum : datumsListe) {
			Map<String, List<GesamtspielplanEintrag>> rowMap = new LinkedHashMap<>();
			for (AnzeigeSpalte spalte : spalten) {
				List<GesamtspielplanEintrag> gesammelt = new ArrayList<>();
				Map<String, List<GesamtspielplanEintrag>> datumMap = basisEintraege.getOrDefault(datum, Map.of());
				for (String sourceKey : spalte.getSourceKeys()) {
					gesammelt.addAll(datumMap.getOrDefault(sourceKey, List.of()));
				}
				gesammelt.sort(Comparator
						.comparing(GesamtspielplanEintrag::getZeit, Comparator.nullsLast(String::compareTo))
						.thenComparing(GesamtspielplanEintrag::getGegner, Comparator.nullsLast(String::compareTo)));
				rowMap.put(spalte.getKey(), gesammelt);
			}
			spieleByDatumUndSpalte.put(datum, rowMap);
		}

		ladeVerfuegbarkeitsdaten();

	}

	private GesamtspielplanEintrag createEintrag(Map<String, String> row, String datum, String liga, String heim,
			String gast, boolean hatheim) {
		GesamtspielplanEintrag eintrag = new GesamtspielplanEintrag();
		eintrag.setUniqueKey(row.get("unique_key"));
		eintrag.setDatum(datum);
		eintrag.setZeit(row.get("zeit"));
		eintrag.setLiga(liga);
		eintrag.setHeim(heim);
		eintrag.setGast(gast);
		eintrag.setHatheim(hatheim);
		eintrag.setHeimOderAuswaerts(hatheim ? "H" : "A");
		eintrag.setGegner(hatheim ? gast : heim);
		eintrag.setErgebnis(row.get("ergebnis"));
		eintrag.setJugend(istJugendLiga(liga));
		eintrag.setBetreuer(row.get("betreuer"));
		eintrag.setBestaetigt(parseBooleanObj(row.get("bestaetigt")));
		eintrag.setKommentar(row.get("kommentar"));
		eintrag.setErgebnisFarbeClass(bestimmeErgebnisFarbe(eintrag));
		return eintrag;
	}

	private void ladeVerfuegbarkeitsdaten() {
		verfuegbarkeitBySpielKey.clear();
		Map<String, List<AufstellungSpielerInfo>> aufstellungByLiga = ladeAufstellungByLiga();
		VerfuegbarkeitsDaten verfuegbarkeitsDaten = ladeVerfuegbarkeitByDatumZeit();

		for (Map<String, List<GesamtspielplanEintrag>> bySpalte : spieleByDatumUndSpalte.values()) {
			for (List<GesamtspielplanEintrag> eintraege : bySpalte.values()) {
				for (GesamtspielplanEintrag eintrag : eintraege) {
					eintrag.setZukunftsspiel(isZukunftsspiel(eintrag));
					eintrag.setVerfuegbarJaAnzahl(0);
					eintrag.setVerfuegbarNeinAnzahl(0);
					if (eintrag.isJugend() || !eintrag.isZukunftsspiel()) {
						continue;
					}
					VerfuegbarkeitsSnapshot snapshot = baueSnapshot(eintrag, aufstellungByLiga, verfuegbarkeitsDaten);
					verfuegbarkeitBySpielKey.put(buildVerfuegbarkeitsKey(eintrag), snapshot);
					eintrag.setVerfuegbarJaAnzahl(snapshot.zugesagt().size());
					eintrag.setVerfuegbarNeinAnzahl(snapshot.abgesagt().size());
				}
			}
		}
	}

	private Map<String, List<AufstellungSpielerInfo>> ladeAufstellungByLiga() {
		Map<String, List<AufstellungSpielerInfo>> result = new HashMap<>();
		for (Map<String, String> row : databaseService.ladeAufstellungLigaRangName(vereinnr)) {
			String liga = normalisiereLigaSchluessel(row.get("mannschaft"));
			String rang = row.get("rang");
			String name = row.get("name");
			if (liga == null || liga.isBlank() || rang == null || rang.isBlank() || name == null || name.isBlank()) {
				continue;
			}
			result.computeIfAbsent(liga, k -> new ArrayList<>()).add(new AufstellungSpielerInfo(name, rang));
		}

		for (List<AufstellungSpielerInfo> spieler : result.values()) {
			spieler.sort(Comparator.comparingInt((AufstellungSpielerInfo s) -> parseRangHauptwert(s.rang()))
					.thenComparing(AufstellungSpielerInfo::rang).thenComparing(AufstellungSpielerInfo::name));
		}
		return result;
	}

	private VerfuegbarkeitsDaten ladeVerfuegbarkeitByDatumZeit() {

		Map<String, Map<String, VerfuegbarkeitsRueckmeldung>> result = new HashMap<>();
		List<VerfuegbarkeitsAntwort> antworten = new ArrayList<>();

		for (Map<String, String> row : databaseService.ladeVerfuegbarkeitMitKommentar(vereinnr)) {
			String datumZeitKey = buildDatumZeitKey(row.get("datum"), row.get("uhrzeit"));
			String name = row.get("name");
			if (name == null || name.isBlank()) {
				continue;
			}
			String datum = row.get("datum");
			String normalisierteZeit = normalizeZeitText(row.get("uhrzeit"));
			if (datum != null && !datum.isBlank()) {
				antworten.add(new VerfuegbarkeitsAntwort(datum, normalisierteZeit, name, row.get("verfuegbarkeit"),
						row.get("kommentar")));
			}

			result.computeIfAbsent(datumZeitKey, k -> new HashMap<>()).put(name,
					new VerfuegbarkeitsRueckmeldung(row.get("verfuegbarkeit"), row.get("kommentar")));
		}
		return new VerfuegbarkeitsDaten(result, antworten);

	}

	private VerfuegbarkeitsSnapshot baueSnapshot(GesamtspielplanEintrag eintrag,
			Map<String, List<AufstellungSpielerInfo>> aufstellungByLiga, VerfuegbarkeitsDaten verfuegbarkeitsDaten) {

		String teamName = eintrag.isHatheim() ? eintrag.getHeim() : eintrag.getGast();
		int teamNummer = extrahiereRoemischeTeamNummer(teamName);
		String ligaSchluessel = normalisiereLigaSchluessel(eintrag.getLiga());
		List<AufstellungSpielerInfo> ligaspieler = aufstellungByLiga.getOrDefault(ligaSchluessel, List.of());
		List<AufstellungSpielerInfo> mannschaftsspieler = ligaspieler.stream()
				.filter(spieler -> parseRangHauptwert(spieler.rang()) == teamNummer).collect(Collectors.toList());

		Map<String, VerfuegbarkeitsRueckmeldung> verfMap = verfuegbarkeitsDaten.verfuegbarkeitByDatumZeit()
				.getOrDefault(buildDatumZeitKey(eintrag.getDatum(), eintrag.getZeit()), Map.of());

		List<SpielerRueckmeldung> zugesagt = new ArrayList<>();
		List<SpielerRueckmeldung> abgesagt = new ArrayList<>();
		List<SpielerRueckmeldung> offen = new ArrayList<>();
		List<SpielerRueckmeldung> zusatzZugesagt = new ArrayList<>();

		for (AufstellungSpielerInfo spieler : mannschaftsspieler) {
			VerfuegbarkeitsRueckmeldung rueckmeldung = verfMap.get(spieler.name());
			if (rueckmeldung == null || istLeer(rueckmeldung.verfuegbarkeit())) {
				offen.add(new SpielerRueckmeldung(spieler.name(), null, null, spieler.rang()));
				continue;
			}

			String verf = rueckmeldung.verfuegbarkeit().trim();
			SpielerRueckmeldung anzeige = new SpielerRueckmeldung(spieler.name(), verf, rueckmeldung.kommentar(),
					spieler.rang());
			if (startsWithIgnoreCase(verf, "Ja")) {
				zugesagt.add(anzeige);
			} else if (startsWithIgnoreCase(verf, "Nein")) {
				abgesagt.add(anzeige);
			}
		}

		LocalDate spielDatum = parseDatumSafe(eintrag.getDatum());
		LocalTime spielZeit = parseZeitSafe(eintrag.getZeit());
		List<AufstellungSpielerInfo> zusatzSpieler = ligaspieler.stream()
				.filter(spieler -> parseRangHauptwert(spieler.rang()) > teamNummer).collect(Collectors.toList());
		for (AufstellungSpielerInfo spieler : zusatzSpieler) {
			VerfuegbarkeitsAntwort passendeAntwort = findePassendeJaAntwort(verfuegbarkeitsDaten.antworten(),
					spieler.name(), spielDatum, spielZeit);
			if (passendeAntwort == null) {
				continue;
			}
			zusatzZugesagt.add(new SpielerRueckmeldung(spieler.name(), passendeAntwort.verfuegbarkeit(),
					passendeAntwort.kommentar(), spieler.rang()));
		}
		zusatzZugesagt.sort(Comparator.comparingInt((SpielerRueckmeldung s) -> parseRangHauptwert(s.getRang()))
				.thenComparing(SpielerRueckmeldung::getRang).thenComparing(SpielerRueckmeldung::getName));

		return new VerfuegbarkeitsSnapshot(zugesagt, abgesagt, zusatzZugesagt, offen);
	}

	private VerfuegbarkeitsAntwort findePassendeJaAntwort(List<VerfuegbarkeitsAntwort> antworten, String spielerName,
			LocalDate spielDatum, LocalTime spielZeit) {
		if (spielerName == null || spielerName.isBlank()) {
			return null;
		}
		VerfuegbarkeitsAntwort kandidat = null;
		long bestDelta = Long.MAX_VALUE;
		for (VerfuegbarkeitsAntwort antwort : antworten) {
			if (!spielerName.equals(antwort.name()) || !startsWithIgnoreCase(antwort.verfuegbarkeit(), "Ja")) {
				continue;
			}
			LocalDate antwortDatum = parseDatumSafe(antwort.datum());
			if (!spielDatum.equals(antwortDatum)) {
				continue;
			}
			LocalTime antwortZeit = parseZeitSafe(antwort.zeit());
			long deltaMinutes = Math.abs(ChronoUnit.MINUTES.between(spielZeit, antwortZeit));
			if (deltaMinutes > 240) {
				continue;
			}
			if (deltaMinutes < bestDelta) {
				bestDelta = deltaMinutes;
				kandidat = antwort;
			}
		}
		return kandidat;
	}

	private String normalisiereLigaSchluessel(String liga) {
		if (liga == null) {
			return "";
		}
		String trimmed = liga.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		int firstSpace = trimmed.indexOf(' ');
		String basis = firstSpace >= 0 ? trimmed.substring(0, firstSpace) : trimmed;
		return basis.trim().toUpperCase();
	}

	private boolean isZukunftsspiel(GesamtspielplanEintrag eintrag) {
		LocalDate datum = parseDatumSafe(eintrag.getDatum());
		LocalTime zeit = parseZeitSafe(eintrag.getZeit());
		if (datum.getYear() == 2999) {
			return false;
		}
		return LocalDateTime.of(datum, zeit).isAfter(LocalDateTime.now());
	}

	private LocalTime parseZeitSafe(String zeit) {
		String normalisierteZeit = normalizeZeitText(zeit);
		if (normalisierteZeit == null || normalisierteZeit.isBlank()) {
			return LocalTime.MIN;
		}
		try {
			return LocalTime.parse(normalisierteZeit, DateTimeFormatter.ofPattern("H:mm"));
		} catch (DateTimeParseException ex) {
			try {
				return LocalTime.parse(normalisierteZeit, DateTimeFormatter.ofPattern("HH:mm"));
			} catch (DateTimeParseException ignored) {
				return LocalTime.MIN;
			}
		}
	}

	private String normalizeZeitText(String zeit) {
		if (zeit == null) {
			return "";
		}
		String trimmed = zeit.trim();
		if (trimmed.length() > 5) {
			trimmed = trimmed.substring(0, 5);
		}
		return trimmed;
	}

	private int parseRangHauptwert(String rang) {
		if (rang == null || rang.isBlank()) {
			return Integer.MAX_VALUE;
		}
		String[] teile = rang.trim().split("\\.", 2);
		try {
			return Integer.parseInt(teile[0]);
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	private int extrahiereRoemischeTeamNummer(String teamName) {
		if (teamName == null || teamName.isBlank()) {
			return 1;
		}
		String[] teile = teamName.trim().split("\\s+");
		if (teile.length == 0) {
			return 1;
		}
		String roman = teile[teile.length - 1].trim().toUpperCase();
		int wert = romanToInt(roman);
		return wert > 0 ? wert : 1;
	}

	private int romanToInt(String roman) {
		if (roman == null || roman.isBlank()) {
			return -1;
		}
		Map<Character, Integer> map = Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100);
		int sum = 0;
		int prev = 0;
		for (int i = roman.length() - 1; i >= 0; i--) {
			int current = map.getOrDefault(roman.charAt(i), -1);
			if (current < 0) {
				return -1;
			}
			if (current < prev) {
				sum -= current;
			} else {
				sum += current;
			}
			prev = current;
		}
		return sum;
	}

	private String buildDatumZeitKey(String datum, String zeit) {
		return (datum == null ? "" : datum) + "|" + normalizeZeitText(zeit);
	}

	private String buildVerfuegbarkeitsKey(GesamtspielplanEintrag eintrag) {
		String teamName = eintrag.isHatheim() ? eintrag.getHeim() : eintrag.getGast();
		return (eintrag.getLiga() == null ? "" : eintrag.getLiga()) + "|"
				+ buildDatumZeitKey(eintrag.getDatum(), eintrag.getZeit()) + "|" + (teamName == null ? "" : teamName);
	}

	private boolean startsWithIgnoreCase(String value, String prefix) {
		if (value == null || prefix == null) {
			return false;
		}
		return value.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private boolean istLeer(String value) {
		return value == null || value.trim().isEmpty();
	}

	public void openVerfuegbarkeitDialog(GesamtspielplanEintrag eintrag) {
		if (eintrag == null || eintrag.isJugend() || !eintrag.isZukunftsspiel()) {
			selectedVerfuegbarkeitSpalteKey = null;
			return;
		}
		selectedVerfuegbarkeitEintrag = eintrag;
		selectedVerfuegbarkeitSpalteKey = findeSpaltenKeyFuerEintrag(eintrag);
		VerfuegbarkeitsSnapshot snapshot = verfuegbarkeitBySpielKey.getOrDefault(buildVerfuegbarkeitsKey(eintrag),
				new VerfuegbarkeitsSnapshot(List.of(), List.of(), List.of(), List.of()));
		zugesagtSpieler.clear();
		zugesagtSpieler.addAll(snapshot.zugesagt());
		abgesagtSpieler.clear();
		abgesagtSpieler.addAll(snapshot.abgesagt());
		offeneSpieler.clear();
		offeneSpieler.addAll(snapshot.offen());
		zusatzZugesagtSpieler.clear();
		zusatzZugesagtSpieler.addAll(snapshot.zusatzZugesagt());

	}

	private String findeSpaltenKeyFuerEintrag(GesamtspielplanEintrag eintrag) {
		if (eintrag == null) {
			return null;
		}
		String liga = eintrag.getLiga();
		String team = eintrag.isHatheim() ? eintrag.getHeim() : eintrag.getGast();
		if (liga == null || team == null) {
			return null;
		}
		for (AnzeigeSpalte spalte : spalten) {
			for (String sourceKey : spalte.getSourceKeys()) {
				if ((liga + "|" + team).equals(sourceKey)) {
					return spalte.getKey();
				}
			}
		}
		return null;
	}

	public List<SpielerRueckmeldung> getZugesagtSpieler() {
		return zugesagtSpieler;
	}

	public List<SpielerRueckmeldung> getZusatzZugesagtSpieler() {
		return zusatzZugesagtSpieler;
	}

	public List<SpielerRueckmeldung> getAbgesagtSpieler() {
		return abgesagtSpieler;
	}

	public List<SpielerRueckmeldung> getOffeneSpieler() {
		return offeneSpieler;
	}

	public String getSelectedVerfuegbarkeitSpalteKey() {
		return selectedVerfuegbarkeitSpalteKey;
	}

	public GesamtspielplanEintrag getSelectedVerfuegbarkeitEintrag() {
		return selectedVerfuegbarkeitEintrag;
	}

	private StreamedContent buildPdfDownload() {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A4.rotate(), 10, 10, 10, 10);
			PdfWriter.getInstance(document, out);
			document.open();
			document.add(new Paragraph("Gesamtspielplan " + halbserie + " - " + getVerein(),
					new Font(Font.HELVETICA, 14, Font.BOLD)));
			document.add(new Paragraph(" "));

			PdfPTable table = new PdfPTable(getGesamtColspan());
			table.setWidthPercentage(100f);
			table.setWidths(buildPdfColumnWidths());

			addPdfHeaderCell(table, "Datum", 1, 2);
			for (AnzeigeSpalte spalte : spalten) {
				int colspan = spalte.isJugend() ? 2 : 1;
				addPdfHeaderCell(table, spalte.getMannschaft() + "\n" + spalte.getLiga(), colspan, 1);
			}
			for (AnzeigeSpalte spalte : spalten) {
				addPdfHeaderCell(table, "Uhrzeit/Gegner", 1, 1);

				if (spalte.isJugend()) {
					addPdfHeaderCell(table, "Betreuer", 1, 1);
				}
			}

			for (int i = 0; i < datumsListe.size(); i++) {
				String datum = datumsListe.get(i);
				boolean neueWoche = isNeueWoche(i);
				addPdfDataCell(table, getDatumMitWochentag(datum), new Font(Font.HELVETICA, 10), neueWoche);
				for (AnzeigeSpalte spalte : spalten) {
					addPdfDataCell(table, formatEintraegeText(datum, spalte), new Font(Font.HELVETICA, 9), neueWoche);
					if (spalte.isJugend()) {
						addPdfDataCell(table, formatBetreuerText(datum, spalte), new Font(Font.HELVETICA, 7),
								neueWoche);
					}
				}
			}

			document.add(table);
			document.close();
			LocalDate heute = LocalDate.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			String datum = heute.format(formatter);

			return DefaultStreamedContent.builder().name("gesamtspielplan-" + datum + ".pdf")
					.contentType("application/pdf").stream(() -> new ByteArrayInputStream(out.toByteArray())).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private StreamedContent buildExcelDownload() {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Gesamtspielplan");

			org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
			titleFont.setBold(true);
			titleFont.setFontHeightInPoints((short) 14);
			titleFont.setColor(IndexedColors.WHITE.getIndex());

			org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());

			org.apache.poi.ss.usermodel.Font subHeaderFont = workbook.createFont();
			subHeaderFont.setBold(true);

			CellStyle titleStyle = workbook.createCellStyle();
			titleStyle.setAlignment(HorizontalAlignment.LEFT);
			titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
			titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			titleStyle.setFont(titleFont);

			CellStyle headerStyle = workbook.createCellStyle();
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			headerStyle.setVerticalAlignment(VerticalAlignment.TOP);
			headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setFont(headerFont);
			setStandardBorder(headerStyle);
			headerStyle.setWrapText(true); // Zeilenumbruch aktivieren

			CellStyle subHeaderStyle = workbook.createCellStyle();
			subHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
			subHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			subHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			subHeaderStyle.setFont(subHeaderFont);
			setStandardBorder(subHeaderStyle);

			CellStyle dateStyle = workbook.createCellStyle();
			dateStyle.setVerticalAlignment(VerticalAlignment.TOP);
			dateStyle.setWrapText(true);
			dateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			dateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			setStandardBorder(dateStyle);

			CellStyle centerDataStyle = workbook.createCellStyle();
			centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
			centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			setStandardBorder(centerDataStyle);

			CellStyle topWrapDataStyle = workbook.createCellStyle();
			topWrapDataStyle.setVerticalAlignment(VerticalAlignment.TOP);
			topWrapDataStyle.setWrapText(true);
			setStandardBorder(topWrapDataStyle);

			CellStyle textDataStyle = workbook.createCellStyle();
			textDataStyle.setVerticalAlignment(VerticalAlignment.TOP);
			textDataStyle.setWrapText(true);
			setStandardBorder(textDataStyle);

			CellStyle spielZeitStyle = workbook.createCellStyle();
			spielZeitStyle.cloneStyleFrom(textDataStyle);
			spielZeitStyle.setBorderRight(BorderStyle.NONE);
			spielZeitStyle.setBorderBottom(BorderStyle.NONE);

			CellStyle spielHaStyle = workbook.createCellStyle();
			spielHaStyle.cloneStyleFrom(centerDataStyle);
			spielHaStyle.setAlignment(HorizontalAlignment.RIGHT);
			spielHaStyle.setBorderLeft(BorderStyle.NONE);
			spielHaStyle.setBorderBottom(BorderStyle.NONE);

			CellStyle spielGegnerLeftStyle = workbook.createCellStyle();
			spielGegnerLeftStyle.cloneStyleFrom(textDataStyle);
			spielGegnerLeftStyle.setBorderTop(BorderStyle.NONE);
			spielGegnerLeftStyle.setBorderRight(BorderStyle.NONE);

			CellStyle spielGegnerRightStyle = workbook.createCellStyle();
			spielGegnerRightStyle.cloneStyleFrom(textDataStyle);
			spielGegnerRightStyle.setBorderTop(BorderStyle.NONE);
			spielGegnerRightStyle.setBorderLeft(BorderStyle.NONE);

			int rowIndex = 0;
			Row title = sheet.createRow(rowIndex++);
			title.setHeightInPoints(24);
			Cell titleCell = title.createCell(0);
			titleCell.setCellValue("Gesamtspielplan - " + getVerein());
			titleCell.setCellStyle(titleStyle);
			rowIndex++;

			int dateCol = 0;
			List<Integer> startCols = new ArrayList<>();
			int runningCol = 1;
			for (AnzeigeSpalte spalte : spalten) {
				startCols.add(runningCol);
				runningCol += spalte.isJugend() ? 3 : 2;
			}

			Row headerRow = sheet.createRow(rowIndex++);
			headerRow.setHeightInPoints(22);
			Cell dateHeaderCell = headerRow.createCell(dateCol);
			dateHeaderCell.setCellValue("Datum");
			dateHeaderCell.setCellStyle(headerStyle);
			for (int i = 0; i < spalten.size(); i++) {
				AnzeigeSpalte spalte = spalten.get(i);
				int startCol = startCols.get(i);
				int endCol = startCol + (spalte.isJugend() ? 2 : 1);
				Cell cell = headerRow.createCell(startCol);

				cell.setCellValue(spalte.getMannschaft() + "\n" + spalte.getLiga());
				cell.setCellStyle(headerStyle);
				for (int c = startCol + 1; c <= endCol; c++) {
					headerRow.createCell(c).setCellStyle(headerStyle);
				}
				if (endCol > startCol) {
					sheet.addMergedRegion(
							new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), startCol, endCol));
				}
			}
			headerRow.setHeight((short) -1);
			sheet.addMergedRegion(new CellRangeAddress(title.getRowNum(), title.getRowNum(), 0, runningCol - 1));

			Row subHeaderRow = sheet.createRow(rowIndex++);
			subHeaderRow.setHeightInPoints(20);
			Cell subDateHeaderCell = subHeaderRow.createCell(dateCol);
			subDateHeaderCell.setCellValue("Datum");
			subDateHeaderCell.setCellStyle(subHeaderStyle);
			for (int i = 0; i < spalten.size(); i++) {
				AnzeigeSpalte spalte = spalten.get(i);
				int startCol = startCols.get(i);
				Cell zeitCell = subHeaderRow.createCell(startCol);
				zeitCell.setCellValue("Zeit");
				zeitCell.setCellStyle(subHeaderStyle);
				Cell haHeaderCell = subHeaderRow.createCell(startCol + 1);
				haHeaderCell.setCellValue("Gegner");
				haHeaderCell.setCellStyle(subHeaderStyle);
				if (spalte.isJugend()) {
					Cell betreuerHeaderCell = subHeaderRow.createCell(startCol + 2);
					betreuerHeaderCell.setCellValue("Betreuer");
					betreuerHeaderCell.setCellStyle(subHeaderStyle);
				}
			}

			for (String datum : datumsListe) {
				int maxEintraege = 0;
				for (AnzeigeSpalte spalte : spalten) {
					maxEintraege = Math.max(maxEintraege, getEintraege(datum, spalte).size());
				}
				if (maxEintraege == 0) {
					maxEintraege = 1;
				}
				int blockRows = maxEintraege * 2;
				int blockStart = rowIndex;
				int blockEnd = blockStart + blockRows - 1;

				for (int r = blockStart; r <= blockEnd; r++) {
					sheet.createRow(r);
				}

				Cell dateCell = sheet.getRow(blockStart).createCell(dateCol);
				dateCell.setCellValue(getDatumMitWochentag(datum));
				dateCell.setCellStyle(dateStyle);
				for (int r = blockStart + 1; r <= blockEnd; r++) {
					sheet.getRow(r).createCell(dateCol).setCellStyle(dateStyle);
				}
				if (blockRows > 1) {
					sheet.addMergedRegion(new CellRangeAddress(blockStart, blockEnd, dateCol, dateCol));
				}

				for (int i = 0; i < spalten.size(); i++) {
					AnzeigeSpalte spalte = spalten.get(i);
					int startCol = startCols.get(i);
					List<GesamtspielplanEintrag> eintraege = getEintraege(datum, spalte);

					for (int entryIndex = 0; entryIndex < maxEintraege; entryIndex++) {
						GesamtspielplanEintrag eintrag = entryIndex < eintraege.size() ? eintraege.get(entryIndex)
								: null;
						int topRow = blockStart + (entryIndex * 2);
						int bottomRow = topRow + 1;
						Row rowTop = sheet.getRow(topRow);
						Row rowBottom = sheet.getRow(bottomRow);

						Cell kopfCell = rowTop.createCell(startCol);
						kopfCell.setCellValue(eintrag == null ? "" : eintrag.getAnzeigeKopf());
						kopfCell.setCellStyle(spielZeitStyle);
						Cell haCell = rowTop.createCell(startCol + 1);
						haCell.setCellValue(eintrag == null ? "" : eintrag.getHeimOderAuswaerts());
						haCell.setCellStyle(spielHaStyle);

						Cell gegnerCell = rowBottom.createCell(startCol);
						gegnerCell.setCellValue(eintrag == null ? "" : eintrag.getGegner());
						gegnerCell.setCellStyle(spielGegnerLeftStyle);
						Cell mergedGegnerCell = rowBottom.createCell(startCol + 1);
						mergedGegnerCell.setCellStyle(spielGegnerRightStyle);
						sheet.addMergedRegion(new CellRangeAddress(bottomRow, bottomRow, startCol, startCol + 1));

						if (spalte.isJugend()) {
							Cell betreuerCell = rowTop.createCell(startCol + 2);
							betreuerCell.setCellValue(
									eintrag == null || eintrag.getBetreuer() == null ? "" : eintrag.getBetreuer());
							betreuerCell.setCellStyle(topWrapDataStyle);
							Cell mergedBetreuerCell = rowBottom.createCell(startCol + 2);
							mergedBetreuerCell.setCellStyle(topWrapDataStyle);
							sheet.addMergedRegion(new CellRangeAddress(topRow, bottomRow, startCol + 2, startCol + 2));
						}
					}
				}

				rowIndex = blockEnd + 1;
			}

			for (int i = 0; i < runningCol; i++) {
				sheet.autoSizeColumn(i);
				sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 600, 12000));
			}

			workbook.write(out);
			LocalDate heute = LocalDate.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			String datum = heute.format(formatter);

			return DefaultStreamedContent.builder().name("gesamtspielplan-" + datum + ".xlsx")
					.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
					.stream(() -> new ByteArrayInputStream(out.toByteArray())).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void setStandardBorder(CellStyle style) {
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
	}

	private void addPdfHeaderCell(PdfPTable table, String text) {
		addPdfHeaderCell(table, text, 1, 1);
	}

	private void addPdfHeaderCell(PdfPTable table, String text, int colspan, int rowspan) {
		PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 9, Font.BOLD)));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setColspan(colspan);
		cell.setRowspan(rowspan);

		table.addCell(cell);
	}

	private void addPdfDataCell(PdfPTable table, String text, Font font, boolean neueWoche) {
		PdfPCell cell = new PdfPCell(buildPdfSpielText(text, font));
		cell.setBorderWidthTop(neueWoche ? 2f : 0.5f);
		table.addCell(cell);
	}

	private Phrase buildPdfSpielText(String text, Font font) {
		String value = text == null ? "" : text;
		if (!value.contains("\u0001")) {
			return new Phrase(value, font);
		}

		Phrase phrase = new Phrase();
		String[] zeilen = value.split("\\n", -1);
		for (int i = 0; i < zeilen.length; i++) {
			String zeile = zeilen[i];
			if (zeile.indexOf('\u0001') >= 0) {
				int markerPos = zeile.indexOf('\u0001');
				String links = zeile.substring(0, markerPos);
				String rechts = zeile.substring(markerPos + 1);
				phrase.add(new Phrase(links, font));
				phrase.add(new Chunk(new VerticalPositionMark()));
				phrase.add(new Phrase(rechts, font));
			} else {
				phrase.add(new Phrase(zeile, font));
			}
			if (i < zeilen.length - 1) {
				phrase.add(new Phrase("\n", font));
			}
		}
		return phrase;
	}

	private float[] buildPdfColumnWidths() {
		List<Float> widths = new ArrayList<>();
		widths.add(1f);
		for (AnzeigeSpalte spalte : spalten) {
			widths.add(1f);
			if (spalte.isJugend()) {
				widths.add(0.4f);
			}
		}
		float[] result = new float[widths.size()];
		for (int i = 0; i < widths.size(); i++) {
			result[i] = widths.get(i);
		}
		return result;
	}

	private String formatEintraegeText(String datum, AnzeigeSpalte spalte) {
		List<GesamtspielplanEintrag> eintraege = getEintraege(datum, spalte);
		if (eintraege.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (GesamtspielplanEintrag eintrag : eintraege) {
			if (sb.length() > 0) {
				sb.append("\n\n");
			}
			sb.append(eintrag.getZeit()).append("\u0001").append(eintrag.getHeimOderAuswaerts()).append("\n")
					.append(eintrag.getGegner());
		}
		return sb.toString();
	}

	private String formatBetreuerText(String datum, AnzeigeSpalte spalte) {
		List<GesamtspielplanEintrag> eintraege = getEintraege(datum, spalte);
		if (eintraege.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (GesamtspielplanEintrag eintrag : eintraege) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(eintrag.getBetreuer() == null ? "" : eintrag.getBetreuer());
		}
		return sb.toString();
	}

	public String getDatumMitWochentag(String datum) {
		String wochentag = wochentagByDatum.get(datum);
		if (wochentag == null || wochentag.isBlank()) {
			return datum;
		}
		return wochentag + " " + datum;
	}

	private static List<SpaltenDefinition> parseSpaltenDefinition(String input) {
		List<SpaltenDefinition> result = new ArrayList<>();
		if (input == null || input.isBlank()) {
			return result;
		}

		for (String rawSpalte : input.split("\\|")) {
			Matcher matcher = SPALTEN_PATTERN.matcher(rawSpalte.trim());
			if (!matcher.matches()) {
				continue;
			}

			String spaltenKey = matcher.group(1).trim();
			String anzeigeLiga = matcher.group(2).trim();
			String quellenTeil = matcher.group(3).trim();

			List<QuellenDefinition> quellen = new ArrayList<>();
			String anzeigeMannschaft = "";

			for (String rawQuelle : quellenTeil.split(";")) {
				String quelleText = rawQuelle.trim();
				if (quelleText.isBlank()) {
					continue;
				}
				String[] ligaUndTeam = quelleText.split("/", 2);
				if (ligaUndTeam.length != 2) {
					continue;
				}
				String liga = ligaUndTeam[0].trim();
				String team = ligaUndTeam[1].trim();
				if (liga.isBlank() || team.isBlank()) {
					continue;
				}
				if (anzeigeMannschaft.isBlank()) {
					anzeigeMannschaft = team;
				}
				quellen.add(new QuellenDefinition(liga, team));
			}

			if (!quellen.isEmpty()) {
				boolean jugend = anzeigeLiga.toUpperCase().startsWith("J");
				result.add(new SpaltenDefinition(spaltenKey, anzeigeMannschaft, anzeigeLiga, jugend, quellen));
			}
		}

		return result;
	}

	private boolean passtZurHalbserie(String datum) {
		LocalDate d = parseDatumSafe(datum);
		if (d.getYear() == 2999) {
			return true;
		}
		boolean vorrunde = Month.JULY.getValue() <= d.getMonthValue();
		if ("Rückrunde".equalsIgnoreCase(halbserie)) {
			return !vorrunde;
		}
		return vorrunde;
	}

	private String defaultHalbserie() {
		int month = LocalDate.now().getMonthValue();
		return month >= 7 ? "Vorrunde" : "Rückrunde";
	}

	public void saveBetreuer(GesamtspielplanEintrag eintrag) {
		if (eintrag == null || eintrag.getUniqueKey() == null || eintrag.getUniqueKey().isBlank()) {
			return;
		}
		databaseService.speichereBetreuerOhneBestaetigung(eintrag.getUniqueKey(), eintrag.getBetreuer());
	}

	public void openBetreuerDialog(GesamtspielplanEintrag eintrag) {
		if (eintrag == null) {
			return;
		}
		selectedEintrag = eintrag;
		selectedBetreuer = eintrag.getBetreuer();
		ladeBetreuerNamenFallsNoetig();
	}

	public List<String> completeBetreuer(String query) {
		ladeBetreuerNamenFallsNoetig();
		if (query == null || query.isBlank()) {
			return betreuerNamen;
		}
		String normalizedQuery = query.trim().toLowerCase();
		return betreuerNamen.stream().filter(name -> name.toLowerCase().contains(normalizedQuery))
				.collect(Collectors.toList());
	}

	public void saveSelectedBetreuer() {
		if (selectedEintrag == null) {
			return;
		}
		selectedEintrag.setBetreuer(selectedBetreuer);
		saveBetreuer(selectedEintrag);
		if (selectedBetreuer != null && !selectedBetreuer.isBlank() && !betreuerNamen.contains(selectedBetreuer)) {
			betreuerNamen.add(selectedBetreuer);
			Collections.sort(betreuerNamen);
		}
	}

	private void ladeBetreuerNamenFallsNoetig() {
		if (!betreuerNamen.isEmpty()) {
			return;
		}
		betreuerNamen.addAll(databaseService.ladeBetreuerNamenAusAdressliste(vereinnr));
	}

	private String baueSpaltenKey(String liga, String mannschaft) {
		return (liga == null ? "" : liga) + "|" + (mannschaft == null ? "" : mannschaft);
	}

	private boolean isHattenhofenTeam(String team) {
		return team != null && team.trim().startsWith(verein_prefix);
	}

	private List<String> findeHattenhofenTeams(String heim, String gast) {
		if (isHattenhofenTeam(heim) && isHattenhofenTeam(gast) && !heim.equals(gast)) {
			List<String> teams = new ArrayList<>();
			teams.add(heim);
			teams.add(gast);
			return teams;
		}

		if (isHattenhofenTeam(heim)) {
			return Collections.singletonList(heim);
		}
		if (isHattenhofenTeam(gast)) {
			return Collections.singletonList(gast);
		}
		return Collections.emptyList();
	}

	private boolean istJugendLiga(String liga) {
		return liga != null && liga.trim().toUpperCase().startsWith("J");
	}

	private String bestimmeErgebnisFarbe(GesamtspielplanEintrag eintrag) {
		if (eintrag == null || eintrag.getErgebnis() == null || eintrag.getErgebnis().isBlank()) {
			return "";
		}

		String[] teile = eintrag.getErgebnis().trim().split(":");
		if (teile.length != 2) {
			return "";
		}

		try {
			int heimPunkte = Integer.parseInt(teile[0].trim());
			int gastPunkte = Integer.parseInt(teile[1].trim());
			int hattenhofen = eintrag.isHatheim() ? heimPunkte : gastPunkte;
			int gegner = eintrag.isHatheim() ? gastPunkte : heimPunkte;

			if (hattenhofen > gegner) {
				return "result-win";
			}
			if (hattenhofen < gegner) {
				return "result-loss";
			}
			return "result-draw";
		} catch (NumberFormatException e) {
			return "";
		}
	}

	private LocalDate parseDatumSafe(String datum) {
		if (datum == null) {
			return LocalDate.of(2999, 12, 31);
		}
		try {
			return LocalDate.parse(datum, DATUM_FORMAT);
		} catch (DateTimeParseException e) {
			return LocalDate.of(2999, 12, 31);
		}
	}

	public boolean isNeueWoche(int index) {
		return false;
//		if (index <= 0 || index >= datumsListe.size()) {
//			return false;
//		}
//		String vorher = datumsListe.get(index - 1);
//		String aktuell = datumsListe.get(index);
//		LocalDate vorherDatum = parseDatumSafe(vorher);
//		LocalDate aktuellDatum = parseDatumSafe(aktuell);
//		if (vorherDatum.getYear() == 2999 || aktuellDatum.getYear() == 2999) {
//			return false;
//		}
//		int vorherWoche = vorherDatum.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
//		int aktuellWoche = aktuellDatum.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
//		int vorherJahr = vorherDatum.get(IsoFields.WEEK_BASED_YEAR);
//		int aktuellJahr = aktuellDatum.get(IsoFields.WEEK_BASED_YEAR);
//		return vorherWoche != aktuellWoche || vorherJahr != aktuellJahr;
	}

	public List<GesamtspielplanEintrag> getEintraege(String datum, AnzeigeSpalte spalte) {
		if (datum == null || spalte == null) {
			return List.of();
		}
		return spieleByDatumUndSpalte.getOrDefault(datum, Map.of()).getOrDefault(spalte.getKey(), List.of());
	}

	public int getGesamtColspan() {
		int count = 1;
		for (AnzeigeSpalte spalte : spalten) {
			count += spalte.isJugend() ? 2 : 1;
		}
		return count;
	}

	public int getGesamtColspanOhneBetreuer() {
		return spalten.size() + 1;
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getHalbserie() {
		return halbserie;
	}

	public void setHalbserie(String halbserie) {
		this.halbserie = halbserie;
	}

	public List<AnzeigeSpalte> getSpalten() {
		return spalten;
	}

	public List<String> getDatumsListe() {
		return datumsListe;
	}

	public StreamedContent getDownloadPdf() {
		return downloadPdf;
	}

	public StreamedContent getDownloadExcel() {
		return downloadExcel;
	}

	public void prepareDownloadPdf() {
		downloadPdf = buildPdfDownload();
	}

	public void prepareDownloadExcel() {
		downloadExcel = buildExcelDownload();
	}

	private record BasisSpalte(String key, String liga, String mannschaft) {
	}

	public String getSelectedBetreuer() {
		return selectedBetreuer;
	}

	public void setSelectedBetreuer(String selectedBetreuer) {
		this.selectedBetreuer = selectedBetreuer;
	}

	public GesamtspielplanEintrag getSelectedEintrag() {
		return selectedEintrag;
	}

	private record QuellenDefinition(String liga, String mannschaft) {
		boolean matches(String ligaInput, String teamInput) {
			if (ligaInput == null || teamInput == null) {
				return false;
			}
			return ligaInput.trim().equals(liga.trim()) && teamInput.trim().equals(mannschaft.trim());
		}
	}

	private record SpaltenDefinition(String key, String mannschaft, String liga, boolean jugend,
			List<QuellenDefinition> quellen) {
	}

	private record VerfuegbarkeitsAntwort(String datum, String zeit, String name, String verfuegbarkeit,
			String kommentar) {
	}

	private record VerfuegbarkeitsDaten(Map<String, Map<String, VerfuegbarkeitsRueckmeldung>> verfuegbarkeitByDatumZeit,
			List<VerfuegbarkeitsAntwort> antworten) {
	}

	private record AufstellungSpielerInfo(String name, String rang) {
	}

	private record VerfuegbarkeitsRueckmeldung(String verfuegbarkeit, String kommentar) {
	}

	private record VerfuegbarkeitsSnapshot(List<SpielerRueckmeldung> zugesagt, List<SpielerRueckmeldung> abgesagt,
			List<SpielerRueckmeldung> zusatzZugesagt, List<SpielerRueckmeldung> offen) {
	}

	private Boolean parseBooleanObj(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			return null;
		}
		if ("1".equals(normalized)) {
			return Boolean.TRUE;
		}
		if ("0".equals(normalized)) {
			return Boolean.FALSE;
		}
		if ("true".equalsIgnoreCase(normalized)) {
			return Boolean.TRUE;
		}
		if ("false".equalsIgnoreCase(normalized)) {
			return Boolean.FALSE;
		}
		return null;
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public void zurueck() {

	}
}
