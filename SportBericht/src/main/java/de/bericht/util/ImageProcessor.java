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

		// Randbeschneidung durchführen
		BufferedImage croppedImage = image.getSubimage(cropX, cropY, cropWidth, cropHeight);

		// In Byte-Array zurückkonvertieren
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(croppedImage, "jpg", outputStream);
		return outputStream.toByteArray();
	}
}
