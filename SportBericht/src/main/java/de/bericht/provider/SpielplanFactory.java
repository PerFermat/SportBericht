package de.bericht.provider;

import de.bericht.service.SpielplanService;
import de.bericht.service.SpielplanServiceClick;
import de.bericht.service.SpielplanServiceDb;
import de.bericht.service.TennisSpielplanService;
import de.bericht.util.ConfigManager;

public class SpielplanFactory {

	public static SpielplanProvider create(String vereinnr) throws Exception {
		String url = ConfigManager.getSpielplanURL(vereinnr);
		return createForUrl(vereinnr, url);
	}

	public static SpielplanProvider create(String vereinnr, String url) throws Exception {
		return createForUrl(vereinnr, url);
	}

	public static SpielplanProvider createForUrl(String vereinnr, String url) throws Exception {
		if (!isHttpUrl(url)) {
			return createDbFallback(vereinnr, url);
		}

		try {
			SpielplanProvider provider;
			String urllow = url.toLowerCase();

			if (urllow.contains("wtb-tennis")) {
				provider = new TennisSpielplanService(vereinnr, url);
			} else if (urllow.contains("click-tt.de")) {
				provider = new SpielplanServiceClick(vereinnr, url);
			} else {
				provider = new SpielplanService(vereinnr, url);
			}

			if (hasError(provider)) {
				return createDbFallback(vereinnr, url);
			}

			return provider;
		} catch (Exception e) {
			return createDbFallback(vereinnr, url);
		}
	}

	private static boolean hasError(SpielplanProvider provider) {
		if (provider instanceof SpielplanServiceClick clickProvider) {
			return clickProvider.getFehlercode() != 0;
		}
		if (provider instanceof SpielplanService serviceProvider) {
			return serviceProvider.getFehlercode() != 0;
		}
		if (provider instanceof TennisSpielplanService tennisProvider) {
			return tennisProvider.getFehlercode() != 0;
		}
		return false;
	}

	private static boolean isHttpUrl(String url) {
		if (url == null) {
			return false;
		}
		String urlTrimmed = url.trim().toLowerCase();
		return urlTrimmed.startsWith("http://") || urlTrimmed.startsWith("https://");
	}

	private static SpielplanProvider createDbFallback(String vereinnr, String url) {
		String fallbackUrl = (url == null || url.isBlank()) ? "Konfiguration" : url;
		return new SpielplanServiceDb(vereinnr, fallbackUrl);
	}
}