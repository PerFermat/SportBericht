package de.bericht.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.bericht.service.DatabaseService;

public class NamePart {
	private static final DatabaseService DB = new DatabaseService();
	private static final List<String> NACHNAME_CACHE = new ArrayList<>();
	private static final List<String> VORNAME_CACHE = new ArrayList<>();
	private static final List<String> NACHNAME_MISS_CACHE = new ArrayList<>();
	private static final List<String> VORNAME_MISS_CACHE = new ArrayList<>();

	private final String vorname;
	private final String nachname;

	public NamePart(String name) {
		String normalized = normalizeWhitespace(name);
		if (normalized.isEmpty()) {
			this.vorname = "";
			this.nachname = "";
			return;
		}
		if (normalized.contains(",")) {
			String[] parts = normalized.split(",", 2);
			this.vorname = normalizeWhitespace(parts.length > 1 ? parts[1] : "");
			this.nachname = normalizeWhitespace(parts[0]);
			return;
		}
		String[] words = normalized.split(" ");
		if (words.length == 1) {
			this.vorname = words[0];
			this.nachname = "";
			return;
		}
		if (words.length == 2) {
			this.vorname = words[0];
			this.nachname = words[1];
			return;
		}

		for (int i = 1; i < words.length - 1; i++) {
			String candidateNachname = String.join(" ", Arrays.copyOfRange(words, i, words.length));
			if (isBekannterNachname(candidateNachname)) {
				this.vorname = String.join(" ", Arrays.copyOfRange(words, 0, i));
				this.nachname = candidateNachname;
				return;
			}
		}

		int splitIndex = 1;
		for (int i = 1; i < words.length - 1; i++) {
			if (isBekannterVorname(words[i])) {
				splitIndex = i + 1;
			} else {
				break;
			}
		}
		this.vorname = String.join(" ", Arrays.copyOfRange(words, 0, splitIndex));
		this.nachname = String.join(" ", Arrays.copyOfRange(words, splitIndex, words.length));
	}

	public String getVorname() {
		return vorname;
	}

	public String getNachname() {
		return nachname;
	}

	private static synchronized boolean isBekannterNachname(String nachname) {
		String normalized = normalizeWhitespace(nachname).toLowerCase();
		if (normalized.isEmpty()) {
			return false;
		}
		if (NACHNAME_CACHE.contains(normalized)) {
			return true;
		}
		if (NACHNAME_MISS_CACHE.contains(normalized)) {
			return false;
		}
		boolean exists = DB.existsAdresslisteNachname(normalized);
		if (exists) {
			NACHNAME_CACHE.add(normalized);
		} else {
			NACHNAME_MISS_CACHE.add(normalized);
		}
		return exists;
	}

	private static synchronized boolean isBekannterVorname(String vorname) {
		String normalized = normalizeWhitespace(vorname).toLowerCase();
		if (normalized.isEmpty()) {
			return false;
		}
		if (VORNAME_CACHE.contains(normalized)) {
			return true;
		}
		if (VORNAME_MISS_CACHE.contains(normalized)) {
			return false;
		}
		boolean exists = DB.existsVorname(normalized);
		if (exists) {
			VORNAME_CACHE.add(normalized);
		} else {
			VORNAME_MISS_CACHE.add(normalized);
		}
		return exists;
	}

	private static String normalizeWhitespace(String input) {
		if (input == null) {
			return "";
		}
		return input.trim().replaceAll("\\s+", " ");
	}
}
