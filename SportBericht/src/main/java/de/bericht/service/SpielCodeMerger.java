package de.bericht.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bericht.util.SpielCode;

public class SpielCodeMerger {

	public static List<SpielCode> mergeSpielCodes(List<SpielCode> codes) {
		Map<String, SpielCode> mergedMap = new HashMap<>();

		for (SpielCode code : codes) {

			String key = generateKey(code);
			if (mergedMap.containsKey(key)) {
				SpielCode existing = mergedMap.get(key);
				if ("Spiel-Codes".equals(code.getTyp())) {
					existing.setSpielCode(code.getSpielCode());
				} else if ("Pin".equals(code.getTyp())) {
					existing.setPin(code.getPin());
				}
			} else {
				mergedMap.put(key, code);
			}
		}

		List<SpielCode> mergedList = new ArrayList<>(mergedMap.values());

		// Sortieren nach Datum und Uhrzeit (kalendarisch)
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		Collections.sort(mergedList, new Comparator<SpielCode>() {
			@Override
			public int compare(SpielCode o1, SpielCode o2) {
				try {
					Date d1 = sdf.parse(o1.getDatum() + " " + o1.getUhrzeit());
					Date d2 = sdf.parse(o2.getDatum() + " " + o2.getUhrzeit());
					return d1.compareTo(d2);
				} catch (ParseException e) {
					throw new RuntimeException("Fehler beim Parsen von Datum/Uhrzeit", e);
				}
			}
		});

		return mergedList;
	}

	private static String generateKey(SpielCode code) {
		return code.getLiga() + "|" + code.getMannschaft() + "|" + code.getWochentag() + "|" + code.getDatum() + "|"
				+ code.getUhrzeit() + "|" + code.getHeimmannschaft() + "|" + code.getGastmannschaft();

	}
}
