package de.bericht.controller;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import de.bericht.service.BilderEintrag;
import de.bericht.service.DatabaseService;
import de.bericht.util.BerichtData;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("bilderBean")
@ViewScoped
public class BilderBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Pattern DATUM_PATTERN = Pattern.compile("\\b(\\d{1,2}\\.\\d{1,2}\\.\\d{2,4})\\b");
	private static final List<DateTimeFormatter> DATUM_FORMATE = List.of(
			DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMAN),
			DateTimeFormatter.ofPattern("d.M.yy", Locale.GERMAN));

	private final DatabaseService dbService = new DatabaseService();
	private String vereinnr;
	private List<BilderEintrag> bilder = new ArrayList<>();
	private List<BilderGruppe> bilderGruppen = new ArrayList<>();
	private StreamedContent downloadBild;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));

		if (vereinnr == null) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null) {
			vereinnr = "13014";
		}

		bilder = dbService.listeBilder(vereinnr);
		for (BilderEintrag bild : bilder) {
			bild.setBildUrl(BerichtHelper.getBildUrl(vereinnr, bild.getErgebnisLink()));
		}
		sortiereBilder();
		gruppiereBilder();
	}

	private void sortiereBilder() {
		Comparator<BilderEintrag> comparator = (a, b) -> {
			LocalDate datumA = extrahiereDatum(a.getUeberschrift(), a.getErgebnisLink());
			LocalDate datumB = extrahiereDatum(b.getUeberschrift(), b.getErgebnisLink());
			boolean hatDatumA = datumA != null;
			boolean hatDatumB = datumB != null;

			if (hatDatumA && !hatDatumB) {
				return -1;
			}
			if (!hatDatumA && hatDatumB) {
				return 1;
			}
			if (hatDatumA) {
				int datumVergleich = datumB.compareTo(datumA);
				if (datumVergleich != 0) {
					return datumVergleich;
				}
			}
			String titelA = a.getUeberschrift() == null ? "" : a.getUeberschrift();
			String titelB = b.getUeberschrift() == null ? "" : b.getUeberschrift();
			return titelA.compareToIgnoreCase(titelB);
		};
		bilder.sort(comparator);
	}

	private void gruppiereBilder() {
		Map<Integer, List<BilderEintrag>> jahresgruppen = new LinkedHashMap<>();
		List<BilderEintrag> rest = new ArrayList<>();

		for (BilderEintrag bild : bilder) {
			LocalDate datum = extrahiereDatum(bild.getUeberschrift(), bild.getErgebnisLink());
			if (datum != null) {
				jahresgruppen.computeIfAbsent(datum.getYear(), k -> new ArrayList<>()).add(bild);
			} else {
				rest.add(bild);
			}
		}

		List<Integer> jahre = new ArrayList<>(jahresgruppen.keySet());
		jahre.sort(Comparator.reverseOrder());
		bilderGruppen = new ArrayList<>();
		for (Integer jahr : jahre) {
			bilderGruppen.add(new BilderGruppe(String.valueOf(jahr), jahresgruppen.get(jahr)));
		}
		if (!rest.isEmpty()) {
			bilderGruppen.add(new BilderGruppe("REST", rest));
		}
	}

	private LocalDate extrahiereDatum(String ueberschrift, String ergebnisLink) {

		LocalDate datum = parseDatum(ueberschrift);
		if (datum != null) {
			return datum;
		}

		return parseDatum(ergebnisLink);
	}

	private LocalDate parseDatum(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		Matcher matcher = DATUM_PATTERN.matcher(text);
		if (!matcher.find()) {
			return null;
		}

		String kandidat = matcher.group(1);

		for (DateTimeFormatter formatter : DATUM_FORMATE) {
			try {
				return LocalDate.parse(kandidat, formatter);
			} catch (DateTimeParseException e) {
				// nächstes Datumsformat versuchen
			}
		}

		return null;
	}

	public void prepareDownloadBild(BilderEintrag eintrag) {
		BerichtData data = dbService.loadBerichtData(vereinnr, eintrag.getErgebnisLink());
		if (data != null && data.getBild() != null && data.getBild().length > 0) {
			ByteArrayInputStream stream = new ByteArrayInputStream(data.getBild());
			downloadBild = DefaultStreamedContent.builder().stream(() -> stream).contentType("image/jpeg")
					.name(getDateiname(eintrag) + ".jpg").build();
		} else {
			downloadBild = null;
		}
	}

	private String getDateiname(BilderEintrag eintrag) {
		String basis = eintrag.getUeberschrift();
		if (basis == null || basis.isBlank()) {
			basis = eintrag.getErgebnisLink();
		}
		if (basis == null || basis.isBlank()) {
			basis = "bild";
		}
		return basis.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	public List<BilderEintrag> getBilder() {
		return bilder;
	}

	public List<BilderGruppe> getBilderGruppen() {
		return bilderGruppen;
	}

	public StreamedContent getDownloadBild() {
		return downloadBild;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public static class BilderGruppe implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String titel;
		private final List<BilderEintrag> bilder;

		public BilderGruppe(String titel, List<BilderEintrag> bilder) {
			this.titel = titel;
			this.bilder = bilder;
		}

		public String getTitel() {
			return titel;
		}

		public List<BilderEintrag> getBilder() {
			return bilder;
		}
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
