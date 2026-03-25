package de.bericht.util.enums;

public enum SpielplanVorschauWas {
	/** Es werden nur Heimspiele in den Vorschaubericht aufgenommen. */
	HEIM,
	/** Es werden nur Auswärtsspiele in den Vorschaubericht aufgenommen. */
	GAST,
	/** Es werden Heim- und Auswärtsspiele in den Vorschaubericht aufgenommen. */
	ALLE;

	public static SpielplanVorschauWas fromConfig(String value) {
		if (value == null) {
			return null;
		}
		for (SpielplanVorschauWas element : values()) {
			if (element.name().equalsIgnoreCase(value.trim())) {
				return element;
			}
		}
		return null;
	}
}
