package de.bericht.service;

public class SpielErgebnis {
	private String art;
	private String heim;
	private String gast;
	private String saetze;

	public SpielErgebnis(String art, String heim, String gast, String saetze) {
		this.heim = heim;
		this.gast = gast;
		this.art = art;
		this.saetze = saetze;

	}

	public String getHeim() {
		return heim;
	}

	public String getGast() {
		return gast;
	}

	public String getArt() {
		return art;
	}

	public String getSaetze() {
		return saetze;
	}

}
