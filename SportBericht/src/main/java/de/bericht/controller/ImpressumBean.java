package de.bericht.controller;

import java.io.Serializable;

import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
@ViewScoped
public class ImpressumBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String vereinnr;
	private static final String DEFAULT_TENNIS_SEITE = "liga.xhtml";
	private static final String DEFAULT_TISCHTENNIS_SEITE = "spielplan.xhtml";
	private static final String RUECKSPRUNG_STANDARD = "index.xhtml";

	private String ruecksprung;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = request.getParameter("vereinnr");
		ruecksprung = request.getParameter("ruecksprung");

	}

	public String getRuecksprung() {
		return ruecksprung;
	}

	public void setRuecksprung(String ruecksprung) {
		this.ruecksprung = ruecksprung;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public String zurueck() {
		if (ruecksprung != null && ruecksprung.matches("[a-zA-Z0-9_-]+\\.xhtml")) {
			return ruecksprung + "?faces-redirect=true";
		}
		if (isTennis()) {
			return DEFAULT_TENNIS_SEITE + "?faces-redirect=true";
		}
		if (isTischtennis()) {
			return DEFAULT_TISCHTENNIS_SEITE + "?faces-redirect=true";
		}
		return RUECKSPRUNG_STANDARD + "?faces-redirect=true";
	}

}
