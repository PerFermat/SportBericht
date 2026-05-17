package de.meintt.testneu;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import de.bericht.service.SpielCodeParser;
import de.bericht.util.SpielCode;

class SpielCodeParserPdfImportTest {
	// @Disabled("temporär deaktiviert")
	@Test
	void parseFromPdf_shouldPrintParsedValuesFromGivenFile() throws Exception {
		String pdfDateiPfad = "/home/michael/Nextcloud/Tischtennis/Spielcodes/Saison 25-26/E BL-Erwachsene1-Spiel-Code.pdf";
		assertFalse(pdfDateiPfad == null || pdfDateiPfad.isBlank(),
				"Bitte Test-PDF angeben, z.B. -Dspielcode.test.pdf=/pfad/zur/datei.pdf");

		Path datei = Path.of(pdfDateiPfad);
		assertFalse(!Files.exists(datei), "Angegebene PDF-Datei existiert nicht: " + datei);

		try (PDDocument document = PDDocument.load(datei.toFile())) {
			SpielCodeParser scp = new SpielCodeParser("13014", "Erwachsene1", "E BL", document);
			List<SpielCode> result = scp.getSpiele();

			System.out.println("=== Geparste SpielCode-Einträge aus: " + datei + " ===");
			if (result.isEmpty()) {
				System.out.println("Keine Einträge gefunden.");
				return;
			}

			for (SpielCode spiel : result) {
				System.out.println("----------------------------------------");
				System.out.println("Mannschaft: " + spiel.getMannschaft());
				System.out.println("Liga      : " + spiel.getLiga());
				System.out.println("Wochentag : " + spiel.getWochentag());
				System.out.println("Datum     : " + spiel.getDatum());
				System.out.println("Uhrzeit   : " + spiel.getUhrzeit());
				System.out.println("Heim      : " + spiel.getHeimmannschaft());
				System.out.println("Gast      : " + spiel.getGastmannschaft());
				System.out.println("SpielCode : " + spiel.getSpielCode());
				System.out.println("Pin       : " + spiel.getPin());
			}
		}
	}
}
