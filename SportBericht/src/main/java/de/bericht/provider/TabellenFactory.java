package de.bericht.provider;

import de.bericht.service.TennisTabellenService;
import de.bericht.service.TischtennisTabelleService;

public class TabellenFactory {

	public static TabellenProvider create(String url) throws Exception {
		return createForUrl(url);
	}

	public static TabellenProvider createForUrl(String url) throws Exception {

		try {
			TabellenProvider provider;
			String urllow = url.toLowerCase();

			if (urllow.contains("wtb-tennis")) {
				provider = new TennisTabellenService(url);
			} else {
				provider = new TischtennisTabelleService(url);
			}

			return provider;
		} catch (Exception e) {
			return null;
		}
	}

}