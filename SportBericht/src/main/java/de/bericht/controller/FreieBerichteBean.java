package de.bericht.controller;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.bericht.service.BerichtText;
import de.bericht.service.DatabaseService;
import de.bericht.service.Spiel;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("freieBerichteBean")
@ViewScoped
public class FreieBerichteBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<Spiel> spiele;

	ConfigManager config;
	private DatabaseService dbService = new DatabaseService();

	private String berichtDatum;
	private String vereinnr;
	private Date berichtDatumCal;
	List<BerichtText> meineBerichtTexte = new ArrayList<>();

	private String freierText;

	@PostConstruct
	public void init() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		this.vereinnr = params.get("vereinnr");
		spiele = dbService.listeFreieBerichte(vereinnr);
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");

		spiele.sort((s1, s2) -> {
			try {
				Date d1 = formatter.parse(s1.getDatumAnzeige());
				Date d2 = formatter.parse(s2.getDatumAnzeige());
				return d1.compareTo(d2);
			} catch (ParseException e) {
				return 0;
			}
		});
	}

	public void loeschen(String ergebnisLink) {
		dbService.deleteBericht(vereinnr, ergebnisLink);
		spiele.removeIf(spiel -> spiel.getErgebnisLink().equals(ergebnisLink));
	}

	public List<Spiel> getSpiele() {
		long configTage = Long.parseLong(ConfigManager.getConfigValue(vereinnr, "freierBericht.rueckschau.tage"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate heute = LocalDate.now();
		LocalDate vorTagen = heute.minusDays(configTage);

		return spiele.stream().filter(spiel -> {
			LocalDate spielDatum = LocalDate.parse(spiel.getDatumAnzeige(), formatter);
			return (spielDatum.isAfter(vorTagen) || spielDatum.isEqual(vorTagen));
		}).collect(Collectors.toList());
	}

	public void updConfig() {
		config = ConfigManager.updInstance();
	}

	public String getupdConfig() {
		return "";
	}

	public void setUpdConfig(String ergebnis) {
	}

	public String getHomepage() {
		return BerichtHelper.getHomepage(vereinnr);
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

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public boolean hasBild(String ergebnisLink) {
		BerichtHelper bh = new BerichtHelper();
		return bh.hasBild(vereinnr, ergebnisLink);
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

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}
}