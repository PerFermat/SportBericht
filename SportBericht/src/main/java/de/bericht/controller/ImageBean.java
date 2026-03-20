package de.bericht.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.imageio.ImageIO;

import de.bericht.service.DatabaseService;
import de.bericht.util.ConfigManager;
import de.bericht.util.ImageProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServlet;

@Named
@ViewScoped
public class ImageBean extends HttpServlet implements Serializable {
	private static final long serialVersionUID = 1L;
	private String heim;
	private String gast;
	private String datum;
	private String vereinnr;
	private String ergebnis;
	private String berichtText;
	private String ergebnisLink;
	private String cropX = "0"; // String
	private String cropY = "0"; // String
	private String cropWidth = "0"; // String
	private String cropHeight = "0"; // String
	private byte[] processedImage;
	private String imagePath;
	private String liga;
	private String uuid;
	private DatabaseService dbService = new DatabaseService();

	@Override
	@PostConstruct
	public void init() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		this.vereinnr = params.get("vereinnr");
		this.heim = params.get("heim");
		this.gast = params.get("gast");
		this.datum = params.get("datum");
		this.ergebnis = params.get("ergebnis");
		this.ergebnisLink = params.get("ergebnisLink");
		this.berichtText = params.get("berichtText");
		this.liga = params.get("liga");
		this.uuid = params.get("uuid");
		imagePath = "data:image/jpg;base64," + java.util.Base64.getEncoder().encodeToString(loadImageFromDatabase());

		try {
			// Byte-Array in ein BufferedImage umwandeln
			byte[] originalImage = loadImageFromDatabase();

			ByteArrayInputStream bis = new ByteArrayInputStream(originalImage);
			BufferedImage image = ImageIO.read(bis);

			// Bildgröße abrufen
			this.cropWidth = String.valueOf(image.getWidth());
			this.cropHeight = String.valueOf(image.getHeight());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	public String getCropX() {
		return cropX;
	}

	public void setCropX(String cropX) {
		this.cropX = cropX;
	}

	public String getCropY() {
		return cropY;
	}

	public void setCropY(String cropY) {
		this.cropY = cropY;
	}

	public String getCropWidth() {
		return cropWidth;
	}

	public void setCropWidth(String cropWidth) {
		this.cropWidth = cropWidth;
	}

	public String getCropHeight() {
		return cropHeight;
	}

	public void setCropHeight(String cropHeight) {
		this.cropHeight = cropHeight;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void coordinateSelected() {
		// Diese Methode ist optional,
		// aber praktisch wenn du nach
		// jeder Auswahl sofort reagierst (z.B. Logging, live Feedback)
		System.out.println("Koordinaten aktualisiert: " + cropX + "," + cropY + "," + cropWidth + ", " + cropHeight);
	}

	public void cropImage() {
		try {
			int x = Integer.parseInt(cropX);
			int y = Integer.parseInt(cropY);
			int width = Integer.parseInt(cropWidth);
			int height = Integer.parseInt(cropHeight);
			byte[] originalImage = loadImageFromDatabase();
			processedImage = ImageProcessor.processImage(originalImage, x, y, width, height);
			dbService.saveBerichtData(vereinnr, ergebnisLink, processedImage);
			imagePath = "data:image/jpg;base64," + java.util.Base64.getEncoder().encodeToString(processedImage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private byte[] loadImageFromDatabase() {
		return dbService.loadBerichtData(vereinnr, ergebnisLink).getBild();
	}

	public String getHeim() {
		return heim;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

	public String getGast() {
		return gast;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public String getDatum() {
		return datum;
	}

	public void setDatum(String datum) {
		this.datum = datum;
	}

	public String getErgebnis() {
		return ergebnis;
	}

	public void setErgebnis(String ergebnis) {
		this.ergebnis = ergebnis;
	}

	public String getBerichtText() {
		return berichtText;
	}

	public void setBerichtText(String berichtText) {
		this.berichtText = berichtText;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public void updateCropValues(String startX, String startY, String width, String height) {
		System.out.println("Crop-Werte aktualisiert: X=" + startX + ", Y=" + startY + ", Breite=" + cropWidth
				+ ", Höhe=" + cropHeight);
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void updBearbeitung() {
		dbService.verarbeiteEintrag(vereinnr, ergebnisLink, uuid); // Fügt einen neuen Eintrag hinzu
	}

	// Diese Methode wird durch den "Speichern"-Button aufgerufen.
	public void zurueck() {
		dbService.deleteUUID(vereinnr, ergebnisLink, uuid);
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

}