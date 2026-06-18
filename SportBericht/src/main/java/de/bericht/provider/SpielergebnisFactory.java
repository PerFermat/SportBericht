package de.bericht.provider;

import de.bericht.service.SpielergebnisClickTTService;
import de.bericht.service.SpielergebnisService;
import de.bericht.service.TennisSpielergebnisService;
import de.bericht.util.NamensSpeicher;
import de.bericht.util.enums.HeimGastArt;

public class SpielergebnisFactory {

	public static SpielergebnisProvider create(String vereinnr, String url, NamensSpeicher ns) {
		return create(vereinnr, url, ns, true);
	}

	public static SpielergebnisProvider create(String vereinnr, String url, NamensSpeicher ns, Boolean verschluesseln) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("URL is missing for Verein: " + vereinnr);
		}

		String berichtMannschaft = de.bericht.util.ConfigManager.getConfigValue(vereinnr, "spielplan.Verein");
		return create(vereinnr, HeimGastArt.VEREIN, berichtMannschaft, url, ns, verschluesseln);
	}

	public static SpielergebnisProvider create(String vereinnr, HeimGastArt art, String berichtMannschaft, String url,
			NamensSpeicher ns) {
		return create(vereinnr, art, berichtMannschaft, url, ns, true);
	}

	public static SpielergebnisProvider create(String vereinnr, HeimGastArt art, String berichtMannschaft, String url,
			NamensSpeicher ns, Boolean verschluesseln) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("URL is missing for Verein: " + vereinnr);
		}

		String urllow = url.toLowerCase();

		if (urllow.contains("wtb-tennis")) {
			return new TennisSpielergebnisService(vereinnr, art, berichtMannschaft, url, ns, verschluesseln);
		}
		if (urllow.contains("click-tt.de")) {
			return new SpielergebnisClickTTService(vereinnr, art, berichtMannschaft, url, ns, verschluesseln);
		}
		return new SpielergebnisService(vereinnr, art, berichtMannschaft, url, ns, verschluesseln);
	}
}