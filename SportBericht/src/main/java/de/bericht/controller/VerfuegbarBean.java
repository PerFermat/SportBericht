package de.bericht.controller;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.bericht.service.AufstellungSpieler;
import de.bericht.service.DatabaseService;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.VerfuegbarSpiel;
import de.bericht.util.VerfuegbarkeitEintrag;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("verfuegbarBean")
@ViewScoped
public class VerfuegbarBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static final List<String> VERFUEGBARKEIT_OPTIONEN = List.of("Ja", "Ja (aber noch nicht sicher)",
			"Nein (aber noch nicht sicher)", "Nein", "Ja (im Notfall)");

	private final DatabaseService databaseService = new DatabaseService();

	private String vereinnr;
	private String spielerFilterKey;
	private String mannschaft;
	private String liga;
	private String spielTeam;
	private String selectedSpieler;
	private String ruecksprung;
	private String halbserie;
	private final List<VerfuegbarSpiel> spiele = new ArrayList<>();
	private final List<String> spielerNamen = new ArrayList<>();
	private final Map<String, String> verfuegbarkeitBySpiel = new HashMap<>();
	private final Map<String, String> kommentarBySpiel = new HashMap<>();
	private final Map<String, List<VerfuegbarkeitEintrag>> eintraegeBySpiel = new LinkedHashMap<>();
	private final Map<String, Boolean> collapsedBySpiel = new HashMap<>();
	private static final long RUECKMELDUNG_TOLERANZ_STUNDEN = 4L;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}

		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}

		spielerFilterKey = request.getParameter("sp");
		ruecksprung = request.getParameter("ruecksprung");
		halbserie = request.getParameter("runde");
		liga = normalize(request.getParameter("liga"));
		spielTeam = normalize(request.getParameter("team"));
		if (vereinnr == null || vereinnr.isBlank() || spielerFilterKey == null || spielerFilterKey.isBlank()) {
			return;
		}

		ladeSpaltenKontext();
		ladeSpiele();
		ladeSpieler();
		ladeAlleEintraege();
		selectedSpieler = request.getParameter("name");
		onSpielerAenderung();
	}

	private void ladeSpaltenKontext() {
		String[] teile = spielerFilterKey.split("-Mannschaft-", 2);
		if (teile.length != 2) {
			return;
		}
		mannschaft = normalize(teile[0]);
		if (mannschaft == null) {
			return;
		}
		try {
			Integer.parseInt(teile[1].trim());
		} catch (NumberFormatException e) {
			mannschaft = null;
		}

	}

	private void ladeSpiele() {
		spiele.clear();
		if (istLeer(liga) || istLeer(spielTeam)) {
			return;
		}

		List<Map<String, String>> rows = databaseService.ladeVerfuegbarkeitSpiele(vereinnr);
		for (Map<String, String> row : rows) {
			String rowLiga = row.get("liga");
			String heim = row.get("heim");
			String gast = row.get("gast");
			if (!liga.equals(rowLiga) || (!spielTeam.equals(heim) && !spielTeam.equals(gast))) {
				continue;
			}
			VerfuegbarSpiel spiel = new VerfuegbarSpiel();
			spiel.setDatum(row.get("datum"));
			spiel.setWochentag(row.get("wochentag"));
			spiel.setZeit(row.get("zeit"));
			spiel.setLiga(rowLiga);
			spiel.setHeim(heim);
			spiel.setGast(gast);
			if (passtZurHalbserie(spiel.getDatum())) {
				spiele.add(spiel);
			}
		}

		spiele.sort(Comparator.comparing((VerfuegbarSpiel s) -> parseDatumSafe(s.getDatum()))
				.thenComparing(s -> parseZeitSafe(s.getZeit())));
		for (VerfuegbarSpiel spiel : spiele) {
			collapsedBySpiel.putIfAbsent(getSpielKey(spiel), true);
		}
	}

	private void ladeSpieler() {
		spielerNamen.clear();
		if (liga == null || liga.isBlank() || mannschaft == null || mannschaft.isBlank()) {
			return;
		}

		List<AufstellungSpieler> spielerListe = new ArrayList<>();
		for (Map<String, String> row : databaseService.ladeAufstellungRows(vereinnr)) {
			AufstellungSpieler spieler = new AufstellungSpieler();
			spieler.setVereinnr(row.get("vereinnr"));
			spieler.setMannschaft(row.get("mannschaft"));
			spieler.setRang(row.get("rang"));
			spieler.setQttr(row.get("qttr"));
			spieler.setName(row.get("name"));
			spieler.setA(row.get("a"));
			spieler.setStatus(row.get("status"));
			if (isSpielberechtigt(spieler)) {
				spielerListe.add(spieler);
			}
		}

		spielerListe.sort(Comparator.comparing(AufstellungSpieler::getRang, this::compareRang));
		spielerNamen.addAll(spielerListe.stream().map(AufstellungSpieler::getName)
				.filter(name -> name != null && !name.isBlank()).distinct().collect(Collectors.toList()));
	}

	private boolean isSpielberechtigt(AufstellungSpieler spieler) {
		if (mannschaft == null || spieler == null) {
			return false;
		}
		if (!mannschaft.equals(spieler.getMannschaft())) {
			return false;
		}
		return extrahiereZahlVorPunkt(spieler.getRang()) == extrahiereZahlAusSpielerKey(spielerFilterKey);

	}

	public List<String> getSpielerNamen() {
		return spielerNamen;
	}

	public List<String> getVerfuegbarkeiten() {
		return VERFUEGBARKEIT_OPTIONEN;
	}

	public void onSpielerAenderung() {
		ladeSpielerEingaben();
		boolean aufklappen = selectedSpieler != null && !selectedSpieler.isBlank();
		for (VerfuegbarSpiel spiel : spiele) {
			collapsedBySpiel.put(getSpielKey(spiel), !aufklappen);
		}
	}

	private void ladeSpielerEingaben() {
		verfuegbarkeitBySpiel.clear();
		kommentarBySpiel.clear();
		if (selectedSpieler == null || selectedSpieler.isBlank()) {
			return;
		}

		for (Map<String, String> row : databaseService.ladeSpielerVerfuegbarkeitNachName(vereinnr, selectedSpieler)) {
			String key = buildKey(row.get("datum"), row.get("uhrzeit"));
			verfuegbarkeitBySpiel.put(key, row.get("verfuegbarkeit"));
			kommentarBySpiel.put(key, row.get("kommentar"));
		}
	}

	public void speichereSpiel(VerfuegbarSpiel spiel) {
		if (spiel == null || selectedSpieler == null || selectedSpieler.isBlank()) {
			return;
		}
		String key = getSpielKey(spiel);
		String verfuegbarkeit = normalize(verfuegbarkeitBySpiel.get(key));
		String kommentar = normalize(kommentarBySpiel.get(key));

		if (verfuegbarkeit == null && kommentar == null) {
			databaseService.loescheSpielerVerfuegbarkeit(vereinnr, spiel.getDatum(), spiel.getZeit(), selectedSpieler);
		} else {
			databaseService.speichereSpielerVerfuegbarkeit(vereinnr, spiel.getDatum(), spiel.getZeit(), selectedSpieler,
					mannschaft, verfuegbarkeit, kommentar);
		}
		ladeAlleEintraege();
	}

	public void loescheSpiel(VerfuegbarSpiel spiel) {
		verfuegbarkeitBySpiel.put(getSpielKey(spiel), null);
		kommentarBySpiel.put(getSpielKey(spiel), null);
		speichereSpiel(spiel);
	}

	private void ladeAlleEintraege() {
		eintraegeBySpiel.clear();
		Map<String, SpielerAnzeigeInfo> spielerInfoByName = ladeSpielerInfoByName();
		Set<String> erlaubteSpieler = Set.copyOf(spielerNamen);
		Map<String, LocalDateTime> spielZeitByKey = new HashMap<>();
		Map<String, Map<String, ZugeordneterEintrag>> besteEintraegeBySpiel = new HashMap<>();

		for (VerfuegbarSpiel spiel : spiele) {
			String spielKey = getSpielKey(spiel);
			spielZeitByKey.put(spielKey, parseDatumZeitSafe(spiel.getDatum(), spiel.getZeit()));
		}

		for (Map<String, String> row : databaseService.ladeAlleSpielerVerfuegbarkeiten(vereinnr)) {
			String name = row.get("name");
			if (name == null || name.isBlank() || !erlaubteSpieler.contains(name)) {
				continue;
			}

			VerfuegbarkeitEintrag eintrag = new VerfuegbarkeitEintrag();
			eintrag.setName(name);
			eintrag.setMannschaft(row.get("mannschaft"));
			eintrag.setVerfuegbarkeit(row.get("verfuegbarkeit"));
			eintrag.setKommentar(row.get("kommentar"));
			SpielerAnzeigeInfo info = spielerInfoByName.get(name);
			if (info != null) {
				eintrag.setRang(info.rang());
				eintrag.setQttr(info.qttr());
			}

			eintrag.setAndereMannschaft(eintrag.getMannschaft() != null && mannschaft != null
					&& !eintrag.getMannschaft().isBlank() && !eintrag.getMannschaft().equals(mannschaft));

			LocalDateTime rueckmeldungZeit = parseDatumZeitSafe(row.get("datum"), row.get("uhrzeit"));
			for (VerfuegbarSpiel spiel : spiele) {
				String spielKey = getSpielKey(spiel);
				LocalDateTime spielZeit = spielZeitByKey.get(spielKey);
				if (!passtZurRueckmeldungZeit(spielZeit, rueckmeldungZeit, spiel, row)) {
					continue;
				}

				long differenzMinuten = berechneDifferenzMinuten(spielZeit, rueckmeldungZeit);
				Map<String, ZugeordneterEintrag> nachName = besteEintraegeBySpiel.computeIfAbsent(spielKey,
						k -> new HashMap<>());
				ZugeordneterEintrag vorhanden = nachName.get(name);
				if (vorhanden == null || differenzMinuten < vorhanden.differenzMinuten()) {
					nachName.put(name, new ZugeordneterEintrag(eintrag, differenzMinuten));
				}
			}
		}

		for (Map.Entry<String, Map<String, ZugeordneterEintrag>> spielEintrag : besteEintraegeBySpiel.entrySet()) {
			List<VerfuegbarkeitEintrag> eintraege = spielEintrag.getValue().values().stream()
					.map(ZugeordneterEintrag::eintrag).collect(Collectors.toList());
			eintraegeBySpiel.put(spielEintrag.getKey(), eintraege);

		}
		for (List<VerfuegbarkeitEintrag> eintraege : eintraegeBySpiel.values()) {
			eintraege.sort(Comparator
					.comparing((VerfuegbarkeitEintrag eintrag) -> eintrag.getRang() == null ? "9999.9999"
							: eintrag.getRang(), this::compareRang)
					.thenComparing(VerfuegbarkeitEintrag::getName, Comparator.nullsLast(String::compareTo)));
		}
	}

	private Map<String, SpielerAnzeigeInfo> ladeSpielerInfoByName() {
		Map<String, SpielerAnzeigeInfo> spielerInfoByName = new HashMap<>();
		if (mannschaft == null || mannschaft.isBlank()) {
			return spielerInfoByName;
		}

		for (Map<String, String> row : databaseService.ladeAufstellungRows(vereinnr)) {
			String name = row.get("name");
			String rang = row.get("rang");
			String qttr = row.get("qttr");
			AufstellungSpieler spieler = new AufstellungSpieler();
			spieler.setMannschaft(row.get("mannschaft"));
			spieler.setRang(rang);

			if (name == null || name.isBlank() || rang == null || rang.isBlank()) {
				continue;
			}
			if (!isSpielberechtigt(spieler)) {
				continue;
			}
			SpielerAnzeigeInfo bisherigeInfo = spielerInfoByName.get(name);
			if (bisherigeInfo == null || compareRang(rang, bisherigeInfo.rang()) < 0) {
				spielerInfoByName.put(name, new SpielerAnzeigeInfo(rang, qttr));

			}
		}
		return spielerInfoByName;
	}

	public long getAnzahl(VerfuegbarSpiel spiel, String option) {
		if (spiel == null || option == null || option.isBlank()) {
			return 0L;
		}
		long count = 0L;
		for (VerfuegbarkeitEintrag eintrag : getEintraege(spiel)) {
			if (eintrag.isAndereMannschaft()) {
				continue;
			}
			String wert = normalize(eintrag.getVerfuegbarkeit());
			if (option.equals(wert)) {
				count++;
			}
		}
		return count;
	}

	public List<VerfuegbarkeitEintrag> getEintraege(VerfuegbarSpiel spiel) {
		if (spiel == null) {
			return List.of();
		}
		return eintraegeBySpiel.getOrDefault(getSpielKey(spiel), List.of());
	}

	public void toggleSpiel(VerfuegbarSpiel spiel) {
		if (spiel == null) {
			return;
		}
		String key = getSpielKey(spiel);
		collapsedBySpiel.put(key, !isCollapsed(spiel));
	}

	public boolean isCollapsed(VerfuegbarSpiel spiel) {
		String key = getSpielKey(spiel);
		if (!collapsedBySpiel.containsKey(key)) {
			return selectedSpieler == null || selectedSpieler.isBlank();
		}
		return collapsedBySpiel.get(key);
	}

	public String getToggleSymbol(VerfuegbarSpiel spiel) {
		return isCollapsed(spiel) ? "+" : "−";
	}

	private String buildKey(String datum, String zeit) {
		return (datum == null ? "" : datum) + "|" + normalizeZeitWert(zeit);
	}

	private boolean istLeer(String value) {
		return value == null || value.trim().isEmpty();
	}

	private LocalDate parseDatumSafe(String datum) {
		if (datum == null || datum.isBlank()) {
			return LocalDate.of(2999, 1, 1);
		}
		try {
			return LocalDate.parse(datum, DATUM_FORMAT);
		} catch (DateTimeParseException ex) {
			return LocalDate.of(2999, 1, 1);
		}
	}

	private LocalTime parseZeitSafe(String zeit) {
		String normalisierteZeit = normalizeZeitWert(zeit);
		if (normalisierteZeit.isBlank()) {
			return LocalTime.MAX;
		}
		try {
			return LocalTime.parse(normalisierteZeit, DateTimeFormatter.ofPattern("H:mm"));
		} catch (DateTimeParseException ex) {
			try {
				return LocalTime.parse(normalisierteZeit, DateTimeFormatter.ofPattern("HH:mm"));
			} catch (DateTimeParseException ignored) {
				return LocalTime.MAX;
			}
		}
	}

	private String normalizeZeitWert(String zeit) {
		if (zeit == null) {
			return "";
		}
		String trimmed = zeit.trim();
		if (trimmed.length() >= 5) {
			return trimmed.substring(0, 5);
		}
		return trimmed;
	}

	private LocalDateTime parseDatumZeitSafe(String datum, String zeit) {
		if (datum == null || datum.isBlank() || zeit == null || zeit.isBlank()) {
			return null;
		}
		LocalDate parsedDatum = parseDatumSafe(datum);
		if (parsedDatum.getYear() == 2999) {
			return null;
		}
		LocalTime parsedZeit = parseZeitSafe(zeit);
		if (parsedZeit.equals(LocalTime.MAX)) {
			return null;
		}
		return LocalDateTime.of(parsedDatum, parsedZeit);
	}

	private boolean passtZurRueckmeldungZeit(LocalDateTime spielZeit, LocalDateTime rueckmeldungZeit,
			VerfuegbarSpiel spiel, Map<String, String> row) {
		if (spielZeit != null && rueckmeldungZeit != null) {
			return berechneDifferenzMinuten(spielZeit, rueckmeldungZeit) <= Duration
					.ofHours(RUECKMELDUNG_TOLERANZ_STUNDEN).toMinutes();
		}
		return getSpielKey(spiel).equals(buildKey(row.get("datum"), row.get("uhrzeit")));
	}

	private long berechneDifferenzMinuten(LocalDateTime zeit1, LocalDateTime zeit2) {
		return Math.abs(Duration.between(zeit1, zeit2).toMinutes());
	}

	private int compareRang(String r1, String r2) {
		RangParts p1 = parseRang(r1);
		RangParts p2 = parseRang(r2);
		if (p1.haupt() != p2.haupt()) {
			return Integer.compare(p1.haupt(), p2.haupt());
		}
		if (p1.neben() != p2.neben()) {
			return Integer.compare(p1.neben(), p2.neben());

		}
		return String.valueOf(r1).compareTo(String.valueOf(r2));
	}

	private RangParts parseRang(String rang) {
		if (rang == null || rang.isBlank()) {
			return new RangParts(Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
		String[] teile = rang.trim().split("\\.", 2);
		int haupt = parseIntSafe(teile[0], Integer.MAX_VALUE);
		int neben = teile.length > 1 ? parseIntSafe(teile[1], Integer.MAX_VALUE) : Integer.MAX_VALUE;
		return new RangParts(haupt, neben);
	}

	private int parseIntSafe(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public int extrahiereZahlVorPunkt(String text) {
		if (text == null || text.isBlank()) {
			return -1;
		}
		int pos = text.indexOf(".");
		String hauptteil = pos == -1 ? text : text.substring(0, pos);
		try {
			return Integer.parseInt(hauptteil.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private int extrahiereZahlAusSpielerKey(String value) {
		if (value == null) {
			return -1;
		}
		String[] teile = value.split("-Mannschaft-", 2);
		if (teile.length != 2) {
			return -1;
		}
		try {
			return Integer.parseInt(teile[1].trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getSelectedSpieler() {
		return selectedSpieler;
	}

	public void setSelectedSpieler(String selectedSpieler) {
		this.selectedSpieler = selectedSpieler;
	}

	public List<VerfuegbarSpiel> getSpiele() {
		return spiele;
	}

	public Map<String, String> getVerfuegbarkeitBySpiel() {
		return verfuegbarkeitBySpiel;
	}

	public Map<String, String> getKommentarBySpiel() {
		return kommentarBySpiel;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public String getLiga() {
		return liga;
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public String getSpielKey(VerfuegbarSpiel spiel) {
		if (spiel == null) {
			return "";
		}
		return buildKey(spiel.getDatum(), spiel.getZeit());
	}

	private record RangParts(int haupt, int neben) {
	}

	private record SpielerAnzeigeInfo(String rang, String qttr) {
	}

	private record ZugeordneterEintrag(VerfuegbarkeitEintrag eintrag, long differenzMinuten) {
	}

	public String getRuecksprung() {
		return ruecksprung;
	}

	public String ruecksprung() {
		return ruecksprung;
	}

	public void setRuecksprung(String ruecksprung) {
		this.ruecksprung = ruecksprung;
	}

	private boolean passtZurHalbserie(String datum) {
		if (halbserie == null) {
			return true;
		}

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

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public void zurueck() {

	}
}
