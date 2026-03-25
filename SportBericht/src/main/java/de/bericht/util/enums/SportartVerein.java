package de.bericht.util.enums;

public enum SportartVerein {
	/** Tischtennis-Modus: Spielplan, Ergebnislogik und Views für Tischtennis. */
	TISCHTENNIS,
	/** Tennis-Modus: Spielplan, Ergebnislogik und Views für Tennis. */
	TENNIS;

	public static SportartVerein fromConfig(String value) {
		if (value == null) {
			return null;
		}
		for (SportartVerein element : values()) {
			if (element.name().equalsIgnoreCase(value.trim())) {
				return element;
			}
		}
		return null;
	}
}
