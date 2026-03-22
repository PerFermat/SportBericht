package de.bericht.controller;

import java.io.Serializable;

import de.bericht.service.DatabaseService;
import de.bericht.service.SpielcodeEintrag;
import de.bericht.util.BerichtHelper;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("zwischenablageBean")
@ViewScoped
public class ZwischenablageBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private final DatabaseService databaseService = new DatabaseService();

	private String vereinnr;
	private String key;
	private SpielcodeEintrag spiel;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		key = request.getParameter("key");
		if (key == null || key.isBlank()) {
			key = request.getParameter("unique_key");
		}

		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}

		if (key == null || key.isBlank()) {
			return;
		}

		spiel = databaseService.ladeSpielcodeEintrag(key, vereinnr);
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public SpielcodeEintrag getSpiel() {
		return spiel;
	}

	public boolean isSpielGefunden() {
		return spiel != null;
	}

	public boolean isSpielcodeVorhanden() {
		return spiel != null && spiel.isSpielcodeGefunden();
	}
}
