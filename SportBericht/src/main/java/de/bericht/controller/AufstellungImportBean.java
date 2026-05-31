package de.bericht.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bericht.service.Aufstellung;
import de.bericht.service.AufstellungService;
import de.bericht.service.DatabaseService;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.LoginCookieDaten;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("aufstellungImportBean")
@ViewScoped
public class AufstellungImportBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Pattern LIGA_PATTERN = Pattern.compile("^([A-Za-z]+\\s*\\d*|[A-Za-z]\\d+)");

	private final DatabaseService databaseService = new DatabaseService();
	private String vereinnr;
	private String passwort;
	private String liga;
	private String url;
	private boolean angemeldet;
	private List<String> ligaOptionen = new ArrayList<>();

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		lesenCookieParameter();

		String userPasswort = ConfigManager.getUserPasswort(vereinnr);
		String adminPasswort = ConfigManager.getAdminPasswort(vereinnr);
		angemeldet = userPasswort.equals(passwort) || adminPasswort.equals(passwort);
		if (angemeldet) {
			ladeLigaOptionen();
		}
	}

	private void lesenCookieParameter() {
		LoginCookieDaten logging = new LoginCookieDaten();
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = logging.getVereinnr();
			if (passwort == null) {
				passwort = logging.getPasswort();
			}
		} else if (passwort == null && vereinnr.equals(logging.getVereinnr())) {
			passwort = logging.getPasswort();
		}
	}

	private void ladeLigaOptionen() {
		Set<String> optionen = new LinkedHashSet<>();
		for (Map<String, String> row : databaseService.ladeVerfuegbarkeitSpiele(vereinnr)) {
			String ligaWert = row.get("liga");
			String kuerzel = extrahiereLigaPrefix(ligaWert);
			if (kuerzel != null && !kuerzel.isBlank()) {
				optionen.add(kuerzel.trim());
			}
		}
		ligaOptionen = optionen.stream().sorted().toList();
	}

	private String extrahiereLigaPrefix(String ligaWert) {
		if (ligaWert == null) {
			return null;
		}
		String trimmed = ligaWert.trim();
		Matcher matcher = LIGA_PATTERN.matcher(trimmed);
		if (!matcher.find()) {
			return trimmed;
		}
		return matcher.group(1).trim().replaceAll("\\s+", " ");
	}

	public void importieren() {
		if (!angemeldet) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Nicht autorisiert."));
			return;
		}
		if (liga == null || liga.isBlank() || url == null || url.isBlank()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Hinweis", "Liga und URL sind Pflichtfelder."));
			return;
		}

		List<Aufstellung> aufstellungen = new ArrayList<>();
		AufstellungService as = new AufstellungService(liga.trim(), url.trim());
		aufstellungen.addAll(as.getAufstellungen());
		databaseService.saveAufstellungenFuerLiga(vereinnr, liga.trim(), aufstellungen);

		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolgreich",
				aufstellungen.size() + " Aufstellungs-Einträge importiert."));
	}

	public List<Aufstellung> getGefilterteAufstellungen() {
		List<Aufstellung> ergebnis = new ArrayList<>();
		for (Map<String, String> row : databaseService.ladeAufstellungRows(vereinnr)) {
			String mannschaft = trimToEmpty(row.get("mannschaft"));
			if (liga != null && !liga.isBlank() && !liga.trim().equalsIgnoreCase(mannschaft)) {
				continue;
			}
			ergebnis.add(new Aufstellung(0, mannschaft, trimToEmpty(row.get("rang")), trimToEmpty(row.get("qttr")),
					trimToEmpty(row.get("name")), trimToEmpty(row.get("a")), trimToEmpty(row.get("status"))));
		}
		return ergebnis;
	}

	private String trimToEmpty(String value) {
		return value == null ? "" : value.trim();
	}

	public String getPreviewUrl() {
		if (url == null || url.isBlank()) {
			return null;
		}
		String trimmed = url.trim();
		if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
			return trimmed;
		}
		return null;
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getLigaOptionen() {
		return ligaOptionen;
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return !ConfigManager.isTennis(vereinnr);
	}
}
