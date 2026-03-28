package de.bericht.provider;

import de.bericht.service.TennisBilanzService;
import de.bericht.service.TischtennisBilanzService;

public class BilanzFactory {

	public static BilanzProvider create(String vereinnr, String url) throws Exception {
		return createForUrl(vereinnr, url);
	}

	public static BilanzProvider createForUrl(String vereinnr, String url) throws Exception {

		try {
			BilanzProvider provider;
			String urllow = url.toLowerCase();

			if (urllow.contains("wtb-tennis")) {
				provider = new TennisBilanzService(vereinnr, url);
			} else {
				provider = new TischtennisBilanzService(vereinnr, url);
			}

			return provider;
		} catch (Exception e) {
			return null;
		}
	}

}