package de.bericht.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import de.bericht.service.TischtennisSpiel;

public class SpielUtils {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	// Hilfsmethode: Datum + Zeit in LocalDateTime konvertieren
	private static LocalDateTime toDateTime(TischtennisSpiel spiel) {
		String combined = spiel.getDatum() + " " + spiel.getZeit();
		return LocalDateTime.parse(combined, FORMATTER);
	}

	// Entfernt alle Spiele, die älter als 14 Tage sind
	public static void removeOldGames(List<TischtennisSpiel> spiele) {
		LocalDateTime grenze = LocalDateTime.now().minusDays(14);

		Iterator<TischtennisSpiel> it = spiele.iterator();
		while (it.hasNext()) {
			TischtennisSpiel s = it.next();
			if (toDateTime(s).isBefore(grenze)) {
				it.remove(); // sicheres Entfernen beim Iterieren
			}
		}
	}
}
