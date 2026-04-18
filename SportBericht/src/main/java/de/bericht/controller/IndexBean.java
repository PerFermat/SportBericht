package de.bericht.controller;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.bericht.service.DatabaseService;
import de.bericht.service.EmailService;
import de.bericht.util.AdressEintrag;
import de.bericht.util.ConfigEintrag;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Named("indexBean")
@ViewScoped
public class IndexBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String LOGIN_COOKIE = "sportbericht_login";
	private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365 * 10;
	private static final String SESSION_VEREINNR = "sportbericht_vereinnr";
	private static final String SESSION_VEREIN = "sportbericht_verein";
	private static final Pattern EMAIL_TRENNZEICHEN = Pattern.compile("[,;\\s]+");

	private final DatabaseService db = new DatabaseService();

	private Map<String, String> vereinZuNr;
	private List<String> vereine;
	private List<String> namen;

	private String selectedVerein;
	private String selectedVereinnr;
	private String selectedName;
	private String passwort;
	private boolean passwortVergessenSichtbar;
	private String passwortVergessenEmail;

	@PostConstruct
	public void init() {
		vereinZuNr = db.ladeLoginOrteMitVereinnr();
		vereine = new ArrayList<>(vereinZuNr.keySet());
		namen = new ArrayList<>();
		pruefeCookieUndRedirect();
	}

	public void onVereinChange() {
		selectedVereinnr = vereinZuNr.get(selectedVerein);
		if (selectedVereinnr == null || selectedVereinnr.isBlank()) {
			namen = new ArrayList<>();
			selectedName = null;
			return;
		}
		namen = db.ladeBetreuerNamenAusAdressliste(selectedVereinnr);
		if (!namen.contains(selectedName)) {
			selectedName = null;
		}
	}

	public String anmelden() {
		selectedVereinnr = vereinZuNr.get(selectedVerein);
		if (selectedVereinnr == null || selectedVereinnr.isBlank()) {
			addError("Bitte einen Verein auswählen.");
			return null;
		}
		if (passwort == null || passwort.isBlank()) {
			addError("Bitte ein Passwort eingeben.");
			return null;
		}
		if (selectedName == null || selectedName.isBlank()) {
			addError("Bitte ein Name eingeben.");
			return null;
		}

		if (!(Objects.equals(ConfigManager.getUserPasswort(selectedVereinnr), passwort)
				|| Objects.equals(ConfigManager.getAdminPasswort(selectedVereinnr), passwort))) {
			addError("Passwort ist falsch.");
			return null;
		}
		speichereCookie();
		weiterleitenOhneUrlAenderung(selectedVereinnr, selectedVerein);
		return null;
	}

	public void zeigePasswortVergessen() {
		passwortVergessenSichtbar = true;
	}

	public void passwortVergessenSenden() {
		selectedVereinnr = vereinZuNr.get(selectedVerein);
		if (selectedVereinnr == null || selectedVereinnr.isBlank()) {
			addError("Bitte zuerst einen Verein auswählen.");
			return;
		}

		String email = passwortVergessenEmail == null ? "" : passwortVergessenEmail.trim();
		if (email.isBlank()) {
			addError("Bitte eine E-Mail-Adresse eingeben.");
			return;
		}

		if (!istEmailInConfigOderAdressliste(selectedVereinnr, email)) {
			String berechtigung = ConfigManager.getConfigValue(selectedVereinnr, "config.berechtigung");
			addError("e-Mail unbekannt. Bitte wende dich an den Admin "
					+ (berechtigung == null || berechtigung.isBlank() ? "-" : berechtigung));
			return;
		}

		String userPasswort = ConfigManager.getUserPasswort(selectedVereinnr);
		boolean adminMitsenden = istInConfigBerechtigung(selectedVereinnr, email);
		StringBuilder nachricht = new StringBuilder();
		nachricht.append("Dein user.passwort lautet: ").append(userPasswort == null ? "" : userPasswort);
		if (adminMitsenden) {
			String adminPasswort = ConfigManager.getAdminPasswort(selectedVereinnr);
			nachricht.append("<br/>Dein admin.passwort lautet: ").append(adminPasswort == null ? "" : adminPasswort);
		}

		try {
			new EmailService(selectedVereinnr, "TO:" + email).sendEmail(selectedVereinnr, "Passwort SportBericht",
					nachricht.toString(), null, null, false);
			addInfo("Passwort wurde an " + email + " gesendet.");
		} catch (MessagingException e) {
			addError("E-Mail konnte nicht versendet werden.");
		}
	}

	private boolean istEmailInConfigOderAdressliste(String vereinnr, String email) {
		if (istEmailInConfig(vereinnr, email)) {
			return true;
		}
		List<AdressEintrag> eintraege = db.ladeAdressEintraege(vereinnr);
		for (AdressEintrag eintrag : eintraege) {
			if (email.equalsIgnoreCase(nullToEmpty(eintrag.getEmailPrivat()).trim())
					|| email.equalsIgnoreCase(nullToEmpty(eintrag.getEmailGesch()).trim())) {
				return true;
			}
		}
		return false;
	}

	private boolean istEmailInConfig(String vereinnr, String email) {
		List<ConfigEintrag> eintraege = db.ladeConfigEintraege(vereinnr);
		for (ConfigEintrag eintrag : eintraege) {
			if (enthaeltEmail(eintrag.getWert(), email)) {
				return true;
			}
		}
		return false;
	}

	private boolean istInConfigBerechtigung(String vereinnr, String email) {
		String berechtigung = ConfigManager.getConfigValue(vereinnr, "config.berechtigung");
		return enthaeltEmail(berechtigung, email);
	}

	private boolean enthaeltEmail(String wert, String email) {
		if (wert == null || wert.isBlank() || email == null || email.isBlank()) {
			return false;
		}
		for (String teil : EMAIL_TRENNZEICHEN.split(wert.trim())) {
			if (email.equalsIgnoreCase(teil.trim())) {
				return true;
			}
		}
		return false;
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private void pruefeCookieUndRedirect() {
		Cookie cookie = findeCookie();
		if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
			return;
		}
		try {
			Map<String, String> data = parseCookie(cookie.getValue());
			String verein = data.get("verein");
			String vereinnr = data.get("vereinnr");
			String encryptedPasswort = data.get("pwd");
			String name = data.get("name");
			if (verein == null || vereinnr == null || encryptedPasswort == null) {
				loescheCookie();
				return;
			}
			String decrypted = ConfigManager.decryptPasswort(vereinnr, encryptedPasswort);
			String expectedUser = ConfigManager.getUserPasswort(vereinnr);
			String expectedAdmin = ConfigManager.getAdminPasswort(vereinnr);
			if (!(Objects.equals(decrypted, expectedUser) || Objects.equals(decrypted, expectedAdmin))) {
				loescheCookie();
				return;
			}
			String vereinAusMap = findeVereinZuVereinnr(vereinnr);
			if (vereinAusMap != null && !vereinAusMap.isBlank()) {
				verein = vereinAusMap;
			}

			selectedVerein = verein;
			selectedVereinnr = vereinnr;
			selectedName = name;
			weiterleitenOhneUrlAenderung(vereinnr, verein);
		} catch (Exception ignored) {
			loescheCookie();
		}
	}

	private void weiterleitenOhneUrlAenderung(String vereinnr, String verein) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext == null) {
			return;
		}
		try {
			ExternalContext ec = facesContext.getExternalContext();
			Object sessionObj = ec.getSession(true);
			if (sessionObj instanceof HttpSession session) {
				session.setAttribute(SESSION_VEREINNR, vereinnr);
				session.setAttribute(SESSION_VEREIN, verein);
			}

			String sportart = ConfigManager.getConfigValue(vereinnr, "sportart.verein");
			String ziel = "/spielplan.xhtml";
			if (sportart != null && sportart.equalsIgnoreCase("TENNIS")) {
				ziel = "/liga.xhtml";
			}
			if (ec.getRequestContextPath().toUpperCase().contains("BERICHT")) {
				ec.redirect(ziel.startsWith("/") ? ziel.substring(1) : ziel);
			} else {
				ec.redirect(ziel);
			}
			facesContext.responseComplete();
		} catch (IOException e) {
			addError("Weiterleitung ist fehlgeschlagen.");
		}
	}

	public List<String> completeNamen(String query) {
		if (namen == null || namen.isEmpty() || query == null || query.isBlank()) {
			return Collections.emptyList();
		}
		String q = query.trim().toLowerCase();
		return namen.stream().filter(name -> name != null && name.toLowerCase().contains(q)).toList();
	}

	private void speichereCookie() {
		try {
			String encrypted = ConfigManager.encryptPasswort(selectedVereinnr, passwort);
			String value = "verein=" + enc(selectedVerein) + "&vereinnr=" + enc(selectedVereinnr) + "&name="
					+ enc(selectedName == null ? "" : selectedName) + "&pwd=" + enc(encrypted);
			ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
			HttpServletResponse response = (HttpServletResponse) ec.getResponse();
			Cookie cookie = new Cookie(LOGIN_COOKIE, value);
			cookie.setHttpOnly(true);
			String contextPath = ec.getRequestContextPath();
			cookie.setPath(contextPath == null || contextPath.isBlank() ? "/" : contextPath);

			cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
			response.addCookie(cookie);
		} catch (Exception e) {
			addError("Cookie konnte nicht gespeichert werden.");
		}
	}

	private void loescheCookie() {
		try {
			ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
			HttpServletResponse response = (HttpServletResponse) ec.getResponse();
			Cookie cookie = new Cookie(LOGIN_COOKIE, "");
			String contextPath = ec.getRequestContextPath();
			cookie.setPath(contextPath == null || contextPath.isBlank() ? "/" : contextPath);
			cookie.setMaxAge(0);
			cookie.setHttpOnly(true);
			response.addCookie(cookie);
		} catch (Exception ignored) {
		}
	}

	private String findeVereinZuVereinnr(String vereinnr) {
		if (vereinZuNr == null || vereinnr == null || vereinnr.isBlank()) {
			return null;
		}
		return vereinZuNr.entrySet().stream().filter(entry -> Objects.equals(entry.getValue(), vereinnr))
				.map(Map.Entry::getKey).findFirst().orElse(null);
	}

	private Map<String, String> parseCookie(String value) {
		return Arrays.stream(value.split("&")).map(part -> part.split("=", 2)).filter(arr -> arr.length == 2)
				.collect(java.util.stream.Collectors.toMap(arr -> dec(arr[0]), arr -> dec(arr[1]), (a, b) -> b));
	}

	private Cookie findeCookie() {
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
				.getRequest();
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (LOGIN_COOKIE.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	private String enc(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private String dec(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private void addError(String text) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, text, text));
	}

	public List<String> getVereine() {
		return vereine;
	}

	public List<String> getNamen() {
		return namen;
	}

	public String getSelectedVerein() {
		return selectedVerein;
	}

	public void setSelectedVerein(String selectedVerein) {
		this.selectedVerein = selectedVerein;
	}

	public String getSelectedName() {
		return selectedName;
	}

	public void setSelectedName(String selectedName) {
		this.selectedName = selectedName;
	}

	public String getPasswort() {
		return passwort;
	}

	public void setPasswort(String passwort) {
		this.passwort = passwort;
	}

	public boolean isNamenVorhanden() {
		return namen != null && !namen.isEmpty();
	}

	private void addInfo(String text) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, text, text));
	}

	public String getVereinnr() {
		return selectedVereinnr == null ? "13014" : selectedVereinnr;
	}

	public boolean isPasswortVergessenSichtbar() {
		return passwortVergessenSichtbar;
	}

	public String getPasswortVergessenEmail() {
		return passwortVergessenEmail;
	}

	public void setPasswortVergessenEmail(String passwortVergessenEmail) {
		this.passwortVergessenEmail = passwortVergessenEmail;
	}

}
