package de.bericht.util;

public class ConfigBedeutung {
	private final String configEintrag;
	private final String bedeutung;
	private final String inhaltformat;
	private final String wertebereich;

	public ConfigBedeutung(String configEintrag, String bedeutung, String inhaltformat, String wertebereich) {
		this.configEintrag = configEintrag;
		this.bedeutung = bedeutung;
		this.inhaltformat = inhaltformat;
		this.wertebereich = wertebereich;
	}

	public String getConfigEintrag() {
		return configEintrag;
	}

	public String getBedeutung() {
		return bedeutung;
	}

	public String getInhaltformat() {
		return inhaltformat;
	}

	public String getWertebereich() {
		return wertebereich;
	}
}
