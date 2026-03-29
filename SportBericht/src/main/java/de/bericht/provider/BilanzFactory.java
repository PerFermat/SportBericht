package de.bericht.provider;

import de.bericht.service.TennisBilanzService;
import de.bericht.service.TischtennisBilanzService;
import de.bericht.util.NamensSpeicher;

public class BilanzFactory {

	public static BilanzProvider create(String vereinnr, String url, NamensSpeicher namensSepeicher,
			Boolean verschluesseln) throws Exception {
		return createForUrl(vereinnr, url, namensSepeicher, verschluesseln);
	}

	public static BilanzProvider createForUrl(String vereinnr, String url, NamensSpeicher namensSepeicher,
			Boolean verschluesseln) throws Exception {

		try {
			BilanzProvider provider;
			String urllow = url.toLowerCase();

			if (urllow.contains("wtb-tennis")) {
				provider = new TennisBilanzService(vereinnr, url, namensSepeicher, verschluesseln);
			} else {
				provider = new TischtennisBilanzService(vereinnr, url, namensSepeicher, verschluesseln);
			}

			return provider;
		} catch (Exception e) {
			return null;
		}
	}

}