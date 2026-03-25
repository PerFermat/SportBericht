package de.bericht.util.enums;

public enum WordpressDatumOption {
	/** WordPress-Post nutzt das Spieldatum aus dem Bericht als Veröffentlichungszeitpunkt. */
	SPIELDATUM("Spieldatum"),
	/** Fallback für alle anderen Werte: aktuelles Erstellungsdatum des Beitrags. */
	AKTUELL("Aktuell");

	private final String configValue;

	WordpressDatumOption(String configValue) {
		this.configValue = configValue;
	}

	public String getConfigValue() {
		return configValue;
	}

	public static WordpressDatumOption fromConfig(String value) {
		if (value == null) {
			return AKTUELL;
		}
		for (WordpressDatumOption option : values()) {
			if (option.getConfigValue().equalsIgnoreCase(value.trim())) {
				return option;
			}
		}
		return AKTUELL;
	}
}
