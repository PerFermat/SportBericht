package de.bericht.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.bericht.service.DatabaseService;
import de.bericht.service.EmailService;
import de.bericht.service.GesamtspielplanEintrag;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("bestaetigungBean")
@ViewScoped
public class BestaetigungBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private final ConfigManager configManager = ConfigManager.getInstance();
	private final DatabaseService databaseService = new DatabaseService();

	private String uuid;
	private String vereinnr;
	private GesamtspielplanEintrag eintrag;

	private String zusatzKommentar;
	private boolean rueckmeldungGesendet;
	private String selectedBetreuer;
	private String initialBetreuer;
	private final List<String> betreuerNamen = new ArrayList<>();
	private static final String BESTAETIGEN = "Bin dabei! 💪";
	private static final String ABLEHNEN_MIT_ERSATZ = "Bin raus – ich kümmere mich um Ersatz.";
	private static final String ABLEHNEN_OHNE_ERSATZ = "Bin raus – bitte kümmere du dich um Ersatz.";
	private String selectedTauschSpielUniqueKey;
	private List<GesamtspielplanEintrag> tauschSpiele = new ArrayList<>();
	private boolean mailGesendetHinweis;
	private String liga;

	@PostConstruct
	public void init() {
		uuid = jakarta.faces.context.FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.get("UUID");
		if (uuid == null || uuid.isBlank()) {
			uuid = jakarta.faces.context.FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
					.get("uuid");
		}
		ladeEintrag();
	}

	private void ladeEintrag() {
		if (uuid == null || uuid.isBlank()) {
			return;
		}
		eintrag = databaseService.ladeBestaetigungEintrag(uuid);
		if (eintrag != null) {
			liga = eintrag.getLiga();
			vereinnr = databaseService.ladeVereinnrFuerSpiel(uuid);
			rueckmeldungGesendet = eintrag.getBestaetigt() != null;
			selectedBetreuer = eintrag.getBetreuer();
			initialBetreuer = eintrag.getBetreuer();
			ladeBetreuerNamenFallsNoetig();
			tauschSpiele = ladeTauschSpiele();
		}
	}

	public void bestaetigen() {
		mailGesendetHinweis = false;
		speichereRueckmeldung(Boolean.TRUE, BESTAETIGEN);
	}

	public void ablehnenMitErsatz() {
		mailGesendetHinweis = false;
		speichereRueckmeldung(Boolean.FALSE, ABLEHNEN_MIT_ERSATZ);
	}

	public void ablehnenOhneErsatz() {
		mailGesendetHinweis = false;
		speichereRueckmeldung(Boolean.FALSE, ABLEHNEN_OHNE_ERSATZ);
	}

	private void speichereRueckmeldung(Boolean bestaetigt, String buttonKommentar) {
		if (eintrag == null || eintrag.getUniqueKey() == null || eintrag.getUniqueKey().isBlank()
				|| rueckmeldungGesendet) {
			return;
		}
		String betreuerName = selectedBetreuer;
		eintrag.setBetreuer(betreuerName);

		String gesamtKommentar = baueKommentar(buttonKommentar, zusatzKommentar);
		try {
			databaseService.upsertSpielplanBetreuer(eintrag.getUniqueKey(), betreuerName, bestaetigt, gesamtKommentar,
					null);
			eintrag.setBestaetigt(bestaetigt);
			eintrag.setKommentar(gesamtKommentar);
			rueckmeldungGesendet = true;
			if (Boolean.FALSE.equals(bestaetigt)) {
				sendeAblehnungsEmail(buttonKommentar, gesamtKommentar);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<String> completeBetreuer(String query) {
		ladeBetreuerNamenFallsNoetig();
		if (query == null || query.isBlank()) {
			return betreuerNamen;
		}
		String normalizedQuery = query.trim().toLowerCase();
		return betreuerNamen.stream().filter(name -> name.toLowerCase().contains(normalizedQuery))
				.collect(Collectors.toList());
	}

	public void aktualisiereTauschSpiele() {
		tauschSpiele = ladeTauschSpiele();
		selectedTauschSpielUniqueKey = null;
	}

	public void rueckmeldungZuruecknehmenOderSpeichern() {
		if (eintrag == null || eintrag.getUniqueKey() == null || eintrag.getUniqueKey().isBlank()) {
			return;
		}

		String aktuellerBetreuer = selectedBetreuer;
		boolean warBestaetigt = Boolean.TRUE.equals(eintrag.getBestaetigt());
		boolean nameGeaendert = hatNameGeaendert();
		mailGesendetHinweis = false;

		try {
			databaseService.upsertSpielplanBetreuer(eintrag.getUniqueKey(), aktuellerBetreuer, null, null, null);

			eintrag.setBetreuer(aktuellerBetreuer);
			eintrag.setBestaetigt(null);
			eintrag.setKommentar(null);
			eintrag.setMailTimestamp(null);
			rueckmeldungGesendet = false;

			if (aktuellerBetreuer != null && !aktuellerBetreuer.isBlank()
					&& !betreuerNamen.contains(aktuellerBetreuer)) {
				betreuerNamen.add(aktuellerBetreuer);
				Collections.sort(betreuerNamen);
			}

			if (warBestaetigt) {
				sendeRuecknahmeEmail(nameGeaendert ? "Speichern" : "Rückmeldung zurücknehmen");
			} else if (nameGeaendert) {
				sendeBetreuerGeaendertEmail(nameGeaendert ? "Speichern" : "Betreuer wurde geändert");
			}

			initialBetreuer = aktuellerBetreuer;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void tauschen() {
		tauschen(selectedTauschSpielUniqueKey);
	}

	public void tauschen(String tauschSpielUniqueKey) {
		if (eintrag == null || eintrag.getUniqueKey() == null || eintrag.getUniqueKey().isBlank()) {
			return;
		}
		String neuerBetreuer = selectedBetreuer == null ? null : selectedBetreuer.trim();
		if (neuerBetreuer == null || neuerBetreuer.isBlank()) {
			return;
		}

		GesamtspielplanEintrag spielDesNeuenBetreuers = findeTauschSpiel(tauschSpielUniqueKey);
		if (spielDesNeuenBetreuers == null || spielDesNeuenBetreuers.getUniqueKey() == null
				|| spielDesNeuenBetreuers.getUniqueKey().isBlank()) {
			return;
		}

		String alterBetreuer = eintrag.getBetreuer();
		try {
			databaseService.tauscheSpielbetreuer(eintrag.getUniqueKey(), neuerBetreuer,
					spielDesNeuenBetreuers.getUniqueKey(), alterBetreuer);

			eintrag.setBetreuer(neuerBetreuer);
			selectedBetreuer = neuerBetreuer;
			initialBetreuer = neuerBetreuer;
			tauschSpiele = ladeTauschSpiele();
			selectedTauschSpielUniqueKey = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private GesamtspielplanEintrag findeTauschSpiel(String tauschSpielUniqueKey) {
		if (tauschSpielUniqueKey == null || tauschSpielUniqueKey.isBlank()) {
			return null;
		}
		for (GesamtspielplanEintrag spiel : tauschSpiele) {
			if (tauschSpielUniqueKey.equals(spiel.getUniqueKey())) {
				return spiel;
			}
		}
		return null;
	}

	private List<GesamtspielplanEintrag> ladeTauschSpiele() {
		if (eintrag == null || eintrag.getUniqueKey() == null || eintrag.getUniqueKey().isBlank() || vereinnr == null
				|| vereinnr.isBlank()) {
			return Collections.emptyList();
		}
		String betreuer = selectedBetreuer == null ? "" : selectedBetreuer.trim();
		if (betreuer.isBlank()) {
			return Collections.emptyList();
		}

		return databaseService.ladeTauschSpiele(vereinnr, betreuer, eintrag.getUniqueKey());
	}

	public List<GesamtspielplanEintrag> getTauschSpiele() {
		return tauschSpiele;
	}

	public String formatTauschSpielText(GesamtspielplanEintrag tauschSpiel) {
		if (tauschSpiel == null) {
			return "-";
		}
		return "Ich übernehme stattdessen das Spiel: " + (tauschSpiel.getDatum() == null ? "-" : tauschSpiel.getDatum())
				+ " " + (tauschSpiel.getZeit() == null ? "-" : tauschSpiel.getZeit()) + " | "
				+ (tauschSpiel.getHeim() == null ? "-" : tauschSpiel.getHeim()) + " - "
				+ (tauschSpiel.getGast() == null ? "-" : tauschSpiel.getGast());
	}

	private void sendeRuecknahmeEmail(String aktion) {
		if (vereinnr == null || vereinnr.isBlank() || eintrag == null) {
			return;
		}

		EmailService emailService = new EmailService(vereinnr,
				ConfigManager.getConfigValue(vereinnr, "email.jugendleiter"), null);
		String betreff = "Betreuer-Rückmeldung zurückgesetzt: " + safeText(eintrag.getBetreuer());
		String link = getGenerateBerichtUrl(vereinnr, eintrag.getUniqueKey());

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(
				"<body style='margin:0; padding:0; background:#f4f6f8; font-family:Arial, Helvetica, sans-serif; color:#333;'>");
		sb.append("<div style='max-width:600px; margin:0 auto; padding:20px 12px;'>");
		sb.append("<div style='background:#ffffff; border-radius:12px; padding:20px; ");
		sb.append("box-shadow:0 2px 8px rgba(0,0,0,0.08);'>");

		sb.append("<p style='margin:0 0 18px 0; font-size:16px; line-height:1.5;'>");
		sb.append("Hallo Jugendleiter,");
		sb.append("</p>");

		sb.append("<p style='margin:0 0 18px 0; font-size:16px; line-height:1.5;'>");
		sb.append("eine bereits bestätigte Betreuung wurde zurückgenommen.");
		sb.append("</p>");

		sb.append("<div style='background:#fff4f4; border:1px solid #f0caca; border-radius:10px; ");
		sb.append("padding:16px; margin:0 0 20px 0;'>");
		sb.append("<div style='font-size:18px; font-weight:bold; margin:0 0 12px 0; color:#b71c1c;'>");
		sb.append("📅 Spieldetails");
		sb.append("</div>");

		appendInfoBlock(sb, "Datum", eintrag.getDatum());
		appendInfoBlock(sb, "Uhrzeit", eintrag.getZeit());
		appendInfoBlock(sb, "Liga", eintrag.getLiga());
		appendInfoBlock(sb, "Spiel", safeText(eintrag.getHeim()) + " - " + safeText(eintrag.getGast()));
		appendInfoBlock(sb, "Betreuer", eintrag.getBetreuer());
		appendInfoBlock(sb, "Aktion", aktion);
		appendInfoBlock(sb, "Status", "Rückmeldung wurde zurückgesetzt (bestaetigt/kommentar = null).");

		sb.append("</div>");

		sb.append("<p style='margin:0 0 12px 0; font-size:15px; line-height:1.6;'>");
		sb.append("Link zur Betreuung:");
		sb.append("</p>");
		// Button
		sb.append("<div style='text-align:center; margin:24px 0 18px 0;'>");
		sb.append("<a href='").append(escapeHtmlAttribute(getGenerateBerichtUrl(vereinnr, eintrag.getUniqueKey())))
				.append("' ");
		sb.append("style='background:#2e7d32; color:#ffffff; text-decoration:none; ");
		sb.append("padding:16px 24px; border-radius:10px; font-size:17px; font-weight:bold; ");
		sb.append("display:block; max-width:320px; margin:0 auto;'>");
		sb.append("Betreuer ändern");
		sb.append("</a>");
		sb.append("</div>");

		sb.append("</div>");
		sb.append("</div>");
		sb.append("</body>");
		sb.append("</html>");

		String inhalt = sb.toString();
		try {
			emailService.sendEmail(vereinnr, betreff, inhalt, null, null, true);
			mailGesendetHinweis = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendeBetreuerGeaendertEmail(String aktion) {
		if (vereinnr == null || vereinnr.isBlank() || eintrag == null) {
			return;
		}

		EmailService emailService = new EmailService(vereinnr,
				ConfigManager.getConfigValue(vereinnr, "email.jugendleiter"), null);
		String betreff = "Betreuer geändert: " + safeText(eintrag.getBetreuer());
		String link = getGenerateBerichtUrl(vereinnr, eintrag.getUniqueKey());

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(
				"<body style='margin:0; padding:0; background:#f4f6f8; font-family:Arial, Helvetica, sans-serif; color:#333;'>");
		sb.append("<div style='max-width:600px; margin:0 auto; padding:20px 12px;'>");
		sb.append("<div style='background:#ffffff; border-radius:12px; padding:20px; ");
		sb.append("box-shadow:0 2px 8px rgba(0,0,0,0.08);'>");

		sb.append("<p style='margin:0 0 18px 0; font-size:16px; line-height:1.5;'>");
		sb.append("Hallo Jugendleiter,");
		sb.append("</p>");

		sb.append("<p style='margin:0 0 18px 0; font-size:16px; line-height:1.5;'>");
		sb.append("der Betreuer wurde für dieses Spiel geändert.");
		sb.append("</p>");

		sb.append("<div style='background:#fff4f4; border:1px solid #f0caca; border-radius:10px; ");
		sb.append("padding:16px; margin:0 0 20px 0;'>");
		sb.append("<div style='font-size:18px; font-weight:bold; margin:0 0 12px 0; color:#b71c1c;'>");
		sb.append("📅 Spieldetails");
		sb.append("</div>");

		appendInfoBlock(sb, "Datum", eintrag.getDatum());
		appendInfoBlock(sb, "Uhrzeit", eintrag.getZeit());
		appendInfoBlock(sb, "Liga", eintrag.getLiga());
		appendInfoBlock(sb, "Spiel", safeText(eintrag.getHeim()) + " - " + safeText(eintrag.getGast()));
		appendInfoBlock(sb, "Ursprünglicher Betreuer", initialBetreuer);
		appendInfoBlock(sb, "Neuer Betreuer", eintrag.getBetreuer());
		appendInfoBlock(sb, "Aktion", aktion);
		appendInfoBlock(sb, "Status", "Betreuer wurde geändert.");

		sb.append("</div>");

		sb.append("<p style='margin:0 0 12px 0; font-size:15px; line-height:1.6;'>");
		sb.append("Link zur Betreuung:");
		sb.append("</p>");
		// Button
		sb.append("<div style='text-align:center; margin:24px 0 18px 0;'>");
		sb.append("<a href='").append(escapeHtmlAttribute(getGenerateBerichtUrl(vereinnr, eintrag.getUniqueKey())))
				.append("' ");
		sb.append("style='background:#2e7d32; color:#ffffff; text-decoration:none; ");
		sb.append("padding:16px 24px; border-radius:10px; font-size:17px; font-weight:bold; ");
		sb.append("display:block; max-width:320px; margin:0 auto;'>");
		sb.append("Betreuerverwaltung");
		sb.append("</a>");
		sb.append("</div>");

		sb.append("</div>");
		sb.append("</div>");
		sb.append("</body>");
		sb.append("</html>");

		String inhalt = sb.toString();
		try {
			emailService.sendEmail(vereinnr, betreff, inhalt, null, null, true);
			mailGesendetHinweis = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void ladeBetreuerNamenFallsNoetig() {
		if (vereinnr == null || vereinnr.isBlank() || !betreuerNamen.isEmpty()) {
			return;
		}

		betreuerNamen.addAll(databaseService.ladeBetreuerNamenAusAdressliste(vereinnr));
		Collections.sort(betreuerNamen);
	}

	public boolean hatNameGeaendert() {
		String initial = initialBetreuer == null ? "" : initialBetreuer.trim();
		String selected = selectedBetreuer == null ? "" : selectedBetreuer.trim();
		return !initial.equals(selected);
	}

	public String getRueckmeldungButtonText() {
		if (isAbgelehnt()) {
			return "Speichern";
		}
		return hatNameGeaendert() ? "Speichern" : "Rückmeldung zurücknehmen";
	}

	private String baueKommentar(String buttonKommentar, String zusatzKommentarText) {
		String extra = zusatzKommentarText == null ? "" : zusatzKommentarText.trim();
		if (extra.isEmpty()) {
			return buttonKommentar;
		}
		return buttonKommentar + "\n" + extra;
	}

	private void sendeAblehnungsEmail(String buttonKommentar, String kommentarGesamt) {
		if (vereinnr == null || vereinnr.isBlank() || eintrag == null) {
			return;
		}

		EmailService emailService = new EmailService(vereinnr,
				ConfigManager.getConfigValue(vereinnr, "email.jugendleiter"), null);

		boolean istBestaetigt = Boolean.TRUE.equals(eintrag.getBestaetigt());
		String statusText = istBestaetigt ? "OK" : "Nein";

		String betreff = statusText + ": Betreuer-Rückmeldung " + safeText(eintrag.getBetreuer()) + " ("
				+ safeText(buttonKommentar) + ")";

		StringBuilder sb = new StringBuilder();

		sb.append("<html>");
		sb.append(
				"<body style='margin:0; padding:0; background:#f4f6f8; font-family:Arial, Helvetica, sans-serif; color:#333;'>");

		sb.append("<div style='max-width:600px; margin:0 auto; padding:20px 12px;'>");

		sb.append("<div style='background:#ffffff; border-radius:12px; padding:20px; ");
		sb.append("box-shadow:0 2px 8px rgba(0,0,0,0.08);'>");

		sb.append("<p style='margin:0 0 18px 0; font-size:16px; line-height:1.5;'>");
		sb.append("Hallo Jugendleiter,");
		sb.append("</p>");

		sb.append("<p style='margin:0 0 18px 0; font-size:16px; line-height:1.5;'>");
		sb.append("es gibt eine neue Rückmeldung zur Betreuung mit einer ");
		sb.append("<strong>Absage</strong>.");
		sb.append("</p>");

		// Info-Box
		sb.append("<div style='background:#fff4f4; border:1px solid #f0caca; border-radius:10px; ");
		sb.append("padding:16px; margin:0 0 20px 0;'>");

		sb.append("<div style='font-size:18px; font-weight:bold; margin:0 0 12px 0; color:#b71c1c;'>");
		sb.append("📅 Spieldetails");
		sb.append("</div>");

		appendInfoBlock(sb, "Datum", eintrag.getDatum());
		appendInfoBlock(sb, "Uhrzeit", eintrag.getZeit());
		appendInfoBlock(sb, "Liga", eintrag.getLiga());
		appendInfoBlock(sb, "Spiel", safeText(eintrag.getHeim()) + " - " + safeText(eintrag.getGast()));
		appendInfoBlock(sb, "Betreuer", eintrag.getBetreuer());
		appendInfoBlock(sb, "Rückmeldung", buttonKommentar);

		if (!Objects.equals(buttonKommentar, kommentarGesamt) && !isBlank(kommentarGesamt)) {
			appendInfoBlock(sb, "Kommentar", kommentarGesamt);
		}

		sb.append("</div>");

		sb.append("<p style='margin:0 0 22px 0; font-size:15px; line-height:1.6;'>");
		sb.append("Bitte die weitere Betreuung entsprechend einplanen.");
		sb.append("</p>");

		// Button
		sb.append("<div style='text-align:center; margin:24px 0 18px 0;'>");
		sb.append("<a href='").append(escapeHtmlAttribute(getGenerateBerichtUrl(vereinnr, eintrag.getUniqueKey())))
				.append("' ");
		sb.append("style='background:#2e7d32; color:#ffffff; text-decoration:none; ");
		sb.append("padding:16px 24px; border-radius:10px; font-size:17px; font-weight:bold; ");
		sb.append("display:block; max-width:320px; margin:0 auto;'>");
		sb.append("Betreuer ändern");
		sb.append("</a>");
		sb.append("</div>");

		sb.append("<p style='margin:0; font-size:15px; line-height:1.5;'>");
		sb.append("Viele Grüße");
		sb.append("</p>");

		sb.append("</div>");
		sb.append("</div>");
		sb.append("</body>");
		sb.append("</html>");

		String inhalt = sb.toString();

		try {
			emailService.sendEmail(vereinnr, betreff, inhalt, null, null, true);
			mailGesendetHinweis = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void appendInfoBlock(StringBuilder sb, String label, Object value) {
		sb.append("<div style='margin:0 0 12px 0;'>");

		sb.append("<div style='font-size:13px; color:#666; margin-bottom:2px;'>").append(escapeHtml(label))
				.append("</div>");

		sb.append("<div style='font-size:16px; color:#222; font-weight:bold; line-height:1.4;'>")
				.append(escapeHtml(safeText(value))).append("</div>");

		sb.append("</div>");
	}

	private String safeText(Object value) {
		return value == null ? "-" : String.valueOf(value);
	}

	private boolean isBlank(String text) {
		return text == null || text.isBlank();
	}

	private String escapeHtml(String text) {
		if (text == null) {
			return "-";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#39;");
	}

	private String escapeHtmlAttribute(String text) {
		return escapeHtml(text);
	}

	public GesamtspielplanEintrag getEintrag() {
		return eintrag;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getHeimOderAuswaerts() {
		if (eintrag == null || vereinnr == null) {
			return "-";
		}
		String vereinsPraefix = ConfigManager.getSpielplanVerein(vereinnr);
		if (eintrag.getHeim() != null && eintrag.getHeim().startsWith(vereinsPraefix)) {
			return "Heim";
		}
		return "Auswärts";
	}

	public String getGegner() {
		if (eintrag == null || vereinnr == null) {
			return "-";
		}
		String vereinsPraefix = ConfigManager.getSpielplanVerein(vereinnr);
		if (eintrag.getHeim() != null && eintrag.getHeim().startsWith(vereinsPraefix)) {
			return eintrag.getGast();
		}
		return eintrag.getHeim();
	}

	public String getZusatzKommentar() {
		return zusatzKommentar;
	}

	public void setZusatzKommentar(String zusatzKommentar) {
		this.zusatzKommentar = zusatzKommentar;
	}

	public boolean isRueckmeldungGesendet() {
		return rueckmeldungGesendet;
	}

	public String getBestaetigenText() {
		return BESTAETIGEN;

	}

	public String getAblehnenMitErsatzText() {
		return ABLEHNEN_MIT_ERSATZ;
	}

	public String getAblehnenOhneErsatzText() {
		return ABLEHNEN_OHNE_ERSATZ;
	}

	public String getSelectedBetreuer() {
		return selectedBetreuer;
	}

	public void setSelectedBetreuer(String selectedBetreuer) {
		this.selectedBetreuer = selectedBetreuer;
	}

	public boolean isAbgelehnt() {
		return eintrag == null || !Boolean.TRUE.equals(eintrag.getBestaetigt());
	}

	public String getSelectedTauschSpielUniqueKey() {
		return selectedTauschSpielUniqueKey;
	}

	public void setSelectedTauschSpielUniqueKey(String selectedTauschSpielUniqueKey) {
		this.selectedTauschSpielUniqueKey = selectedTauschSpielUniqueKey;
	}

	public String getGenerateBerichtUrl(String vereinnr, String uuid) {
		return getGenerateBerichtUrlLink(vereinnr, uuid);
	}

	public String getGenerateBerichtUrlLink(String vereinnr, String uuid) {
		String baseUrl = configManager.getProgrammUrl(vereinnr);
		DatabaseService dbService = new DatabaseService();
		String targetPage = baseUrl + "l/" + dbService.createShortLink("bestaetigung.xhtml" + "?uuid=" + uuid);

		return targetPage;
	}

	public boolean isMailGesendetHinweis() {
		return mailGesendetHinweis;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public void zurueck() {

	}

}
