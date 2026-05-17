package de.bericht.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.primefaces.model.file.UploadedFile;

import de.bericht.service.DatabaseService;
import de.bericht.service.SpielCodeParser;
import de.bericht.service.SpielcodeEintrag;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.LoginCookieDaten;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("spielCodeParserBean")
@ViewScoped
public class SpielCodeParserBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private final DatabaseService databaseService = new DatabaseService();
	private String vereinnr;
	private String passwort;
	private String mannschaft;
	private String liga;
	private UploadedFile pdfDatei;
	private byte[] pdfInhalt;
	private String pdfDateiname;
	private String pdfPreviewUrl;
	private boolean datumUhrzeitErsetzen = true;
	private boolean angemeldet;
	private List<String> mannschaftOptionen = new ArrayList<>();
	private List<String> ligaOptionen = new ArrayList<>();
	private List<SpielcodeEintrag> spielcodeEintraege = new ArrayList<>();

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		lesenCookieParameter();

		String userPasswort = ConfigManager.getUserPasswort(vereinnr);
		String adminPasswort = ConfigManager.getAdminPasswort(vereinnr);
		angemeldet = userPasswort.equals(passwort) || adminPasswort.equals(passwort);
		if (angemeldet) {
			ladeOptionen();
		}
	}

	private void lesenCookieParameter() {
		LoginCookieDaten logging = new LoginCookieDaten();
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = logging.getVereinnr();
			if (passwort == null) {
				passwort = logging.getPasswort();
			}
		} else if (passwort == null && vereinnr.equals(logging.getVereinnr())) {
			passwort = logging.getPasswort();
		}
	}

	private void ladeOptionen() {
		Set<String> mannschaften = new LinkedHashSet<>();
		Set<String> ligen = new LinkedHashSet<>();
		List<SpielcodeEintrag> eintraege = new ArrayList<>();
		List<Map<String, String>> rows = databaseService.ladeSpielcodesRohdaten(vereinnr);
		for (Map<String, String> row : rows) {
			String rowMannschaft = row.get("mannschaft");
			String rowLiga = row.get("liga");
			if (rowMannschaft != null && !rowMannschaft.isBlank()) {
				mannschaften.add(rowMannschaft);
			}
			if (rowLiga != null && !rowLiga.isBlank()) {
				ligen.add(rowLiga);
			}

			String spielCode = row.get("spiel_code");
			String pin = row.get("pin");
			if ((spielCode == null || spielCode.isBlank()) && (pin == null || pin.isBlank())) {
				continue;
			}
			SpielcodeEintrag eintrag = new SpielcodeEintrag();
			eintrag.setMannschaft(rowMannschaft);
			eintrag.setLiga(rowLiga);
			eintrag.setDatum(row.get("datum"));
			eintrag.setZeit(row.get("zeit"));
			eintrag.setHeim(row.get("heim"));
			eintrag.setGast(row.get("gast"));
			eintrag.setSpielCode(spielCode);
			eintrag.setPin(pin);
			eintraege.add(eintrag);

		}
		spielcodeEintraege = eintraege;
		mannschaftOptionen = new ArrayList<>(mannschaften);
		ligaOptionen = new ArrayList<>(ligen);
	}

	public void ladeVorschau() {
		if (pdfInhalt != null && pdfDatei == null) {
			return;
		} else if (pdfDatei == null) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Hinweis", "Bitte PDF-Datei auswählen."));
			return;
		}
		try (InputStream input = pdfDatei.getInputStream()) {
			pdfDateiname = pdfDatei.getFileName();
			pdfInhalt = input.readAllBytes();
			pdfPreviewUrl = "data:application/pdf;base64," + java.util.Base64.getEncoder().encodeToString(pdfInhalt);
		} catch (IOException e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "PDF konnte nicht gelesen werden."));
		}
	}

	public void importieren() {
		ladeVorschau();
		if (!validierePflichtfelder(true)) {
			return;
		}
		if (pdfInhalt == null || pdfInhalt.length == 0) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Hinweis", "Bitte zuerst Vorschau laden."));
			return;
		}
		try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfInhalt))) {
			new SpielCodeParser(vereinnr, mannschaft.trim(), liga.trim(), document, datumUhrzeitErsetzen);
			ladeOptionen();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolgreich", "Spielcodes wurden importiert."));
		} catch (IOException e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "PDF konnte nicht verarbeitet werden."));
		}
	}

	private boolean validierePflichtfelder(boolean mitTyp) {
		if (!angemeldet) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Nicht autorisiert."));
			return false;
		}
		if (pdfDatei == null && (pdfInhalt == null || pdfInhalt.length == 0)) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Hinweis", "Bitte PDF-Datei auswählen."));
			return false;
		}
		if (mannschaft == null || mannschaft.isBlank() || liga == null || liga.isBlank()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Hinweis", "Mannschaft und Liga sind Pflichtfelder."));
			return false;
		}
		return true;
	}

	public List<String> completeMannschaft(String query) {
		return filterOptionen(mannschaftOptionen, query);
	}

	public List<String> completeLiga(String query) {
		return filterOptionen(ligaOptionen, query);
	}

	private List<String> filterOptionen(List<String> optionen, String query) {
		if (query == null || query.isBlank()) {
			return optionen.stream().sorted().toList();
		}

		String lower = query.toLowerCase();

		return optionen.stream().filter(eintrag -> eintrag.toLowerCase().contains(lower)).sorted().toList();
	}

	public List<SpielcodeEintrag> getGefilterteSpielcodeEintraege() {
		return spielcodeEintraege.stream().filter(this::passtMannschaft).filter(this::passtLiga)
				.sorted(Comparator.comparing(this::parseDatum, Comparator.nullsLast(LocalDate::compareTo))
						.thenComparing(SpielcodeEintrag::getZeit, Comparator.nullsLast(String::compareTo)))
				.collect(Collectors.toList());
	}

	private LocalDate parseDatum(SpielcodeEintrag eintrag) {
		if (eintrag == null || eintrag.getDatum() == null || eintrag.getDatum().isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(eintrag.getDatum(), DATUM_FORMAT);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private boolean passtMannschaft(SpielcodeEintrag eintrag) {
		if (mannschaft == null || mannschaft.isBlank()) {
			return true;
		}
		return mannschaft.trim().equalsIgnoreCase(String.valueOf(eintrag.getMannschaft()).trim());
	}

	private boolean passtLiga(SpielcodeEintrag eintrag) {
		if (liga == null || liga.isBlank()) {
			return true;
		}
		return liga.trim().equalsIgnoreCase(String.valueOf(eintrag.getLiga()).trim());
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public UploadedFile getPdfDatei() {
		return pdfDatei;
	}

	public void setPdfDatei(UploadedFile pdfDatei) {
		this.pdfDatei = pdfDatei;
	}

	public String getPdfDateiname() {
		return pdfDateiname;
	}

	public String getPdfPreviewUrl() {
		return pdfPreviewUrl;
	}

	public boolean isDatumUhrzeitErsetzen() {
		return datumUhrzeitErsetzen;
	}

	public void setDatumUhrzeitErsetzen(boolean datumUhrzeitErsetzen) {
		this.datumUhrzeitErsetzen = datumUhrzeitErsetzen;
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
}
