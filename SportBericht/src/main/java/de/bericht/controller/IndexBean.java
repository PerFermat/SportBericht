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

import de.bericht.service.DatabaseService;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Named("indexBean")
@ViewScoped
public class IndexBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String LOGIN_COOKIE = "sportbericht_login";
	private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365 * 10;

	private final DatabaseService db = new DatabaseService();

	private Map<String, String> vereinZuNr;
	private List<String> vereine;
	private List<String> namen;

	private String selectedVerein;
	private String selectedVereinnr;
	private String selectedName;
	private String passwort;

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
		onVereinChange();
		if (selectedVereinnr == null || selectedVereinnr.isBlank()) {
			addError("Bitte einen Verein auswählen.");
			return null;
		}
		if (passwort == null || passwort.isBlank()) {
			addError("Bitte ein Passwort eingeben.");
			return null;
		}
		System.out.println(ConfigManager.getUserPasswort(selectedVereinnr));
		System.out.println(passwort);

		if (!Objects.equals(ConfigManager.getUserPasswort(selectedVereinnr), passwort)) {
			addError("Passwort ist falsch.");
			return null;
		}
		speichereCookie();
		weiterleitenOhneUrlAenderung(selectedVereinnr, selectedVerein);
		return null;
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
				return;
			}
			String decrypted = ConfigManager.decryptPasswort(vereinnr, encryptedPasswort);
			String expected = ConfigManager.getUserPasswort(vereinnr);
			if (!Objects.equals(decrypted, expected)) {
				return;
			}
			List<String> gefundeneNamen = db.ladeBetreuerNamenAusAdressliste(vereinnr);
			if (!gefundeneNamen.isEmpty() && name != null && !name.isBlank() && !gefundeneNamen.contains(name)) {
				return;
			}
			selectedVerein = verein;
			selectedVereinnr = vereinnr;
			selectedName = name;
			weiterleitenOhneUrlAenderung(vereinnr, verein);
		} catch (Exception ignored) {
		}
	}

	private void weiterleitenOhneUrlAenderung(String vereinnr, String verein) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext == null) {
			return;
		}
		try {
			ExternalContext ec = facesContext.getExternalContext();
			HttpServletRequest request = (HttpServletRequest) ec.getRequest();
			HttpServletResponse response = (HttpServletResponse) ec.getResponse();
			String sportart = ConfigManager.getConfigValue(vereinnr, "sportart.verein");
			String ziel = "/spielplan.xhtml";
			if (sportart != null && sportart.equalsIgnoreCase("TENNIS")) {
				ziel = "/liga.xhtml";
			}
			String encodedVerein = URLEncoder.encode(verein, StandardCharsets.UTF_8);
			RequestDispatcher dispatcher = request.getRequestDispatcher(ziel + "?v=" + encodedVerein);
			dispatcher.forward(request, response);
			facesContext.responseComplete();
		} catch (IOException | ServletException e) {
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
			cookie.setPath("/");
			cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
			response.addCookie(cookie);
		} catch (Exception e) {
			addError("Cookie konnte nicht gespeichert werden.");
		}
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
}
