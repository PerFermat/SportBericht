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
import de.bericht.util.OpenAIModelFetcher;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class ConfigBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String vereinnr;
	private String emailPasswort;
	private String encryptedPasswort;
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
	private final Map<String, List<String>> enumWerteCache = new HashMap<>();
	private List<String> chatGptModelle = new ArrayList<>();
	private ConfigEintrag passwortDialogEintrag;
	private String passwortDialogAnzeige;
	private String passwortDialogInput;

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

	public boolean isFarbFeld(ConfigEintrag eintrag) {
		return eintrag != null && (Boolean.TRUE.equals(eintrag.getFarbe()) || eintrag.isInhaltformatFarbe());
	}


	
	public boolean farbFeld(ConfigEintrag eintrag) {
		return eintrag != null && (Boolean.TRUE.equals(eintrag.getFarbe()) || eintrag.isInhaltformatFarbe());
	}

	public boolean textFeld(ConfigEintrag eintrag) {
		return eintrag != null && (eintrag.isInhaltformatText()
				|| (!isFarbFeld(eintrag) && !eintrag.isInhaltformatZahl() && !eintrag.isInhaltformatEnum()
						&& !eintrag.isInhaltformatChatGpt() && !eintrag.isInhaltformatPasswort()));
	}

	public int minWert(ConfigEintrag eintrag) {
		return leseGrenzen(eintrag)[0];
	}

	public int maxWert(ConfigEintrag eintrag) {
		return leseGrenzen(eintrag)[1];
	}
	
	public int schrittweite(ConfigEintrag eintrag) {
		return leseGrenzen(eintrag)[2];
	}


	public List<String> enumWerte(ConfigEintrag eintrag) {
		if (eintrag == null || eintrag.getEintrag() == null || eintrag.getEintrag().isBlank()) {
			return Collections.emptyList();
		}
		return enumWerteCache.computeIfAbsent(eintrag.getEintrag(), key -> enumWerteAusWertebereichUndDaten(key, eintrag));
	}

	public List<String> getChatGptModelle() {
		if (!chatGptModelle.isEmpty()) {
			return chatGptModelle;
		}
		String apiKey = ConfigManager.getChatGptPasswort(vereinnr);
		if (!isNotBlank(apiKey)) {
			return Collections.emptyList();
		}
		try {
			OpenAIModelFetcher fetcher = new OpenAIModelFetcher(apiKey);
			chatGptModelle = fetcher.getModelNames().stream().sorted().collect(Collectors.toList());
		} catch (Exception e) {
			return Collections.emptyList();
		}
		return chatGptModelle;
	}

	public void openPasswortDialog(ConfigEintrag eintrag) {
		passwortDialogEintrag = eintrag;
		passwortDialogInput = "";
		passwortDialogAnzeige = "";
		if (eintrag == null || !isNotBlank(eintrag.getWert())) {
			return;
		}
		try {
			String entschluesselt = ConfigManager.decryptPasswort(vereinnr, eintrag.getWert());
			passwortDialogInput = defaultString(entschluesselt);
		} catch (Exception e) {
			passwortDialogInput = defaultString(eintrag.getWert());
		}
	}

	public void speicherePasswortDialog() {
		if (passwortDialogEintrag == null || !isNotBlank(passwortDialogInput)) {
			return;
		}
		try {
			String verschluesselt = ConfigManager.encryptPasswort(vereinnr, passwortDialogInput);
			passwortDialogEintrag.setWert(verschluesselt);
			passwortDialogAnzeige = passwortDialogInput;
		} catch (Exception e) {
			// absichtlich leer, Anzeige bleibt unverändert
		}
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
		dialogBedeutung = "";
		dialogWertebereich = "";
		dialogInhaltformat = verfuegbareInhaltformate.isEmpty() ? "" : verfuegbareInhaltformate.get(0);
		dialogKategorien = new ArrayList<>();
		dialogKategorien.add("");
	}

	public void openBearbeitenDialog(ConfigEintrag eintrag) {
		neuerDialog = false;
		dialogKey = eintrag.getEintrag();
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
	}

	public void speichereDialog() {
		if (dialogKey == null || dialogKey.isBlank()) {
			return;
		}

		String bereinigterKey = dialogKey.trim();
		if (neuerDialog) {
			List<String> vereine = service.ladeAlleVereine();
			for (String verein : vereine) {
				service.insertConfigEintrag(verein, bereinigterKey, "");
			}

		}

		service.upsertConfigBedeutung(bereinigterKey, defaultString(dialogBedeutung),
				defaultString(dialogInhaltformat), defaultString(dialogWertebereich));
		service.replaceConfigKategorien(bereinigterKey, dialogKategorien);
		ladeConfigEintraegeMitBedeutung();
	}

	private List<String> enumWerteAusWertebereichUndDaten(String key, ConfigEintrag eintrag) {
		LinkedHashSet<String> zusammengefuehrteWerte = new LinkedHashSet<>();
		if (eintrag != null && isNotBlank(eintrag.getWertebereich())) {
			String[] wertebereichWerte = eintrag.getWertebereich().split(",");
			for (String wert : wertebereichWerte) {
				String bereinigt = defaultString(wert).trim();
				if (isNotBlank(bereinigt)) {
					zusammengefuehrteWerte.add(bereinigt);
				}
			}
		}
		List<String> vorhandeneWerte = service.ladeDistinctConfigWerteByEintrag(key);
		if (vorhandeneWerte != null) {
			for (String wert : vorhandeneWerte) {
				String bereinigt = defaultString(wert).trim();
				if (isNotBlank(bereinigt)) {
					zusammengefuehrteWerte.add(bereinigt);
				}
			}
		}
		return new ArrayList<>(zusammengefuehrteWerte);
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
		enumWerteCache.clear();
		chatGptModelle = new ArrayList<>();

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
		return configEintraege.stream().filter(this::passtZurKategorie).filter(this::passtZurSuche)
				.collect(Collectors.toList());
	}

	private boolean passtZurKategorie(ConfigEintrag eintrag) {
		if (ausgewaehlteKategorie == null || ausgewaehlteKategorie.isBlank()) {
			return true;
		}
		return eintrag.getKategorien() != null
				&& eintrag.getKategorien().toLowerCase().contains(ausgewaehlteKategorie.toLowerCase());

	}

	private int[] leseGrenzen(ConfigEintrag eintrag) {
		int min = 0;
		int max = 100;
		int schritt = 1;
		if (eintrag == null || !isNotBlank(eintrag.getWertebereich())) {
			return new int[] { min, max, schritt };
		}

		String[] teile = eintrag.getWertebereich().split(";");
		try {
			if (teile.length >= 1 && isNotBlank(teile[0])) {
				min = Integer.parseInt(teile[0].trim());
			}
			if (teile.length >= 2 && isNotBlank(teile[1])) {
				max = Integer.parseInt(teile[1].trim());
			}
			if (teile.length >= 3 && isNotBlank(teile[2])) {
				schritt = Integer.parseInt(teile[2].trim());
			}
		} catch (NumberFormatException e) {
			return new int[] { 0, 100, 1 };
		}

		if (max < min) {
			int tmp = min;
			min = max;
			max = tmp;
		}
		if (schritt <= 0) {
			schritt = 1;
		}
		return new int[] { min, max, schritt };
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
			encryptedPasswort = ConfigManager.encryptPasswort(vereinnr, emailPasswort);
		} catch (Exception e) {
			encryptedPasswort = "Fehler: " + e.getMessage();
		}
	}

	// Getter & Setter
	public String getEmailPasswort() {
		return emailPasswort;
	}

	public void setEmailPasswort(String emailPasswort) {
		this.emailPasswort = emailPasswort;
	}

	public String getEncryptedPasswort() {
		return encryptedPasswort;
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

	public String getPasswortDialogAnzeige() {
		return passwortDialogAnzeige;
	}

	public String getPasswortDialogInput() {
		return passwortDialogInput;
	}

	public void setPasswortDialogInput(String passwortDialogInput) {
		this.passwortDialogInput = passwortDialogInput;
	}

	public void zurueck() {

	}
}
