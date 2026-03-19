package de.bericht.service;

public class Mannschaft {

	private String mannschaft;
	private String mannschaftUrl;
	private String liga;
	private String ligaUrl;
	private String rang;
	private String punkte;

	public Mannschaft(String mannschaft, String mannschaftUrl, String liga, String ligaUrl, String rang,
			String punkte) {
		super();
		this.mannschaft = mannschaft;
		this.mannschaftUrl = mannschaftUrl;
		this.liga = liga;
		this.ligaUrl = ligaUrl;
		this.rang = rang;
		this.punkte = punkte;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public String getBilanzenUrl() {
		return mannschaftUrl;
	}

	public String getSpielplanUrl() {
		return mannschaftUrl.replace("spielerbilanzen", "spielplan");
	}

	public String getLiga() {
		String lang = liga;
		lang = lang.replaceAll("VR", "").replaceAll("RR", "").trim();
		lang = lang.replaceAll(",?\\s*(Gr\\.\\s*\\d+|Gruppe\\s+\\d+\\.?)(?=$|\\s)", "");
		lang = lang.replaceAll("(Gr\\.\\s*\\d+|Gruppe\\s+\\d+\\.? )", "");
		lang = lang.replaceAll("Vorrunde", "").replaceAll("Rückrunde", "").trim();
		return lang;
	}

	public String getLigaKurz() {

		String kurz = liga;

		kurz = kurz.replaceAll("Jugend ", "J").replaceAll("Erwachsene ", "E ").replaceAll("Senioren ", "S")
				.replaceAll("Mädchen ", "M").replaceAll("Damen ", "D ");

		kurz = kurz.replaceAll("Landesklasse", "LK").replaceAll("Bezirksliga", "BL").replaceAll("Kreisklasse", "KK")
				.replaceAll("Kreisliga", "KL").replaceAll("Bezirksklasse", "BK").replaceAll("Verbandsliga", "VL");
		kurz = kurz.replaceAll(",?\\s*(Gr\\.\\s*\\d+|Gruppe\\s+\\d+\\.?)(?=$|\\s)", "");
		kurz = kurz.replaceAll("(Gr\\.\\s*\\d+|Gruppe\\s+\\d+\\.? )", "");

		kurz = kurz.replaceAll("Bezirkspokal", "BP");

		kurz = kurz.replaceAll("VR", "").replaceAll("RR", "").trim();
		kurz = kurz.replaceAll("JU", "J").replaceAll("JU ", "J").trim();
		kurz = kurz.replaceAll("J ", "J");
		kurz = kurz.replaceAll("Vorrunde", "").replaceAll("Rückrunde", "").trim();
		;
		return kurz;
	}

	public String getLigaUrl() {
		return ligaUrl;
	}

	public String getRang() {
		return rang;
	}

	public String getPunkte() {
		return punkte;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public void setMannschaftUrl(String mannschaftUrl) {
		this.mannschaftUrl = mannschaftUrl;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public void setLigaUrl(String ligaUrl) {
		this.ligaUrl = ligaUrl;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public void setPunkte(String punkte) {
		this.punkte = punkte;
	}

}
