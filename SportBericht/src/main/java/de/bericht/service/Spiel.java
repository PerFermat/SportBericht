package de.bericht.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Spiel implements Comparable<Spiel> {

	protected String vereinnr;
	protected String heim;
	protected String gast;
	protected String ergebnis;
	protected String ergebnisLink;
	protected int sortierung = 99;
	protected boolean wahl = true;
	protected boolean bericht;
	protected boolean bild;

	public String getVereinnr() {
		return vereinnr;
	}

	public String getHeim() {
		return heim;
	}

	public String getGast() {
		return gast;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public String getErgebnisLink() {
		if (ergebnisLink == null || ergebnisLink.isBlank()) {
			return "Kein Spielbericht vorhanden";
		}
		return ergebnisLink;
	}

	public boolean isWahl() {
		return wahl;
	}

	public void setWahl(boolean wahl) {
		this.wahl = wahl;
	}

	public boolean isBericht() {
		return bericht;
	}

	public void setBericht(boolean bericht) {
		this.bericht = bericht;
	}

	public boolean isBild() {
		return bild;
	}

	public void setBild(boolean bild) {
		this.bild = bild;
	}

	public String getHeimEncoded() throws UnsupportedEncodingException {
		return URLEncoder.encode(heim == null ? "" : heim, "UTF-8");
	}

	public String getGastEncoded() throws UnsupportedEncodingException {
		return URLEncoder.encode(gast == null ? "" : gast, "UTF-8");
	}

	public String getGenerateUniqueKey() {
		String keyInput = String.join("|", sanitize(vereinnr), sanitize(heim), sanitize(gast), sanitize(getLiga()));
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(keyInput.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 ist nicht verfügbar", e);
		}
	}

	private String sanitize(String value) {
		return value == null ? "" : value.trim();
	}

	@Override
	public int compareTo(Spiel anderesObjekt) {
		return Integer.compare(this.sortierung, anderesObjekt.sortierung);
	}

	public abstract String getSportart();

	public abstract String getDatumAnzeige();

	public abstract String getZeitAnzeige();

	public abstract String getLiga();

	public abstract void setGruppe(String liga);

	public abstract String getLigaJugend();

}