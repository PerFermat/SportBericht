package de.bericht.service;

import java.io.Serializable;

public class GesamtspielplanEintrag implements Serializable {
	private static final long serialVersionUID = 1L;
	private String uniqueKey;
	private String datum;
	private String zeit;
	private String liga;
	private String heim;
	private String gast;
	private String ergebnis;
	private String gegner;
	private String heimOderAuswaerts;
	private String ergebnisFarbeClass;
	private String betreuer;
	private Boolean bestaetigt;
	private String kommentar;
	private boolean hatheim;
	private boolean jugend;
	private java.time.LocalDateTime mailTimestamp;
	private int verfuegbarJaAnzahl;
	private int verfuegbarNeinAnzahl;
	private boolean zukunftsspiel;

	public String getAnzeigeKopf() {
		return zeit;
	}

	public String getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public String getDatum() {
		return datum;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public String getZeit() {
		return zeit;
	}

	public void setZeit(String zeit) {
		this.zeit = zeit;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getHeim() {
		return heim;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

	public String getGast() {
		return gast;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
	}

	public String getGegner() {
		return gegner;
	}

	public void setGegner(String gegner) {
		this.gegner = gegner;
	}

	public String getHeimOderAuswaerts() {
		return heimOderAuswaerts;
	}

	public void setHeimOderAuswaerts(String heimOderAuswaerts) {
		this.heimOderAuswaerts = heimOderAuswaerts;
	}

	public String getErgebnisFarbeClass() {
		return ergebnisFarbeClass;
	}

	public void setErgebnisFarbeClass(String ergebnisFarbeClass) {
		this.ergebnisFarbeClass = ergebnisFarbeClass;
	}

	public String getBetreuer() {
		return betreuer;
	}

	public String getBetreuerInitialen() {
		return getBetreuerInitialen(betreuer);
	}

	public static String getBetreuerInitialen(String betreuer) {
		if (betreuer == null || betreuer.trim().isEmpty()) {
			return "";
		}

		StringBuilder initialen = new StringBuilder();

		String[] worte = betreuer.trim().split("\\s+"); // trennt bei einem oder mehreren Leerzeichen

		for (String wort : worte) {
			if (!wort.isEmpty()) {
				initialen.append(Character.toUpperCase(wort.charAt(0)));
			}
		}

		return initialen.toString();
	}

	public void setBetreuer(String betreuer) {
		this.betreuer = betreuer;
	}

	public boolean isHatheim() {
		return hatheim;
	}

	public String matchTooltipHtml(boolean jugendSpiel) {
		String ligaText = liga == null || liga.isBlank() ? "Liga: -" : "Liga: " + liga;
		StringBuilder html = new StringBuilder("<div class='match-tooltip-content'>");
		html.append("<span class='tooltip-line tooltip-liga'>").append(escapeHtml(ligaText)).append("</span>");
		if (jugendSpiel) {
			String betreuerText = betreuer == null || betreuer.isBlank() ? "Betreuer: Kein Betreuer hinterlegt"
					: "Betreuer: " + betreuer;
			html.append("<span class='tooltip-line tooltip-betreuer'>").append(escapeHtml(betreuerText))
					.append("</span>");
		}
		html.append("</div>");
		return html.toString();
	}

	private String escapeHtml(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	public void setHatheim(boolean hatheim) {
		this.hatheim = hatheim;
	}

	public boolean isJugend() {
		return jugend;
	}

	public void setJugend(boolean jugend) {
		this.jugend = jugend;
	}

	public boolean isErgebnisVorhanden() {
		return ergebnis != null && !ergebnis.isBlank();
	}

	public Boolean getBestaetigt() {
		return bestaetigt;
	}

	public void setBestaetigt(Boolean bestaetigt) {
		this.bestaetigt = bestaetigt;
	}

	public String getKommentar() {
		return kommentar;
	}

	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}

	public java.time.LocalDateTime getMailTimestamp() {
		return mailTimestamp;
	}

	public void setMailTimestamp(java.time.LocalDateTime mailTimestamp) {
		this.mailTimestamp = mailTimestamp;
	}

	public String getBetreuerStatusClass() {
		if (Boolean.TRUE.equals(bestaetigt)) {
			return "betreuer-status-confirmed";
		}
		if (Boolean.FALSE.equals(bestaetigt)) {
			return "betreuer-status-declined";
		}
		return "betreuer-status-open";
	}

	public int getVerfuegbarJaAnzahl() {
		return verfuegbarJaAnzahl;
	}

	public void setVerfuegbarJaAnzahl(int verfuegbarJaAnzahl) {
		this.verfuegbarJaAnzahl = verfuegbarJaAnzahl;
	}

	public boolean isRueckmeldungen() {
		if ((verfuegbarNeinAnzahl + verfuegbarJaAnzahl) == 0) {
			return false;
		}
		return true;
	}

	public int getVerfuegbarNeinAnzahl() {
		return verfuegbarNeinAnzahl;
	}

	public void setVerfuegbarNeinAnzahl(int verfuegbarNeinAnzahl) {
		this.verfuegbarNeinAnzahl = verfuegbarNeinAnzahl;
	}

	public boolean isZukunftsspiel() {
		return zukunftsspiel;
	}

	public void setZukunftsspiel(boolean zukunftsspiel) {
		this.zukunftsspiel = zukunftsspiel;
	}

}
