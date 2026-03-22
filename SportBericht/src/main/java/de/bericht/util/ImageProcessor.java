package de.bericht.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class ImageProcessor {

	public static byte[] processImage(byte[] imageData, int cropX, int cropY, int cropWidth, int cropHeight)
			throws IOException {
		// Bild aus Byte-Array lesen
		InputStream inputStream = new ByteArrayInputStream(imageData);
		BufferedImage image = ImageIO.read(inputStream);

		if (image == null) {
			throw new IOException("Ungültiges Bildformat oder leere Eingabe.");
		}
		
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		if (imageWidth <= 0 || imageHeight <= 0) {
			throw new IOException("Bild hat keine gültigen Dimensionen.");
		}

		int safeX = clamp(cropX, 0, imageWidth - 1);
		int safeY = clamp(cropY, 0, imageHeight - 1);

		int maxCropWidth = imageWidth - safeX;
		int maxCropHeight = imageHeight - safeY;

		int safeWidth = clamp(cropWidth, 1, maxCropWidth);
		int safeHeight = clamp(cropHeight, 1, maxCropHeight);

		// Randbeschneidung durchführen
		BufferedImage croppedImage = image.getSubimage(safeX, safeY, safeWidth, safeHeight);

		// In Byte-Array zurückkonvertieren
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(croppedImage, "jpg", outputStream);
		return outputStream.toByteArray();
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(value, max));
	}

}
