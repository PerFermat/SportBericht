package de.bericht.util;

public class ConfigKategorie {
	private final String configEintrag;
	private final String kategorie;

	public ConfigKategorie(String configEintrag, String kategorie) {
		this.configEintrag = configEintrag;
		this.kategorie = kategorie;
	}

	public String getConfigEintrag() {
		return configEintrag;
	}

	public String getKategorie() {
		return kategorie;
	}
}
