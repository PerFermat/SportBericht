package de.bericht.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import de.bericht.service.ConfigService;
import de.bericht.service.DatabaseService;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigBedeutung;
import de.bericht.util.ConfigEintrag;
import de.bericht.util.ConfigKategorie;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Named
@ViewScoped
public class ConfigBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String vereinnr;
	private String emailPassword;
	private String encryptedPassword;
	private List<ConfigEintrag> configEintraege;
	private Map<String, ConfigBedeutung> configBedeutungen;
	private Map<String, List<String>> configKategorieMap;
	private String suchText;
	private String ausgewaehlteKategorie;
	private List<String> verfuegbareKategorien = new ArrayList<>();
	


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
		ladeConfigEintraegeMitBedeutung();
	}

	public void speichern() {
		service.speichereConfigEintraege(vereinnr, configEintraege);
		ladeConfigEintraegeMitBedeutung();		
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
		ladeConfigEintraegeMitBedeutung();		
	}

	// Getter & Setter
	public String getVereinnr() {
		return vereinnr;
	}

	public List<ConfigEintrag> getConfigEintraege() {
		return configEintraege;
	}
	
	private void ladeConfigEintraegeMitBedeutung() {
		configEintraege = service.ladeConfigEintraege(vereinnr);
		configBedeutungen = service.ladeConfigBedeutungen();
		List<ConfigKategorie> kategorien = service.ladeConfigKategorien();
		configKategorieMap = kategorien.stream().collect(Collectors.groupingBy(ConfigKategorie::getConfigEintrag,
				Collectors.mapping(ConfigKategorie::getKategorie, Collectors.toList())));
		Set<String> kategorieSet = new LinkedHashSet<>();
		for (ConfigKategorie kategorie : kategorien) {
			kategorieSet.add(kategorie.getKategorie());
		}
		verfuegbareKategorien = new ArrayList<>(kategorieSet);
		for (ConfigEintrag eintrag : configEintraege) {
			ConfigBedeutung bedeutung = findeConfigBedeutung(eintrag.getEintrag());
			if (bedeutung != null) {
				eintrag.setBedeutung(bedeutung.getBedeutung());
				eintrag.setInhaltformat(bedeutung.getInhaltformat());
				eintrag.setWertebereich(bedeutung.getWertebereich());
			}
			List<String> trefferKategorien = findeKategorien(eintrag.getEintrag());
			if (trefferKategorien != null && !trefferKategorien.isEmpty()) {
				eintrag.setKategorien(String.join(", ", trefferKategorien));
			}
		}
	}

	private ConfigBedeutung findeConfigBedeutung(String eintrag) {
		if (configBedeutungen == null || eintrag == null) {
			return null;
		}
		ConfigBedeutung exakt = configBedeutungen.get(eintrag);
		if (exakt != null) {
			return exakt;
		}

		String wildcard = eintrag.replaceAll("^wordpress\\.[^.]+\\.", "wordpress.*.");
		return configBedeutungen.get(wildcard);
	}

	private List<String> findeKategorien(String eintrag) {
		if (configKategorieMap == null || eintrag == null) {
			return null;
		}
		List<String> exakt = configKategorieMap.get(eintrag);
		if (exakt != null) {
			return exakt;
		}
		String wildcard = eintrag.replaceAll("^wordpress\\.[^.]+\\.", "wordpress.*.");
		return configKategorieMap.get(wildcard);
	}

	public List<ConfigEintrag> getGefilterteConfigEintraege() {
		return configEintraege.stream().filter(this::passtZurKategorie).filter(this::passtZurSuche).collect(Collectors.toList());
	}

	private boolean passtZurKategorie(ConfigEintrag eintrag) {
		if (ausgewaehlteKategorie == null || ausgewaehlteKategorie.isBlank()) {
			return true;
		}
		return eintrag.getKategorien() != null && eintrag.getKategorien().toLowerCase().contains(ausgewaehlteKategorie.toLowerCase());
	}

	private boolean passtZurSuche(ConfigEintrag eintrag) {
		if (suchText == null || suchText.isBlank()) {
			return true;
		}
		String needle = suchText.toLowerCase();
		return containsIgnoreCase(eintrag.getEintrag(), needle) || containsIgnoreCase(eintrag.getWert(), needle)
				|| containsIgnoreCase(eintrag.getBedeutung(), needle);
	}

	private boolean containsIgnoreCase(String text, String needle) {
		return text != null && text.toLowerCase().contains(needle);
	}

	public String getSuchText() {
		return suchText;
	}

	public void setSuchText(String suchText) {
		this.suchText = suchText;
	}

	public String getAusgewaehlteKategorie() {
		return ausgewaehlteKategorie;
	}

	public void setAusgewaehlteKategorie(String ausgewaehlteKategorie) {
		this.ausgewaehlteKategorie = ausgewaehlteKategorie;
	}

	public List<String> getVerfuegbareKategorien() {
		return verfuegbareKategorien;
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

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public void zurueck() {

	}
}
