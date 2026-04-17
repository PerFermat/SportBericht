package de.bericht.util;

import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

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
	private final String verschluesseltesPasswort;

	/**
	 * Liest automatisch den Cookie aus dem aktuellen JSF-Request.
	 */
	public LoginCookieDaten() {
		this(readCurrentRequestCookieValues());
	}

	private LoginCookieDaten(Map<String, String> daten) {
		this.verein = daten.get("verein");
		this.vereinnr = daten.get("vereinnr");
		this.name = daten.get("name");
		this.verschluesseltesPasswort = daten.get("pwd");
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
		return parseCookieValue(cookie.getValue());
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
		return Arrays.stream(cookieValue.split("&")).map(part -> part.split("=", 2)).filter(parts -> parts.length == 2)
				.collect(
						java.util.stream.Collectors.toMap(parts -> dec(parts[0]), parts -> dec(parts[1]), (a, b) -> b));
	}

	public String getVerein() {
		return verein;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getName() {
		return name;
	}

	public String getVerschluesseltesPasswort() {
		return verschluesseltesPasswort;
	}

	public String getPasswort() {
		try {
			return ConfigManager.decryptPasswort(vereinnr, verschluesseltesPasswort);
		} catch (Exception e) {
			return verschluesseltesPasswort;
		}
	}

	public boolean isVollstaendig() {
		return verein != null && !verein.isBlank() && vereinnr != null && !vereinnr.isBlank()
				&& verschluesseltesPasswort != null && !verschluesseltesPasswort.isBlank();
	}

	private static String dec(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
