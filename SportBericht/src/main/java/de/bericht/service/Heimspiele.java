package de.bericht.service;

import java.io.Serializable;

public class Heimspiele implements Serializable {
	private static final long serialVersionUID = 1L;
	private String datum;
	private String zeit;
	private String liga;
	private String gast;
	private String heim;

	public Heimspiele(String datum, String zeit, String liga, String heim, String gast) {
		super();
		this.datum = datum;
		this.zeit = zeit;
		this.liga = liga;
		this.heim = heim;
		this.gast = gast;
	}

	public String getDatum() {
		return datum;
	}

	public int getTag() {
		if (datum == null || datum.length() < 2) {
			return 0;
		}

		try {
			return Integer.parseInt(datum.substring(0, 2));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getTagText() {
		return datum.substring(0, 2);
	}

	public String getZeit() {
		return zeit;
	}

	public String getLiga() {
		return liga;
	}

	public String getGast() {
		return gast;
	}

	public String getHeim() {
		return heim;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public void setZeit(String zeit) {
		this.zeit = zeit;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

}