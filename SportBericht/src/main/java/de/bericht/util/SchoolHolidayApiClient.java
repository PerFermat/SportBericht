package de.bericht.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import de.bericht.service.DatabaseService;
import de.bericht.service.SchoolHoliday;

public class SchoolHolidayApiClient {

	private static final String API_URL = "https://openholidaysapi.org/SchoolHolidays?countryIsoCode=DE&subdivisionCode=%s&validFrom=%d-01-01&validTo=%d-12-31";

	private final HttpClient client = HttpClient.newHttpClient();
	DatabaseService databaseService = new DatabaseService();
	String vereinnr;

	// Jahr -> Datum -> Ferien
	private final Map<Integer, Map<LocalDate, SchoolHoliday>> cache = new HashMap<>();

	public SchoolHolidayApiClient(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	// ---------------- PUBLIC API ----------------

	public boolean isHolidayBW(LocalDate date) {
		return getHolidayMap(date.getYear()).containsKey(date);
	}

	public SchoolHoliday getHolidayIfPresent(LocalDate date) {
		return getHolidayMap(date.getYear()).get(date);
	}

	// ---------------- CORE CACHE ----------------

	private Map<LocalDate, SchoolHoliday> getHolidayMap(int year) {
		return cache.computeIfAbsent(year, this::loadYear);
	}

	// ---------------- LOAD LOGIC ----------------

	private Map<LocalDate, SchoolHoliday> loadYear(int year) {

		// 1. DB versuchen
		List<SchoolHoliday> fromDb = databaseService
				.ladeSchulferienBW(ConfigManager.getConfigValue(vereinnr, "sftp.ferien.bundeslaender"), year);

		if (fromDb != null && !fromDb.isEmpty()) {
			return expand(fromDb);
		}

		// 2. API fallback
		List<SchoolHoliday> fromApi = loadFromApi(year);

		// 3. in DB speichern
		for (SchoolHoliday h : fromApi) {
			databaseService.speichereSchulferien(h,
					ConfigManager.getConfigValue(vereinnr, "sftp.ferien.bundeslaender"));
		}

		return expand(fromApi);
	}

	// ---------------- API CALL ----------------

	private List<SchoolHoliday> loadFromApi(int year) {
		try {
			String url = String.format(API_URL, ConfigManager.getConfigValue(vereinnr, "sftp.ferien.bundeslaender"),
					year, year);
			System.out.println(url);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
					.timeout(java.time.Duration.ofSeconds(10)).GET().build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				System.err.println("API Fehler: " + response.statusCode());
				return Collections.emptyList();
			}

			JSONArray arr = new JSONArray(response.body());

			List<SchoolHoliday> result = new ArrayList<>();

			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);

				LocalDate start = LocalDate.parse(obj.getString("startDate"));
				LocalDate end = LocalDate.parse(obj.getString("endDate"));
				String name = extractGermanName(obj.opt("name"));

				result.add(new SchoolHoliday(start, end, name));
			}

			return result;

		} catch (Exception e) {
			System.err.println("API Load Fehler: " + e.getMessage());
			return Collections.emptyList();
		}
	}

	// ---------------- EXPAND TO MAP (O(1)) ----------------

	private Map<LocalDate, SchoolHoliday> expand(List<SchoolHoliday> holidays) {

		Map<LocalDate, SchoolHoliday> map = new HashMap<>();

		for (SchoolHoliday h : holidays) {
			for (LocalDate d = h.start; !d.isAfter(h.end); d = d.plusDays(1)) {
				map.put(d, h);
			}
		}

		return map;
	}

	private String extractGermanName(Object nameObj) {

		if (nameObj == null) {
			return "Ferien";
		}

		// Fall 1: einfacher String
		if (nameObj instanceof String s) {
			return s;
		}

		// Fall 2: JSONObject { "de": "...", "en": "..." }
		if (nameObj instanceof JSONObject obj) {
			if (obj.has("de")) {
				return obj.getString("de");
			}
			return obj.toString();
		}

		// Fall 3: Array [{language:"de", text:"..."}]
		if (nameObj instanceof JSONArray arr) {
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = arr.getJSONObject(i);
				if ("de".equalsIgnoreCase(o.optString("language"))) {
					return o.optString("text", "Ferien");
				}
			}

			// fallback: erstes Element
			JSONObject o = arr.getJSONObject(0);
			return o.optString("text", "Ferien");
		}

		return nameObj.toString();
	}

}