package de.bericht.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import de.bericht.service.AdresslisteService;
import de.bericht.service.EmailService;
import de.bericht.service.TelegrammService;
import de.bericht.util.AdressEintrag;
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
public class AdresslisteBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private final AdresslisteService service = new AdresslisteService();

	private String vereinnr;

	private List<AdressEintrag> eintraege;
	private List<AdressEintrag> gefilterteEintraege;
	private AdressEintrag bearbeiteterEintrag;
	private AdressEintrag neuerEintrag;
	private AdressEintrag loeschEintrag;

	private StreamedContent downloadPdf;
	private StreamedContent downloadExcel;
	private Map<String, Boolean> sichtbareSpalten;
	private boolean schreibzugriff;
	private String editEinmalpasswort;
	private String editEinmalpasswortEingabe;
	private boolean editAktionenFreigeschaltet;
	private String neuEinmalpasswort;
	private String neuEinmalpasswortEingabe;
	private boolean neuSpeichernFreigeschaltet;
	private boolean neuPasswortAngefordert;
	private final Random random = new java.security.SecureRandom();

	private static final List<SpaltenDefinition> SPALTEN_DEFINITIONEN = List.of(
			new SpaltenDefinition("name", "Nachname", AdressEintrag::getName),
			new SpaltenDefinition("vorname", "Vorname", AdressEintrag::getVorname),
			new SpaltenDefinition("geburtstag", "Geburtstag", eintrag -> formatDateStatic(eintrag.getGeburtstag())),
			new SpaltenDefinition("strasse", "Straße", AdressEintrag::getStrasse),
			new SpaltenDefinition("plz", "PLZ", AdressEintrag::getPlz),
			new SpaltenDefinition("wohnort", "Ort", AdressEintrag::getWohnort),
			new SpaltenDefinition("telefonPrivat", "Telefon privat", AdressEintrag::getTelefonPrivat),
			new SpaltenDefinition("telefonGesch", "Telefon gesch.", AdressEintrag::getTelefonGesch),
			new SpaltenDefinition("telefonMobil", "Handy privat", AdressEintrag::getTelefonMobil),
			new SpaltenDefinition("emailPrivat", "E-Mail privat", AdressEintrag::getEmailPrivat),
			new SpaltenDefinition("emailGesch", "E-Mail gesch.", AdressEintrag::getEmailGesch),
			new SpaltenDefinition("bemerkung", "Bemerkung", AdressEintrag::getBemerkung));

	@PostConstruct
	public void init() {
		initialisiereStandardSpalten();
		bearbeiteterEintrag = new AdressEintrag();
		neuerEintrag = new AdressEintrag();
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null || vereinnr.isBlank()) {
			addError("Keine Vereinnummer übergeben (Parameter v oder vereinnr fehlt).");
			eintraege = new ArrayList<>();
			return;
		}
		schreibzugriff = pruefeSchreibzugriff(request.getParameter("p"));
		if (schreibzugriff || "tsgv000".equals(request.getParameter("p"))) {
			ladeEintraege();
		} else {
			eintraege = new ArrayList<>();
			return;
		}

	}

	private void initialisiereStandardSpalten() {
		sichtbareSpalten = new LinkedHashMap<>();
		for (SpaltenDefinition definition : SPALTEN_DEFINITIONEN) {
			sichtbareSpalten.put(definition.getKey(), false);
		}
		sichtbareSpalten.put("vorname", true);
		sichtbareSpalten.put("name", true);
		sichtbareSpalten.put("strasse", true);
		sichtbareSpalten.put("wohnort", true);
		sichtbareSpalten.put("telefonMobil", true);
		sichtbareSpalten.put("emailPrivat", true);
	}

	public void ladeEintraege() {
		eintraege = service.findAll(vereinnr);
	}

	public void refreshAdressCache() {
		eintraege = service.refreshCache(vereinnr);
		addInfo("Adressliste wurde aus der Datenbank neu geladen.");
	}

	public void vorbereitenBearbeiten(AdressEintrag eintrag) {
		bearbeiteterEintrag = eintrag.copy();
		editEinmalpasswort = null;
		editEinmalpasswortEingabe = "";
		editAktionenFreigeschaltet = schreibzugriff;
		if (!schreibzugriff) {
			sendeEditEinmalpasswort();
		}

	}

	public void speichernBearbeiten() {
		if (!hatSchreibzugriffBearbeiten()) {
			return;
		}

		AdressEintrag alterEintrag = findeEintragNachId(bearbeiteterEintrag.getId());

		service.update(vereinnr, bearbeiteterEintrag);
		ladeEintraege();
		sendeAenderungsInfo("Änderung", alterEintrag, bearbeiteterEintrag);
		addInfo("Eintrag wurde erfolgreich aktualisiert.");
	}

	public void vorbereitenNeu() {

		neuerEintrag = new AdressEintrag();
		neuerEintrag.setGeburtstag(null);
		neuEinmalpasswort = null;
		neuEinmalpasswortEingabe = "";
		neuPasswortAngefordert = false;
		neuSpeichernFreigeschaltet = schreibzugriff;

	}

	public void speichernNeu() {
		if (!hatSchreibzugriffNeu()) {
			return;
		}

		service.create(vereinnr, neuerEintrag);
		ladeEintraege();
		sendeAenderungsInfo("Neuanlage", null, neuerEintrag);
		addInfo("Neue Person wurde hinzugefügt.");
	}

	public void vorbereitenLoeschen(AdressEintrag eintrag) {

		loeschEintrag = eintrag;
	}

	public void loeschen() {
		if (!hatSchreibzugriffBearbeiten()) {
			return;
		}

		if (loeschEintrag == null && bearbeiteterEintrag != null) {
			loeschEintrag = bearbeiteterEintrag;
		}

		if (loeschEintrag != null && loeschEintrag.getId() != null) {
			AdressEintrag alterEintrag = loeschEintrag.copy();
			service.delete(vereinnr, loeschEintrag.getId());
			ladeEintraege();
			sendeAenderungsInfo("Löschung", alterEintrag, null);
			addInfo("Eintrag wurde gelöscht.");
		}
	}

	public void anfordernNeuEinmalpasswort() {
		if (schreibzugriff) {
			neuSpeichernFreigeschaltet = true;
			return;
		}

		String empfaenger = stringOrEmpty(neuerEintrag.getEmailPrivat()).trim();
		if (empfaenger.isBlank()) {
			addError("Bitte zuerst eine private E-Mail-Adresse eingeben.");
			return;
		}

		neuEinmalpasswort = generiereEinmalpasswort();
		neuEinmalpasswortEingabe = "";
		neuPasswortAngefordert = true;
		neuSpeichernFreigeschaltet = false;
		sendeEinmalpasswortEmail(empfaenger, "Adressliste - Einmalpasswort für Neuanlage", neuEinmalpasswort);
	}

	public void pruefeNeuEinmalpasswort() {
		if (schreibzugriff) {
			neuSpeichernFreigeschaltet = true;
			return;
		}
		if (neuEinmalpasswort == null || neuEinmalpasswort.isBlank()) {
			addError("Bitte zuerst ein Einmalpasswort anfordern.");
			return;
		}
		if (neuEinmalpasswort.equals(stringOrEmpty(neuEinmalpasswortEingabe).trim())) {
			neuSpeichernFreigeschaltet = true;
			addInfo("Einmalpasswort korrekt. Speichern ist jetzt möglich.");
			return;
		}
		neuSpeichernFreigeschaltet = false;
		addError("Einmalpasswort ist nicht korrekt.");
	}

	public void pruefeEditEinmalpasswort() {
		if (schreibzugriff) {
			editAktionenFreigeschaltet = true;
			return;
		}
		if (editEinmalpasswort == null || editEinmalpasswort.isBlank()) {
			addError("Einmalpasswort wurde noch nicht versendet.");
			return;
		}
		if (editEinmalpasswort.equals(stringOrEmpty(editEinmalpasswortEingabe).trim())) {
			editAktionenFreigeschaltet = true;
			addInfo("Einmalpasswort korrekt. Speichern und Löschen sind jetzt freigegeben.");
			return;
		}
		editAktionenFreigeschaltet = false;
		addError("Einmalpasswort ist nicht korrekt.");
	}

	private void sendeEditEinmalpasswort() {
		String empfaenger = bearbeiteterEintrag == null ? ""
				: stringOrEmpty(bearbeiteterEintrag.getEmailPrivat()).trim();
		if (empfaenger.isBlank()) {
			addError("Für diesen Eintrag ist keine private E-Mail-Adresse hinterlegt.");
			return;
		}
		editEinmalpasswort = generiereEinmalpasswort();
		sendeEinmalpasswortEmail(empfaenger, "Adressliste - Einmalpasswort für Änderung/Löschung", editEinmalpasswort);
	}

	private void sendeEinmalpasswortEmail(String empfaenger, String betreff, String passwort) {
		EmailService emailService = new EmailService(vereinnr, "TO:" + empfaenger);
		String nachricht = "Ihr Einmalpasswort lautet: <b>" + passwort + "</b><br/>"
				+ "Bitte geben Sie dieses Passwort im Dialog ein, um fortzufahren.";
		try {
			emailService.sendEmail(vereinnr, betreff, nachricht, null, null);
			addInfo("Einmalpasswort wurde an " + empfaenger + " versendet.");
		} catch (Exception e) {
			addError("Einmalpasswort konnte nicht versendet werden: " + e.getMessage());
		}
	}

	private String generiereEinmalpasswort() {
		int code = 100000 + random.nextInt(900000);
		return String.valueOf(code);
	}

	public boolean isSchreibzugriff() {
		return schreibzugriff;
	}

	public String getEditEinmalpasswortEingabe() {
		return editEinmalpasswortEingabe;
	}

	public void setEditEinmalpasswortEingabe(String editEinmalpasswortEingabe) {
		this.editEinmalpasswortEingabe = editEinmalpasswortEingabe;
	}

	public boolean isEditAktionenFreigeschaltet() {
		return editAktionenFreigeschaltet;
	}

	public String getNeuEinmalpasswortEingabe() {
		return neuEinmalpasswortEingabe;
	}

	public void setNeuEinmalpasswortEingabe(String neuEinmalpasswortEingabe) {
		this.neuEinmalpasswortEingabe = neuEinmalpasswortEingabe;
	}

	public boolean isNeuSpeichernFreigeschaltet() {
		return neuSpeichernFreigeschaltet;
	}

	public boolean isNeuPasswortAngefordert() {
		return neuPasswortAngefordert;
	}

	public void prepareDownloadPdf() {
		downloadPdf = buildPdfDownload();
	}

	public void prepareDownloadExcel() {
		downloadExcel = buildExcelDownload();
	}

	private StreamedContent buildPdfDownload() {
		List<AdressEintrag> exportListe = getExportListe();
		List<SpaltenDefinition> exportSpalten = getSichtbareSpaltenDefinitionen();
		if (exportSpalten.isEmpty()) {
			addError("Bitte mindestens eine Spalte auswählen.");
			return null;
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Rectangle pageSize = PageSize.A4.rotate();
			Document document = new Document(pageSize, 24f, 24f, 24f, 24f);

			PdfWriter.getInstance(document, out);
			document.open();
			document.add(new Paragraph("Abteilungsliste Stand " + formatDate(LocalDate.now()),
					FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)));

			document.add(new Paragraph(" "));

			PdfPTable table = new PdfPTable(exportSpalten.size());
			table.setWidthPercentage(100f);
			for (SpaltenDefinition spalte : exportSpalten) {
				addHeader(table, spalte.getLabel());
			}

			for (AdressEintrag eintrag : exportListe) {
				for (SpaltenDefinition spalte : exportSpalten) {
					table.addCell(stringOrEmpty(spalte.getExtractor().apply(eintrag)));
				}

			}
			document.add(table);
			document.close();
			LocalDate heute = LocalDate.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			String datum = heute.format(formatter);

			return DefaultStreamedContent.builder().name("adressliste-" + datum + ".pdf").contentType("application/pdf")
					.stream(() -> new ByteArrayInputStream(out.toByteArray())).build();
		} catch (Exception e) {
			e.printStackTrace();
			addError("PDF konnte nicht erzeugt werden.");
			return null;
		}
	}

	private StreamedContent buildExcelDownload() {
		List<AdressEintrag> exportListe = getExportListe();
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet sheet = workbook.createSheet("Adressliste");
			Row header = sheet.createRow(0);
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);

			String[] columns = { "ID", "Name", "Vorname", "Geburtstag", "Straße", "PLZ", "Wohnort", "Telefon privat",
					"Telefon gesch.", "Telefon mobil", "E-Mail privat", "E-Mail gesch.", "Bemerkung", "Erstellt am",
					"Aktualisiert am" };
			for (int i = 0; i < columns.length; i++) {
				header.createCell(i).setCellValue(columns[i]);
				header.getCell(i).setCellStyle(headerStyle);
			}

			int rowIndex = 1;
			for (AdressEintrag eintrag : exportListe) {
				Row row = sheet.createRow(rowIndex++);
				int i = 0;
				row.createCell(i++).setCellValue(rowIndex - 1);
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getName()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getVorname()));
				row.createCell(i++).setCellValue(formatDate(eintrag.getGeburtstag()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getStrasse()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getPlz()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getWohnort()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getTelefonPrivat()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getTelefonGesch()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getTelefonMobil()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getEmailPrivat()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getEmailGesch()));
				row.createCell(i++).setCellValue(stringOrEmpty(eintrag.getBemerkung()));
				row.createCell(i++).setCellValue(formatDateTime(eintrag.getErstelltAm()));
				row.createCell(i++).setCellValue(formatDateTime(eintrag.getAktualisiertAm()));
			}

			for (int i = 0; i < columns.length; i++) {
				sheet.autoSizeColumn(i);
			}

			LocalDate heute = LocalDate.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			String datum = heute.format(formatter);

			workbook.write(out);
			return DefaultStreamedContent.builder().name("adressliste-" + datum + ".xlsx")
					.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
					.stream(() -> new ByteArrayInputStream(out.toByteArray())).build();
		} catch (IOException e) {
			e.printStackTrace();
			addError("Excel konnte nicht erzeugt werden.");
			return null;
		}
	}

	private List<AdressEintrag> getExportListe() {
		if (gefilterteEintraege != null) {
			return gefilterteEintraege;
		}
		if (eintraege != null) {
			return eintraege;
		}
		return new ArrayList<>();
	}

	private void addHeader(PdfPTable table, String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)));
		table.addCell(cell);
	}

	private String formatDate(LocalDate date) {
		return formatDateStatic(date);
	}

	private static String formatDateStatic(LocalDate date) {

		if (date == null) {
			return "";
		}
		return DATE_FORMAT.format(date);
	}

	public boolean spalteSichtbar(String key) {
		return isSpalteSichtbar(key);
	}

	public String infoQrCodeDataUrl(AdressEintrag eintrag) {
		return getInfoQrCodeDataUrl(eintrag);
	}

	private String formatDateTime(java.time.LocalDateTime dateTime) {
		if (dateTime == null) {
			return "";
		}
		return DATETIME_FORMAT.format(dateTime);
	}

	private String stringOrEmpty(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private void addInfo(String message) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, message, message));
	}

	private void addError(String message) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message));
	}

	public List<AdressEintrag> getEintraege() {
		return eintraege;
	}

	public List<AdressEintrag> getGefilterteEintraege() {
		return gefilterteEintraege;
	}

	public void setGefilterteEintraege(List<AdressEintrag> gefilterteEintraege) {
		this.gefilterteEintraege = gefilterteEintraege;
	}

	public AdressEintrag getBearbeiteterEintrag() {
		return bearbeiteterEintrag;
	}

	public AdressEintrag getNeuerEintrag() {
		return neuerEintrag;
	}

	public AdressEintrag getLoeschEintrag() {
		return loeschEintrag;
	}

	public StreamedContent getDownloadPdf() {
		return downloadPdf;
	}

	public StreamedContent getDownloadExcel() {
		return downloadExcel;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public Map<String, Boolean> getSichtbareSpalten() {
		return sichtbareSpalten;
	}

	public List<SpaltenDefinition> getSpaltenDefinitionen() {
		return SPALTEN_DEFINITIONEN;
	}

	public boolean isSpalteSichtbar(String key) {
		return Boolean.TRUE.equals(sichtbareSpalten.get(key));
	}

	public String getInfoQrCodeDataUrl(AdressEintrag eintrag) {
		try {
			QRCodeWriter writer = new QRCodeWriter();
			Map<EncodeHintType, Object> hints = new LinkedHashMap<>();
			hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
			hints.put(EncodeHintType.MARGIN, 1);

			BitMatrix matrix = writer.encode(baueInfoText(eintrag), BarcodeFormat.QR_CODE, 220, 220, hints);
			try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
				MatrixToImageWriter.writeToStream(matrix, "PNG", output);
				String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
				return "data:image/png;base64," + base64;
			}
		} catch (Exception e) {
			return "";
		}
	}

	public String baueInfoText(AdressEintrag eintrag) {
		String newline = "\n";
		StringBuilder vcard = new StringBuilder();
		vcard.append("BEGIN:VCARD").append(newline);
		vcard.append("VERSION:3.0").append(newline);
		vcard.append("N:").append(escapeVCardValue(eintrag.getName())).append(';')
				.append(escapeVCardValue(eintrag.getVorname())).append(";;;").append(newline);
		String fullName = (stringOrEmpty(eintrag.getVorname()) + " " + stringOrEmpty(eintrag.getName())).trim();
		vcard.append("FN;CHARSET=UTF-8:").append(escapeVCardValue(fullName)).append(newline);
		vcard.append("ADR;TYPE=HOME;CHARSET=UTF-8:;;").append(escapeVCardValue(eintrag.getStrasse())).append(';')
				.append(escapeVCardValue(eintrag.getWohnort())).append(";;").append(escapeVCardValue(eintrag.getPlz()))
				.append(";Deutschland").append(newline);
		if (!stringOrEmpty(eintrag.getTelefonMobil()).isBlank()) {
			vcard.append("TEL;TYPE=CELL:").append(escapeVCardValue(eintrag.getTelefonMobil())).append(newline);
		}
		if (!stringOrEmpty(eintrag.getTelefonPrivat()).isBlank()) {
			vcard.append("TEL;TYPE=HOME:").append(escapeVCardValue(eintrag.getTelefonPrivat())).append(newline);
		}
		if (!stringOrEmpty(eintrag.getTelefonGesch()).isBlank()) {
			vcard.append("TEL;TYPE=WORK:").append(escapeVCardValue(eintrag.getTelefonGesch())).append(newline);
		}
		if (!stringOrEmpty(eintrag.getEmailPrivat()).isBlank()) {
			vcard.append("EMAIL;TYPE=HOME:").append(escapeVCardValue(eintrag.getEmailPrivat())).append(newline);
		}
		if (!stringOrEmpty(eintrag.getEmailGesch()).isBlank()) {
			vcard.append("EMAIL;TYPE=WORK:").append(escapeVCardValue(eintrag.getEmailGesch())).append(newline);
		}
		if (eintrag.getGeburtstag() != null) {
			vcard.append("BDAY:").append(eintrag.getGeburtstag().format(DateTimeFormatter.BASIC_ISO_DATE))
					.append(newline);
		}
		if (!stringOrEmpty(eintrag.getBemerkung()).isBlank()) {
			vcard.append("NOTE;CHARSET=UTF-8:").append(escapeVCardValue(eintrag.getBemerkung())).append(newline);
		}
		vcard.append("END:VCARD");

		return vcard.toString();
	}

	private AdressEintrag findeEintragNachId(Integer id) {
		if (id == null || eintraege == null) {
			return null;
		}
		for (AdressEintrag eintrag : eintraege) {
			if (id.equals(eintrag.getId())) {
				return eintrag.copy();
			}
		}
		return null;
	}

	private void sendeAenderungsInfo(String aktion, AdressEintrag alterEintrag, AdressEintrag neuerEintrag) {
		String betroffenerName = baueAnzeigename(neuerEintrag != null ? neuerEintrag : alterEintrag);
		String betreff = "Adressliste " + aktion + " - " + getVerein() + " - " + betroffenerName;
		String nachricht = baueAenderungsText(aktion, alterEintrag, neuerEintrag);

		EmailService emailService = new EmailService(vereinnr, "TO:" + "michael.spahr@web.de");
		try {
			emailService.sendEmail(vereinnr, betreff, nachricht.replace("\n", "<br/>"), null, null);
		} catch (Exception e) {
			addError("Info-E-Mail konnte nicht gesendet werden: " + e.getMessage());
		}

		TelegrammService telegrammService = new TelegrammService();
		try {
			telegrammService.sendTelegramm(vereinnr, nachricht, null, null);
		} catch (Exception e) {
			addError("Telegram-Nachricht konnte nicht gesendet werden: " + e.getMessage());
		}
	}

	private String baueAenderungsText(String aktion, AdressEintrag alt, AdressEintrag neu) {
		StringBuilder sb = new StringBuilder();
		sb.append("Adressliste ").append(aktion).append("\n");
		sb.append("Verein: ").append(getVerein()).append(" (Vereinnr: ").append(vereinnr).append(")\n\n");
		sb.append("Alte Werte:\n").append(formatiereEintrag(alt)).append("\n\n");
		sb.append("Neue Werte:\n").append(formatiereEintrag(neu));
		return sb.toString();
	}

	private String formatiereEintrag(AdressEintrag eintrag) {
		if (eintrag == null) {
			return "-";
		}
		return "ID: " + stringOrEmpty(eintrag.getId()) + "\n" + "Name: " + stringOrEmpty(eintrag.getName()) + "\n"
				+ "Vorname: " + stringOrEmpty(eintrag.getVorname()) + "\n" + "Geburtstag: "
				+ formatDate(eintrag.getGeburtstag()) + "\n" + "Straße: " + stringOrEmpty(eintrag.getStrasse()) + "\n"
				+ "PLZ: " + stringOrEmpty(eintrag.getPlz()) + "\n" + "Wohnort: " + stringOrEmpty(eintrag.getWohnort())
				+ "\n" + "Telefon privat: " + stringOrEmpty(eintrag.getTelefonPrivat()) + "\n" + "Telefon gesch.: "
				+ stringOrEmpty(eintrag.getTelefonGesch()) + "\n" + "Telefon mobil: "
				+ stringOrEmpty(eintrag.getTelefonMobil()) + "\n" + "E-Mail privat: "
				+ stringOrEmpty(eintrag.getEmailPrivat()) + "\n" + "E-Mail gesch.: "
				+ stringOrEmpty(eintrag.getEmailGesch()) + "\n" + "Bemerkung: " + stringOrEmpty(eintrag.getBemerkung());
	}

	private String baueAnzeigename(AdressEintrag eintrag) {
		if (eintrag == null) {
			return "Unbekannt";
		}
		String name = (stringOrEmpty(eintrag.getVorname()) + " " + stringOrEmpty(eintrag.getName())).trim();
		return name.isBlank() ? "Unbekannt" : name;
	}

	private String escapeVCardValue(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n",
				"\\n");

	}

	private List<SpaltenDefinition> getSichtbareSpaltenDefinitionen() {
		List<SpaltenDefinition> result = new ArrayList<>();
		for (SpaltenDefinition spalte : SPALTEN_DEFINITIONEN) {
			if (isSpalteSichtbar(spalte.getKey())) {
				result.add(spalte);
			}
		}
		return result;
	}

	public String googleMapsLink(AdressEintrag eintrag) {
		if (eintrag == null) {
			return "https://www.google.com/maps";
		}

		String query = String.join(" ", stringOrEmpty(eintrag.getStrasse()), stringOrEmpty(eintrag.getPlz()),
				stringOrEmpty(eintrag.getWohnort())).trim();

		if (query.isBlank()) {
			return "https://www.google.com/maps";
		}

		return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
	}

	public static class SpaltenDefinition implements Serializable {

		private static final long serialVersionUID = 1L;
		private final String key;
		private final String label;
		private final Function<AdressEintrag, String> extractor;

		public SpaltenDefinition(String key, String label, Function<AdressEintrag, String> extractor) {
			this.key = key;
			this.label = label;
			this.extractor = extractor;
		}

		public String getKey() {
			return key;
		}

		public String getLabel() {
			return label;
		}

		public Function<AdressEintrag, String> getExtractor() {
			return extractor;
		}
	}

	public String getVerein() {
		return ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
	}

	private boolean pruefeSchreibzugriff(String passwortAusUrl) {
		if (passwortAusUrl == null || passwortAusUrl.isBlank()) {
			return false;
		}

		String konfigPasswort = ConfigManager.getAdresslistePassword(vereinnr);
		if (konfigPasswort == null || konfigPasswort.isBlank()) {
			return false;
		}

		if (passwortAusUrl.equals(konfigPasswort)) {
			return true;
		}

		try {
			String entschluesselt = ConfigManager.decryptPassword(vereinnr, konfigPasswort);
			return passwortAusUrl.equals(entschluesselt);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean hatSchreibzugriffBearbeiten() {
		if (schreibzugriff || editAktionenFreigeschaltet) {
			return true;
		}
		addError("Bitte zuerst das Einmalpasswort korrekt eingeben.");
		return false;
	}

	private boolean hatSchreibzugriffNeu() {
		if (schreibzugriff || neuSpeichernFreigeschaltet) {
			return true;

		}
		addError("Bitte zuerst ein Einmalpasswort anfordern und korrekt eingeben.");
		return false;
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}
}
