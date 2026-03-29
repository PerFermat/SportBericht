package de.bericht.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import de.bericht.service.DatabaseService;

public class ErgebnisCache {

	private static class CacheEntry {
		int value;
		Instant expiryTime;

		CacheEntry(int value, int ttlMinutes) {
			this.value = value;
			this.expiryTime = Instant.now().plus(Duration.ofMinutes(ttlMinutes));
		}

		boolean isExpired() {
			return Instant.now().isAfter(expiryTime);
		}
	}

	private static final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

	public static int anzahl(String vereinnr, String was, String ergebnisLink, int ttlMinutes) {
		return anzahl(vereinnr, was, ergebnisLink, ttlMinutes, "");
	}

	public static int anzahl(String vereinnr, String was, String ergebnisLink, int ttlMinutes, String name) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return -1;
		}

		CacheEntry entry = cache.get(vereinnr + was + name + ergebnisLink);
		if (entry != null && !entry.isExpired()) {
			return entry.value;
		}
		int value;
		if (was.equals("Freigabe")) {
			DatabaseService dbService = new DatabaseService(vereinnr);
			value = dbService.anzahlFreigabe(vereinnr, ergebnisLink); // dein DB-Zugriff
		} else if (was.equals("Blaettle")) {
			DatabaseService dbService = new DatabaseService(vereinnr);
			value = dbService.anzahlBlaettle(vereinnr, ergebnisLink); // dein DB-Zugriff
		} else if (was.equals("Wordpress")) {
			DatabaseService dbService = new DatabaseService(vereinnr);
			value = dbService.anzahlWordpress(vereinnr, ergebnisLink, name); // dein DB-Zugriff
		} else {
			return -1;
		}
		cache.put(vereinnr + was + name + ergebnisLink, new CacheEntry(value, ttlMinutes));
		return value;
	}

	public static int setze(String vereinnr, String was, DatabaseService dbService, String ergebnisLink, String name) {
		if (ergebnisLink == null || ergebnisLink.trim().isEmpty()) {
			return -1;
		}

		int value;
		if (was.equals("Freigabe")) {
			value = dbService.anzahlFreigabe(vereinnr, ergebnisLink); // dein DB-Zugriff
		} else if (was.equals("Blaettle")) {
			value = dbService.anzahlBlaettle(vereinnr, ergebnisLink); // dein DB-Zugriff
		} else if (was.equals("Wordpress")) {
			value = dbService.anzahlWordpress(vereinnr, ergebnisLink, name); // dein DB-Zugriff
		} else {
			return -1;
		}
		cache.put(vereinnr + was + name + ergebnisLink, new CacheEntry(value, 6000));
		return value;
	}

	public static void cacheLeeren() {
		cache.clear();
	}
}
