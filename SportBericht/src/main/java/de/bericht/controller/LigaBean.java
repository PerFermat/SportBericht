package de.bericht.controller;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.bericht.service.DatabaseService;
import de.bericht.service.Liga;
import de.bericht.service.LigaService;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.ErgebnisCache;
import de.bericht.util.LoginCookieDaten;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("ligaBean")
@ViewScoped
public class LigaBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String SESSION_VEREINNR = "sportbericht_vereinnr";
	private List<Liga> ligen;
	private String verein;

	private String berichtDatum;
	private Date berichtDatumCal;
	private String vereinnr;
	DatabaseService db = new DatabaseService();
	ConfigManager config = ConfigManager.getInstance();

	private String freierText;

	public LigaBean() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		String name = request.getParameter("v");
		vereinnr = BerichtHelper.bestimmenVereinnr(name);

		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		lesenCookieParameter();
		String url = ConfigManager.getSpielplanURL(vereinnr);

		LigaService ls = new LigaService(url);
		ligen = ls.getLigen();
		verein = ls.getVerein();

	}

	private void lesenCookieParameter() {
		LoginCookieDaten logging = new LoginCookieDaten();
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = logging.getVereinnr();
		}
	}

	public List<Liga> getLigen() {
		return ligen;
	}

	public void setBerichtDatum(String berichtDatum) {
		this.berichtDatum = berichtDatum;
	}

	public String getFreierText() {
		return freierText;
	}

	public void setFreierText(String freierText) {
		this.freierText = freierText;
	}

	public void onDateSelect() {
		if (berichtDatumCal != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
			berichtDatum = sdf.format(berichtDatumCal);
		}
	}

	// Getter und Setter
	public Date getBerichtDatumCal() {
		return berichtDatumCal;
	}

	public void setBerichtDatumCal(Date berichtDatumCal) {
		this.berichtDatumCal = berichtDatumCal;
		onDateSelect(); // Automatische Konvertierung nach der Eingabe
	}

	public String getBerichtDatum() {
		return berichtDatum;
	}

	public String getVerein() {
		return verein;
	}

	public void setVerein(String verein) {
		this.verein = verein;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public void updConfig() {
		ErgebnisCache.cacheLeeren();
		BerichtHelper.clearCacheForVerein(vereinnr);
		ConfigManager.updInstance();
	}

	public String getupdConfig() {
		ErgebnisCache.cacheLeeren();
		BerichtHelper.clearCacheForVerein(vereinnr);
		ConfigManager.updInstance();
		return "";
	}

	public void setUpdConfig(String ergebnis) {
	}

	public boolean hasFreigabe(String ergebnisLink) {
		return BerichtHelper.hasFreigabe(vereinnr, ergebnisLink);
	}

	public String getFreierLink() {
		return berichtDatum + "-" + freierText;
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
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
