package de.bericht.util.enums;

public enum WordpressBeitragsbildOption {
	/** Standard: hochgeladenes Spielbild wird als Beitragsbild verwendet. */
	STANDARD("standard"),
	/** Nur Beitragsbild: Bild nicht im Fließtext einbetten, nur als Featured Image nutzen. */
	NUR_BEITRAGSBILD("nurBeitragsbild"),
	/** Immer Standardbild: Beitragsbild wird aus der Symbol-Konfiguration je Kategorie genommen. */
	IMMER_STANDARD("immerStandard");

	private final String configValue;

	WordpressBeitragsbildOption(String configValue) {
		this.configValue = configValue;
	}

	public String getConfigValue() {
		return configValue;
	}

	public static WordpressBeitragsbildOption fromConfig(String value) {
		if (value == null || value.isBlank()) {
			return STANDARD;
		}
		for (WordpressBeitragsbildOption option : values()) {
			if (option.getConfigValue().equalsIgnoreCase(value.trim())) {
				return option;
			}
		}
		return STANDARD;
	}
}
