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

	private List<String> verfuegbareInhaltformate = new ArrayList<>();

	private boolean neuerDialog;
	private String dialogKey;
	private String dialogInhalt;
	private String dialogBedeutung;
	private String dialogInhaltformat;
	private String dialogWertebereich;
	private List<String> dialogKategorien = new ArrayList<>();

	private final ConfigService service = new ConfigService();

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

	public void openNeuerKeyDialog() {
		neuerDialog = true;
		dialogKey = "";
		dialogInhalt = "";
		dialogBedeutung = "";
		dialogWertebereich = "";
		dialogInhaltformat = verfuegbareInhaltformate.isEmpty() ? "" : verfuegbareInhaltformate.get(0);
		dialogKategorien = new ArrayList<>();
		dialogKategorien.add("");
	}

	public void openBearbeitenDialog(ConfigEintrag eintrag) {
		neuerDialog = false;
		dialogKey = eintrag.getEintrag();
		dialogInhalt = eintrag.getWert();
		dialogBedeutung = defaultString(eintrag.getBedeutung());
		dialogInhaltformat = defaultString(eintrag.getInhaltformat());
		dialogWertebereich = defaultString(eintrag.getWertebereich());
		dialogKategorien = new ArrayList<>();
		List<String> vorhandeneKategorien = findeKategorien(eintrag.getEintrag());
		if (vorhandeneKategorien != null && !vorhandeneKategorien.isEmpty()) {
			dialogKategorien.addAll(vorhandeneKategorien);
		}
		if (dialogKategorien.isEmpty()) {
			dialogKategorien.add("");
		}
	}

	public void addKategorieZeile() {
		if (dialogKategorien == null) {
			dialogKategorien = new ArrayList<>();
		}
		dialogKategorien.add("");
	}

	public void removeKategorieZeile(int index) {
		if (dialogKategorien == null || index < 0 || index >= dialogKategorien.size()) {
			return;
		}
		dialogKategorien.remove(index);
		if (dialogKategorien.isEmpty()) {
			dialogKategorien.add("");
		}
	}

	public void speichereDialog() {
		if (dialogKey == null || dialogKey.isBlank()) {
			return;
		}

		List<String> vereine = service.ladeAlleVereine();
		for (String verein : vereine) {
			service.insertOrUpdateConfigEintrag(verein, dialogKey.trim(), defaultString(dialogInhalt));
		}

		service.upsertConfigBedeutung(dialogKey.trim(), defaultString(dialogBedeutung), defaultString(dialogInhaltformat),
				defaultString(dialogWertebereich));
		service.replaceConfigKategorien(dialogKey.trim(), dialogKategorien);
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
		verfuegbareInhaltformate = configBedeutungen.values().stream().map(ConfigBedeutung::getInhaltformat)
				.filter(this::isNotBlank).distinct().sorted().collect(Collectors.toList());


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
		return eintrag.getKategorien() != null
				&& eintrag.getKategorien().toLowerCase().contains(ausgewaehlteKategorie.toLowerCase());

	}

	private boolean passtZurSuche(ConfigEintrag eintrag) {
		if (suchText == null || suchText.isBlank()) {
			return true;
		}
		String needle = suchText.toLowerCase();
		return containsIgnoreCase(eintrag.getEintrag(), needle) || containsIgnoreCase(eintrag.getWert(), needle)
				|| containsIgnoreCase(eintrag.getBedeutung(), needle);
	}
	private String defaultString(String wert) {
		return wert == null ? "" : wert;
	}

	private boolean isNotBlank(String wert) {
		return wert != null && !wert.isBlank();
	}

	public List<String> getVerfuegbareInhaltformate() {
		return verfuegbareInhaltformate;
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

	public boolean isNeuerDialog() {
		return neuerDialog;
	}

	public String getDialogKey() {
		return dialogKey;
	}

	public void setDialogKey(String dialogKey) {
		this.dialogKey = dialogKey;
	}

	public String getDialogInhalt() {
		return dialogInhalt;
	}

	public void setDialogInhalt(String dialogInhalt) {
		this.dialogInhalt = dialogInhalt;
	}

	public String getDialogBedeutung() {
		return dialogBedeutung;
	}

	public void setDialogBedeutung(String dialogBedeutung) {
		this.dialogBedeutung = dialogBedeutung;
	}

	public String getDialogInhaltformat() {
		return dialogInhaltformat;
	}

	public void setDialogInhaltformat(String dialogInhaltformat) {
		this.dialogInhaltformat = dialogInhaltformat;
	}

	public String getDialogWertebereich() {
		return dialogWertebereich;
	}

	public void setDialogWertebereich(String dialogWertebereich) {
		this.dialogWertebereich = dialogWertebereich;
	}

	public List<String> getDialogKategorien() {
		return dialogKategorien;
	}

	public void setDialogKategorien(List<String> dialogKategorien) {
		this.dialogKategorien = dialogKategorien;
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
