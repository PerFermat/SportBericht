package de.bericht.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class WebCache {
	private static final Cache<String, Document> cache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
			.maximumSize(100) // Maximal 100 Seiten im Cache
			.build();

	public static Document getPage(String url) throws IOException {
		return cache.get(url, key -> {
			try {
				return Jsoup.connect(key).header("Content-Type", "text/html; charset=UTF-8").get();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	// Gesamten Cache leeren
	public static void clearCache() {
		cache.invalidateAll();
	}

}
