package de.bericht.controller;

import java.io.Serializable;
import java.util.Base64;
import java.util.Map;

import de.bericht.service.DatabaseService;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServlet;

@Named
@ViewScoped
public class ImageBean extends HttpServlet implements Serializable {
	private static final long serialVersionUID = 1L;
	private String heim;
	private String gast;
	private String datum;
	private String vereinnr;
	private String ergebnis;
	private String berichtText;
	private String ergebnisLink;
	private String imagePath;
	private String liga;
	private String ligaSpiel;
	private String uuid;
	private String name;
	private String gruppeUrl;
	/** Vom Browser geliefertes, bereits zugeschnittenes + gefiltertes JPEG (Data-URI). */
	private String editedImageBase64;
	private DatabaseService dbService = new DatabaseService();

	@Override
	@PostConstruct
	public void init() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		this.vereinnr = params.get("vereinnr");
		this.heim = params.get("heim");
		this.gast = params.get("gast");
		this.datum = params.get("datum");
		this.ergebnis = params.get("ergebnis");
		this.ergebnisLink = params.get("ergebnisLink");
		this.berichtText = params.get("berichtText");
		this.liga = params.get("liga");
		this.ligaSpiel = params.get("ligaSpiel");
		this.uuid = params.get("uuid");
		this.name = params.get("name");
		this.gruppeUrl = params.get("gruppeUrl");
		imagePath = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(loadImageFromDatabase());
	}

	private byte[] loadImageFromDatabase() {
		return dbService.loadBerichtData(vereinnr, ergebnisLink).getBild();
	}

	public String getImagePath() {
		return imagePath;
	}

	public String getEditedImageBase64() {
		return editedImageBase64;
	}

	public void setEditedImageBase64(String editedImageBase64) {
		this.editedImageBase64 = editedImageBase64;
	}

	/**
	 * Speichert das im Browser zugeschnittene/gefilterte Bild (Base64) und entfernt
	 * die Bearbeitungssperre. Navigation erfolgt über den Button-action.
	 */
	public void speichern() {
		if (editedImageBase64 != null && editedImageBase64.contains(",")) {
			try {
				String base64Data = editedImageBase64.split(",")[1]; // Data-URI-Präfix entfernen
				byte[] bytes = Base64.getDecoder().decode(base64Data);
				dbService.saveBerichtData(vereinnr, ergebnisLink, bytes);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		dbService.deleteUUID(vereinnr, ergebnisLink, uuid);
	}

	public void updBearbeitung() {
		dbService.verarbeiteEintrag(vereinnr, name, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
	}

	public void zurueck() {
		dbService.deleteUUID(vereinnr, ergebnisLink, uuid);
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

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	public String getHeim() {
		return heim;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

	public String getGast() {
		return gast;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public String getDatum() {
		return datum;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
	}

	public String getBerichtText() {
		return berichtText;
	}

	public void setBerichtText(String berichtText) {
		this.berichtText = berichtText;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}

	public String getLigaSpiel() {
		return ligaSpiel;
	}

	public void setLigaSpiel(String ligaSpiel) {
		this.ligaSpiel = ligaSpiel;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
