package de.bericht.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.bericht.service.DatabaseService;
import de.bericht.service.SpielcodeEintrag;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("spielcodesBean")
@ViewScoped
public class SpielcodesBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private final ConfigManager configManager = ConfigManager.getInstance();

	private String vereinnr;
	private final List<SpielcodeEintrag> spiele = new ArrayList<>();
	private final DatabaseService databaseService = new DatabaseService();

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = "13014";
		}
		if ("tsgv000".equals(request.getParameter("p"))) {
			ladeSpiele();
		} else {
			return;
		}

	}

	private void ladeSpiele() {
		spiele.clear();

		LocalDate heute = LocalDate.now();
		List<Map<String, String>> rows = databaseService.ladeSpielcodesRohdaten(vereinnr);
		for (Map<String, String> row : rows) {
			String datum = row.get("datum");
			LocalDate spielDatum = parseDatum(datum);
			if (spielDatum == null || spielDatum.isBefore(heute)) {
				continue;
			}

			SpielcodeEintrag eintrag = new SpielcodeEintrag();
			eintrag.setLiga(row.get("liga"));
			eintrag.setDatum(datum);
			eintrag.setZeit(row.get("zeit"));
			eintrag.setHeim(row.get("heim"));
			eintrag.setGast(row.get("gast"));

			String spielCode = row.get("spiel_code");
			String pin = row.get("pin");
			boolean codeGefunden = (spielCode != null && !spielCode.isBlank()) || (pin != null && !pin.isBlank());
			eintrag.setSpielcodeGefunden(codeGefunden);
			eintrag.setSpielCode(spielCode);
			eintrag.setPin(pin);
			spiele.add(eintrag);
		}

		spiele.sort(Comparator.comparing((SpielcodeEintrag eintrag) -> parseDatum(eintrag.getDatum()))
				.thenComparing(SpielcodeEintrag::getZeit, Comparator.nullsLast(String::compareTo)));

		if (spiele.size() > 10) {
			spiele.subList(10, spiele.size()).clear();
		}
	}

	private LocalDate parseDatum(String datum) {
		if (datum == null || datum.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(datum, DATUM_FORMAT);
		} catch (DateTimeParseException ex) {
			return null;
		}
	}

	public List<SpielcodeEintrag> getSpiele() {
		return spiele;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}
}
