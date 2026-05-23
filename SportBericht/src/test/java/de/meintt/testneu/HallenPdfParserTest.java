package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bericht.service.HallenPdfParser;

public class HallenPdfParserTest {
	@Disabled("temporär deaktiviert")
	@Test
	void testPdfParser() throws Exception {

		// PDF-Datei festlegen
		String dateiname = "/home/michael/Nextcloud/Skripts/Python/Hallenbelegung.pdf";

		try (InputStream inputStream = new FileInputStream(dateiname)) {

			HallenPdfParser parser = new HallenPdfParser(inputStream);

			String html = parser.getHtmlText();

			// Grundprüfungen
			assertNotNull(html);
			assertTrue(html.contains("<html>"));

			// Prüfen ob Montag/Freitag enthalten
			assertTrue(html.contains("Montag") || html.contains("Freitag"));

			// Prüfen ob Halle gefunden wurde
			assertTrue(html.contains("Halle"));

			// Optional Ausgabe zur Kontrolle
			System.out.println(html);
		}
	}
}