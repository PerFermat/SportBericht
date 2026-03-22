package de.bericht.controller;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import de.bericht.provider.SpielplanFactory;
import de.bericht.provider.SpielplanProvider;
import de.bericht.service.BerichtText;
import de.bericht.service.DatabaseService;
import de.bericht.service.Spiel;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.ErgebnisCache;
import de.bericht.util.WebCache;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("spielplanBean")
@ViewScoped
public class SpielplanBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<Spiel> spiele;

	private String berichte;
	private String berichtDatum;
	private Date berichtDatumCal;
	private String vereinnr;
	private String passwort;
	private String liga;
	private String gruppeUrl;
	ConfigManager config;
	List<BerichtText> meineBerichtTexte = new ArrayList<>();
	DatabaseService db = new DatabaseService();

	private String freierText;

	public SpielplanBean() {
	}

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		liga = request.getParameter("liga");
		gruppeUrl = request.getParameter("gruppeUrl");
		passwort = request.getParameter("p");

		if (vereinnr == null) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null) {
			vereinnr = "13014";
		}
		int i = 0;

		SpielplanProvider provider;
		try {
			if (gruppeUrl == null) {
				provider = SpielplanFactory.create(vereinnr);
				gruppeUrl = provider.getFallbackSourceUrl();
			} else {
				provider = SpielplanFactory.create(vereinnr, gruppeUrl);
			}
			spiele = provider.getSpielplan();
			if (ConfigManager.isTennis(vereinnr)) {
				for (Spiel spiel : spiele) {
					db.saveSpielplanEntries(vereinnr, spiele, liga);
				}
			}
			if (provider.isFallbackSourceUsed()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Achtung: Die Daten konnten aus der "
								+ gruppeUrl + " nicht gelesen werden. Sie können daher veraltet sein"));
			}

			String was = ConfigManager.getConfigValue(vereinnr, "spielplan.vorschau.was").toUpperCase();

			if ("HEIM".equals(was) || "GAST".equals(was) || "ALLE".equals(was)) {
				provider.generiereVorschauBericht(vereinnr, was);
			} else {
				DatabaseService dbService = new DatabaseService(vereinnr);
				dbService.deleteBericht(vereinnr, "31.12.2999 - Vorschaubericht");
			}
		} catch (Exception e) {
			System.out.println("Vorschaubericht fehler " + spiele.size());

		}
	}

	public List<Spiel> getSpiele() {
		long configTage = Long.parseLong(ConfigManager.getConfigValue(vereinnr, "spielplan.rueckschau.tage"));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate heute = LocalDate.now();
		LocalDate vorTagen = heute.minusDays(configTage);

		return spiele.stream().filter(spiel -> {
			try {
				LocalDate spielDatum = LocalDate.parse(spiel.getDatum(), formatter);
				return !spielDatum.isBefore(vorTagen); // >= vorTagen
			} catch (DateTimeParseException e) {
				// Ungültiges Datum → trotzdem behalten
				return true;
			}
		}).collect(Collectors.toList());
	}

	public void setBerichtDatum(String berichtDatum) {
		this.berichtDatum = berichtDatum;
	}

	public String getFreierText() {
		return freierText;
	}

	public String getFreierLink() {
		return berichtDatum + "-" + freierText;
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

	public void updConfig() {
		ErgebnisCache.cacheLeeren();
		BerichtHelper.clearCacheForVerein(vereinnr);
		ConfigManager.clearCache(vereinnr);
		ConfigManager.updInstance();
		WebCache.clearCache();
	}

	public String getupdConfig() {
		return "";
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public void setUpdConfig(String ergebnis) {
	}

	public String getBerichte() {
		return berichte;
	}

	public void setBerichte(String berichte) {
		this.berichte = berichte;
	}

	// Download-Methode: jetzt über die Bean

	public List<BerichtText> getMeineBerichtTexte() {
		return meineBerichtTexte;
	}

	public void setMeineBerichtTexte(List<BerichtText> meineBerichtTexte) {
		this.meineBerichtTexte = meineBerichtTexte;
	}

	public String getHomepage() {
		return BerichtHelper.getHomepage(vereinnr);
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getLiga() {
		return liga;
	}

	public boolean hasBild(String ergebnisLink) {
		return BerichtHelper.hasBild(vereinnr, ergebnisLink);
	}

	public boolean hasFreigabe(String ergebnisLink) {
		return BerichtHelper.hasFreigabe(vereinnr, ergebnisLink);
	}

	public boolean hasHomepage(String ergebnisLink) {
		return BerichtHelper.hasHomepage(vereinnr, ergebnisLink);
	}

	public boolean hasBlaettle(String ergebnisLink) {
		return BerichtHelper.hasBlaettle(vereinnr, ergebnisLink);
	}

	public boolean hasBericht(String ergebnisLink) {

		return BerichtHelper.hasBericht(vereinnr, ergebnisLink);
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public void nullPointer() {
		String s = null;
		s.length(); // 💥 NullPointerException
	}

	public void divideByZero() {

		int x = 10 / 0; // 💥 ArithmeticException

	}

	public boolean isPasswortOK() {
		if ("tsgv000".equals(passwort)) {
			return true;
		}

		return false;

	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public void zurueck() {
	}

	public String getGruppeUrl() {
		return gruppeUrl;
	}

	public void setGruppeUrl(String gruppeUrl) {
		this.gruppeUrl = gruppeUrl;
	}
}