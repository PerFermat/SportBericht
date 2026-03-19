package de.bericht.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import de.bericht.service.ConfigService;
import de.bericht.service.DatabaseService;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigEintrag;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
@ViewScoped
public class ConfigBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String vereinnr;
	private String emailPassword;
	private String encryptedPassword;
	private List<ConfigEintrag> configEintraege;

	private final ConfigService service = new ConfigService(); // Deine DAO/Service

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null) {
			try {
				FacesContext.getCurrentInstance().getExternalContext().redirect("fehlenderVerein.xhtml");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		configEintraege = service.ladeConfigEintraege(vereinnr);
	}

	public void speichern() {
		service.speichereConfigEintraege(vereinnr, configEintraege);
	}

	public void insertHomepage() {
		speichern();

		String domains = ConfigManager.getConfigValue(vereinnr, "wordpress.domains");
		String[] domainList = domains.split(",");
		String[] eintraege = { "beitragsbild", "datum", "domain", "filter", "symbol", "werbung" };
		DatabaseService dbService = new DatabaseService(vereinnr);

		for (String domain : domainList) {
			for (String eintrag : eintraege) {

				dbService.insertConfigEintrag(vereinnr, "wordpress." + domain + "." + eintrag, "");
			}
		}

		for (ConfigEintrag config : getConfigEintraege()) {

			if (config.getEintrag().startsWith("wordpress.") && config.getEintrag().endsWith(".domain")) {
				String homepage = config.getEintrag().split("\\.")[1];
				if (!domains.contains(homepage)) {
					for (String eintrag : eintraege) {
						dbService.deleteConfigEintrag(vereinnr, "wordpress." + homepage + "." + eintrag);
					}
				}
			}
		}
		configEintraege = service.ladeConfigEintraege(vereinnr);
	}

	// Getter & Setter
	public String getVereinnr() {
		return vereinnr;
	}

	public List<ConfigEintrag> getConfigEintraege() {
		return configEintraege;
	}

	public void encrypt() {

		try {
			encryptedPassword = ConfigManager.encryptPassword(vereinnr, emailPassword);
		} catch (Exception e) {
			encryptedPassword = "Fehler: " + e.getMessage();
		}
	}

	// Getter & Setter
	public String getEmailPassword() {
		return emailPassword;
	}

	public void setEmailPassword(String emailPassword) {
		this.emailPassword = emailPassword;
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
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

}
