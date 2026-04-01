package de.bericht.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.bericht.service.ConfigHtml;
import de.bericht.service.ConfigHtmlUrl;
import de.bericht.service.DatabaseService;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigHtmlModel;
import de.bericht.util.ConfigHtmlUrlModel;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
@ViewScoped
public class ConfigHtmlBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private final DatabaseService databaseService = new DatabaseService();
	private final List<ConfigHtmlModel> configHtmlEintraege = new ArrayList<>();

	private String vereinnr;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null || vereinnr.isBlank()) {
			try {
				FacesContext.getCurrentInstance().getExternalContext().redirect("fehlenderVerein.xhtml");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		ladeDaten();
	}

	private void ladeDaten() {
		configHtmlEintraege.clear();
		List<ConfigHtml> zeilen = databaseService.ladeConfigHtml(vereinnr);
		Map<Integer, List<ConfigHtmlUrl>> urlsByHtmlId = databaseService.ladeConfigHtmlUrlsByHtmlId(vereinnr);

		for (ConfigHtml zeile : zeilen) {
			ConfigHtmlModel model = new ConfigHtmlModel();
			model.setId(zeile.getId());
			model.setArt(zeile.getArt());
			model.setDateiname(zeile.getDateiname());
			List<ConfigHtmlUrl> urls = urlsByHtmlId.getOrDefault(zeile.getId(), List.of());
			for (ConfigHtmlUrl url : urls) {
				ConfigHtmlUrlModel child = new ConfigHtmlUrlModel();
				child.setId(url.getId());
				child.setUeberschrift(url.getUeberschrift());
				child.setUrl(url.getUrl());
				child.setTyp(url.getTyp());
				child.setLiga(url.isLiga());
				model.getUrls().add(child);
			}
			configHtmlEintraege.add(model);
		}
	}

	public void addConfigHtml() {
		ConfigHtmlModel model = new ConfigHtmlModel();
		model.setExpanded(true);
		configHtmlEintraege.add(model);
	}

	public void removeConfigHtml(ConfigHtmlModel model) {
		if (model == null) {
			return;
		}
		configHtmlEintraege.remove(model);
	}

	public void toggleExpanded(ConfigHtmlModel model) {
		if (model == null) {
			return;
		}
		model.setExpanded(!model.isExpanded());
	}

	public void addUrl(ConfigHtmlModel model) {
		if (model == null) {
			return;
		}
		model.setExpanded(true);
		model.getUrls().add(new ConfigHtmlUrlModel());
	}

	public void removeUrl(ConfigHtmlModel model, ConfigHtmlUrlModel url) {
		if (model == null || url == null) {
			return;
		}
		model.getUrls().remove(url);
	}

	public void speichern() {
		for (int i = 0; i < configHtmlEintraege.size(); i++) {
			ConfigHtmlModel model = configHtmlEintraege.get(i);
			if (isBlank(model.getArt())) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Pflichtfeld fehlt", "Bitte in Zeile " + (i + 1) + " das Feld 'Art' ausfüllen."));
				return;
			}
			if (isBlank(model.getDateiname())) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Pflichtfeld fehlt", "Bitte in Zeile " + (i + 1) + " das Feld 'Dateiname' ausfüllen."));
				return;
			}
			for (int j = 0; j < model.getUrls().size(); j++) {
				ConfigHtmlUrlModel url = model.getUrls().get(j);
				if (isBlank(url.getUrl())) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Pflichtfeld fehlt",
							"Bitte in Zeile " + (i + 1) + ", Eintrag " + (j + 1) + " das Feld 'URL' ausfüllen."));
					return;
				}
				if (isBlank(url.getTyp())) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Pflichtfeld fehlt",
							"Bitte in Zeile " + (i + 1) + ", Eintrag " + (j + 1) + " das Feld 'Typ' ausfüllen."));
					return;
				}
			}
		}

		List<ConfigHtml> payload = new ArrayList<>();
		for (ConfigHtmlModel model : configHtmlEintraege) {
			ConfigHtml row = new ConfigHtml();
			row.setVereinnr(vereinnr);
			row.setArt(trimToEmpty(model.getArt()));
			row.setDateiname(trimToEmpty(model.getDateiname()));
			for (ConfigHtmlUrlModel urlModel : model.getUrls()) {
				ConfigHtmlUrl child = new ConfigHtmlUrl();
				child.setUeberschrift(trimToEmpty(urlModel.getUeberschrift()));
				child.setUrl(trimToEmpty(urlModel.getUrl()));
				child.setTyp(trimToEmpty(urlModel.getTyp()));
				child.setLiga(urlModel.isLiga());
				row.getUrls().add(child);
			}
			payload.add(row);
		}

		databaseService.speichereConfigHtmlKonfiguration(vereinnr, payload);
		ladeDaten();
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Gespeichert",
				"Die HTML-Konfiguration wurde gespeichert."));
	}

	private boolean isBlank(String text) {
		return text == null || text.trim().isEmpty();
	}

	private String trimToEmpty(String text) {
		return text == null ? "" : text.trim();
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

	public List<ConfigHtmlModel> getConfigHtmlEintraege() {
		return configHtmlEintraege;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public List<String> getArtOptionen() {
		return List.of("Spielplan", "Mannschaft", "Seitenleiste", "Spielcodes", "Kinderfotos");
	}

	public List<String> getTypOptionen() {
		return List.of("Tabelle", "Spielplan", "Verzeichnis", "Datenbank");
	}

}
