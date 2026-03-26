package de.bericht.util;

public class ConfigEintrag {
	private String vereinnr;
	private String eintrag;
	private String wert;
	private String bedeutung;
	private String inhaltformat;
	private String wertebereich;
	private String kategorien;
	

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

	public Boolean getPasswort() {
		if (this.eintrag.contains("passwort") || this.eintrag.contains("token")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isInhaltformatFarbe() {
		return hasInhaltformat("farbe");
	}

	public boolean isInhaltformatText() {
		return hasInhaltformat("text");
	}

	public boolean isInhaltformatZahl() {
		return hasInhaltformat("zahl");
	}

	public boolean isInhaltformatEnum() {
		return hasInhaltformat("enum");
	}

	public boolean isInhaltformatChatGpt() {
		return hasInhaltformat("chatgpt");
	}

	public boolean isInhaltformatPasswort() {
		return hasInhaltformat("passwort");
	}
	

	public String getBedeutung() {
		return bedeutung;
	}

	public void setBedeutung(String bedeutung) {
		this.bedeutung = bedeutung;
	}

	public String getInhaltformat() {
		return inhaltformat;
	}

	public void setInhaltformat(String inhaltformat) {
		this.inhaltformat = inhaltformat;
	}

	public String getWertebereich() {
		return wertebereich;
	}

	public void setWertebereich(String wertebereich) {
		this.wertebereich = wertebereich;
	}

	public String getTooltipText() {
		StringBuilder text = new StringBuilder();
		if (bedeutung != null && !bedeutung.isBlank()) {
			text.append("Bedeutung: ").append(bedeutung);
		}
		if (wertebereich != null && !wertebereich.isBlank()) {
			if (text.length() > 0) {
				text.append(" | ");
			}
			text.append("Wertebereich: ").append(wertebereich);
		}
		if (inhaltformat != null && !inhaltformat.isBlank()) {
			if (text.length() > 0) {
				text.append(" | ");
			}
			text.append("Format: ").append(inhaltformat);
		}
		if (kategorien != null && !kategorien.isBlank()) {
			if (text.length() > 0) {
				text.append(" | ");
			}
			text.append("Kategorien: ").append(kategorien);
		}
		return text.toString();
	}

	public String getKategorien() {
		return kategorien;
	}

	public void setKategorien(String kategorien) {
		this.kategorien = kategorien;
	}
	private boolean hasInhaltformat(String format) {
		return inhaltformat != null && inhaltformat.trim().equalsIgnoreCase(format);
	}


}
