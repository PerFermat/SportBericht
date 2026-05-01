package de.bericht.util;

import java.io.Serializable;
import java.util.Map;

import de.bericht.service.DatabaseService;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Liest den Login-Cookie der index.xhtml ein und stellt dessen Werte per Getter
 * bereit.
 */
public class LoginCookieDaten implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String LOGIN_COOKIE_NAME = "sportbericht_login";

	private final String verein;
	private final String vereinnr;
	private final String name;
	private final String token;
	private final String passwortArt;

	/**
	 * Liest automatisch den Cookie aus dem aktuellen JSF-Request.
	 */
	public LoginCookieDaten() {
		this(readCurrentRequestCookieValues());
	}

	private LoginCookieDaten(Map<String, String> daten) {
		this.token = daten.get("token");
		Map<String, String> tokenDaten = token == null || token.isBlank() ? null
				: new DatabaseService().ladeLoginToken(token);
		this.vereinnr = tokenDaten == null ? null : tokenDaten.get("vereinnr");
		this.name = tokenDaten == null ? null : tokenDaten.get("name");
		this.verein = vereinnr == null ? null : BerichtHelper.getOrt(vereinnr);
		this.passwortArt = tokenDaten == null ? "USER" : tokenDaten.getOrDefault("passwort_art", "USER");

	}

	public static LoginCookieDaten fromCookieValue(String cookieValue) {
		if (cookieValue == null || cookieValue.isBlank()) {
			return null;
		}
		return new LoginCookieDaten(parseCookieValue(cookieValue));
	}

	public static LoginCookieDaten fromCookie(Cookie cookie) {
		if (cookie == null) {
			return null;
		}
		return fromCookieValue(cookie.getValue());
	}

	private static Map<String, String> readCurrentRequestCookieValues() {
		Cookie cookie = findCookieInCurrentRequest();
		if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
			return Map.of();
		}
		return Map.of("token", cookie.getValue());
	}

	private static Cookie findCookieInCurrentRequest() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext == null) {
			return null;
		}
		Object requestObj = facesContext.getExternalContext().getRequest();
		if (!(requestObj instanceof HttpServletRequest request) || request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (LOGIN_COOKIE_NAME.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	private static Map<String, String> parseCookieValue(String cookieValue) {
		return Map.of("token", cookieValue);
	}

	public String getToken() {
		return token;

	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getVerein() {
		return verein;

	}

	public String getName() {
		return name;

	}

	public String getPasswort() {
		if (vereinnr == null) {
			return null;
		}
		if ("ADMIN".equalsIgnoreCase(passwortArt)) {
			return ConfigManager.getAdminPasswort(vereinnr);
		}
		return ConfigManager.getUserPasswort(vereinnr);

	}

	public boolean isVollstaendig() {
		return token != null && !token.isBlank();
	}

}
