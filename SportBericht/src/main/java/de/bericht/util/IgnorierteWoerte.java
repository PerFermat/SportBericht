package de.bericht.util;

import java.util.HashSet;
import java.util.Set;

public class IgnorierteWoerte {
	// Set, um doppelte Einträge zu vermeiden (schneller als eine List)
	private Set<String> ignorierteWoerter;

	// Konstruktor
	public IgnorierteWoerte() {
		ignorierteWoerter = new HashSet<>();
	}

	// Methode zum Hinzufügen von Wörtern aus einem gegebenen String
	public void hinzufuegen(String text) {
		// Zuerst prüfen, ob der String null ist
		if (text == null) {
			return;
		}
		// Dann den String in Wörter aufteilen
		String[] woerter = text.split("[\\s/]+");

		// Für jedes Wort prüfen, ob es schon vorhanden ist. Falls nicht, hinzufügen.
		for (String wort : woerter) {
			// Entfernen von möglichen Sonderzeichen (z.B. Kommas, Punkte)

			if (!wort.isEmpty()) {
				ignorierteWoerter.add(wort);
			}
		}
	}

	// Methode zur Prüfung, ob ein Wort in der Liste ist
	public boolean wortIgnorieren(String wort) {
		// Sonderzeichen entfernen und klein schreiben

		return ignorierteWoerter.contains(wort);
	}

	// Methode um die ignorierten Wörter zu sehen (nützlich für Tests)
	public Set<String> getIgnorierteWoerter() {
		return ignorierteWoerter;
	}

}
