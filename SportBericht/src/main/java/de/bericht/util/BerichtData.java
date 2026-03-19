package de.bericht.util;

import java.nio.charset.StandardCharsets;

// Hilfsklasse zur Bündelung von Berichtstext und Bilddaten
public class BerichtData {
	private String berichtText;
	private byte[] bild;
	private String bildUnterschrift;
	private String ueberschrift;
	private Boolean mitSpielberichte;

	public String getBerichtText() {
		return berichtText;
	}

	public void setBerichtText(String berichtText) {
		this.berichtText = berichtText;
	}

	public void setBildUnterschrift(String bildUnterschrift) {
		this.bildUnterschrift = bildUnterschrift;
	}

	public byte[] getBild() {
		return bild;
	}

	public String getBildUnterschrift() {
		return bildUnterschrift;
	}

	public boolean isBild() {
		if (bild == null) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isBericht() {
		if (berichtText.length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	// Hier den Byte-Array in einen String umwandeln:
	public String getBildAsString() {
		if (bild == null) {
			return null;
		}
		return new String(bild, StandardCharsets.UTF_8);
	}

	public void setBild(byte[] bild) {
		this.bild = bild;
	}

	public String getUeberschrift() {
		return ueberschrift;
	}

	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = ueberschrift;
	}

	public Boolean getMitSpielberichte() {
		return mitSpielberichte;
	}

	public void setMitSpielberichte(Boolean mitSpielberichte) {
		this.mitSpielberichte = mitSpielberichte;
	}

}