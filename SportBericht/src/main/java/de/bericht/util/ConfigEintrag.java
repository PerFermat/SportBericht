package de.bericht.util;

public class ConfigEintrag {
	private String vereinnr;
	private String eintrag;
	private String wert;

	public ConfigEintrag(String vereinnr, String eintrag, String wert) {
		this.vereinnr = vereinnr;
		this.eintrag = eintrag;
		this.wert = wert;
	}

	public String getEintrag() {
		return eintrag;
	}

	public void setEintrag(String eintrag) {
		this.eintrag = eintrag;
	}

	public String getWert() {
		return this.wert;
	}

	public void setWert(String wert) {
		this.wert = wert;
	}

	public Boolean getFarbe() {
		if (this.eintrag.contains("farbe")) {
			return true;
		} else {
			return false;
		}
	}

	public Boolean getPassword() {
		if (this.eintrag.contains("password") || this.eintrag.contains("token")) {
			return true;
		} else {
			return false;
		}
	}
}
