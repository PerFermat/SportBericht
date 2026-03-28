package de.bericht.controller;

import java.io.Serializable;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.bericht.service.DatabaseService;
import de.bericht.service.VersendeteMail;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
@ViewScoped
public class VersendeteMailsBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter MONAT_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);
	private static final DateTimeFormatter ZEIT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN);

	private final DatabaseService dbService = new DatabaseService();

	private String vereinnr;
	private String ruecksprung;
	private String suchbegriff;
	private List<VersendeteMail> alleMails = new ArrayList<>();
	private List<MonatsGruppe> gruppen = new ArrayList<>();
	private VersendeteMail ausgewaehlteMail;
	private boolean mobileDetailAnsicht;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		ruecksprung = request.getParameter("ruecksprung");
		ladeMails();
	}

	public void ladeMails() {
		alleMails = dbService.ladeVersendeteMails();
		gruppiereMails(filtereMails(alleMails, suchbegriff));
	}

	public void onSucheAendern() {
		gruppiereMails(filtereMails(alleMails, suchbegriff));
	}

	private List<VersendeteMail> filtereMails(List<VersendeteMail> mails, String filter) {
		if (filter == null || filter.isBlank()) {
			return mails;
		}
		String suchwert = filter.toLowerCase(Locale.ROOT).trim();
		List<VersendeteMail> gefiltert = new ArrayList<>();
		for (VersendeteMail mail : mails) {
			if (enthaelt(mail.getBetreff(), suchwert) || enthaelt(mail.getText(), suchwert)
					|| enthaelt(mail.getEmpfaenger(), suchwert) || enthaelt(mail.getEmpfaengerCc(), suchwert)
					|| enthaelt(mail.getEmpfaengerBcc(), suchwert)) {
				gefiltert.add(mail);
			}
		}
		return gefiltert;
	}

	private boolean enthaelt(String value, String suchwert) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(suchwert);
	}

	private void gruppiereMails(List<VersendeteMail> mails) {
		Map<YearMonth, List<VersendeteMail>> gruppiert = new LinkedHashMap<>();
		for (VersendeteMail mail : mails) {
			if (mail.getTimestamp() == null) {
				continue;
			}
			YearMonth key = YearMonth.from(mail.getTimestamp());
			gruppiert.computeIfAbsent(key, ignore -> new ArrayList<>()).add(mail);
		}

		gruppen = new ArrayList<>();
		YearMonth aktuell = YearMonth.now();
		YearMonth letzterMonat = aktuell.minusMonths(1);
		for (Map.Entry<YearMonth, List<VersendeteMail>> eintrag : gruppiert.entrySet()) {
			boolean aufgeklappt = eintrag.getKey().equals(aktuell) || eintrag.getKey().equals(letzterMonat);
			gruppen.add(new MonatsGruppe(eintrag.getKey().format(MONAT_FORMAT), eintrag.getValue(), aufgeklappt));
		}

		if (!mails.isEmpty()) {
			if (ausgewaehlteMail == null) {
				ausgewaehlteMail = mails.get(0);
			} else {
				long id = ausgewaehlteMail.getId();
				ausgewaehlteMail = mails.stream().filter(mail -> mail.getId() == id).findFirst().orElse(mails.get(0));
			}
		} else {
			ausgewaehlteMail = null;
			mobileDetailAnsicht = false;
		}
	}

	public void mailAuswaehlen(VersendeteMail mail) {
		ausgewaehlteMail = mail;
		mobileDetailAnsicht = true;
	}

	public void zurListe() {
		mobileDetailAnsicht = false;
	}

	public void loescheAusgewaehlteMail() {
		if (ausgewaehlteMail == null) {
			return;
		}
		dbService.loescheVersendeteMail(ausgewaehlteMail.getId());
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Mail gelöscht", "Die Mail wurde entfernt."));
		ladeMails();
	}

	public void loescheAusgewaehlteMail(VersendeteMail mail) {

		dbService.loescheVersendeteMail(mail.getId());
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Mail gelöscht", "Die Mail wurde entfernt."));
		ladeMails();
	}

	public String formatZeitstempel(VersendeteMail mail) {
		return mail.getTimestamp() == null ? "" : mail.getTimestamp().format(ZEIT_FORMAT);
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getSuchbegriff() {
		return suchbegriff;
	}

	public void setSuchbegriff(String suchbegriff) {
		this.suchbegriff = suchbegriff;
	}

	public List<MonatsGruppe> getGruppen() {
		return gruppen;
	}

	public VersendeteMail getAusgewaehlteMail() {
		return ausgewaehlteMail;
	}

	public boolean isMobileDetailAnsicht() {
		return mobileDetailAnsicht;
	}

	public static class MonatsGruppe implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String titel;
		private final List<VersendeteMail> mails;
		private final boolean aufgeklappt;

		public MonatsGruppe(String titel, List<VersendeteMail> mails, boolean aufgeklappt) {
			this.titel = titel;
			this.mails = mails;
			this.aufgeklappt = aufgeklappt;
		}

		public String getTitel() {
			return titel;
		}

		public List<VersendeteMail> getMails() {
			return mails;
		}

		public boolean isAufgeklappt() {
			return aufgeklappt;
		}
	}

	public String getRuecksprung() {
		return ruecksprung;
	}

	public String ruecksprung() {
		return ruecksprung;
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
