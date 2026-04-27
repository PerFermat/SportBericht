package de.bericht.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import de.bericht.service.DatabaseService;
import de.bericht.util.enums.SportartVerein;

public class ConfigManager {
	private static ConfigManager instance;
	private Properties properties;
	private static String passwort = "HängtT@geEbenNetzAmerik@n!sch3Bund3sk@nz1er!n@ng3l4Merk3l";
	private static final Cache<String, ConcurrentHashMap<String, String>> cache = Caffeine.newBuilder()
			.maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(500_000)).build();

	public ConfigManager() {
		properties = new Properties();
		loadProperties();
	}

	public static synchronized ConfigManager getInstance() {
		if (instance == null) {
			instance = new ConfigManager();
		}
		return instance;
	}

	public static synchronized ConfigManager updInstance() {
		instance = new ConfigManager();
		if (cache != null) {
			cache.cleanUp();
		}

		return instance;
	}

	private void loadProperties() {
		try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
			if (input == null) {
				throw new IOException("Konfigurationsdatei nicht gefunden!");
			}
			properties.load(input);
		} catch (IOException e) {
			System.err.println("Fehler beim Laden der Konfigurationsdatei: " + e.getMessage());
		}
	}

	public static String getConfigValue(String vereinnr, String eintrag) {
		// Versuche, die Map für die Vereinsnummer zu holen
		ConcurrentHashMap<String, String> eintraege = cache.getIfPresent(vereinnr);

		if (eintraege != null && eintraege.containsKey(eintrag)) {
			return eintraege.get(eintrag);
		}

		// Noch nicht im Cache -> aus DB laden
		DatabaseService dbService = new DatabaseService(vereinnr);

		// Map ggf. neu anlegen (thread-sicher)
		if (eintraege == null) {
			eintraege = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, String> existing = cache.asMap().putIfAbsent(vereinnr, eintraege);
			if (existing != null) {
				eintraege = existing;
			}
			List<ConfigEintrag> alleConfig = dbService.ladeConfigEintraege(vereinnr);
			for (ConfigEintrag configEintrag : alleConfig) {
				eintraege.put(configEintrag.getEintrag(), configEintrag.getWert());
			}
			return eintraege.get(eintrag);
		} else {

			String wert = dbService.leseConfig(vereinnr, eintrag);
			eintraege.put(eintrag, wert);
			return wert;
		}
	}

	public static String getWordpressValue(String vereinnr, String wo, String was) {
		String key = "wordpress." + wo + "." + was;
		return getConfigValue(vereinnr, key);
	}

	public static String getWordpressValue(String vereinnr, String wo, String was, int pos) {

		String key = "wordpress." + wo + "." + was;
		if (pos > 0) {
			return findeWertAnPosition(vereinnr, getConfigValue(vereinnr, key), pos);
		} else {
			return getConfigValue(vereinnr, key);
		}
	}

	public static String findeWertAnPosition(String vereinnr, String wert, int pos) {
		String[] teile = wert.split(",");
		return teile[--pos]; // Gib den Wert an der gefundenen Position zurück
	}

	public String getDatabaseUrl() {
		return resolveProperty("database.url", "DATABASE_URL");
	}

	public String getDatabaseUser() {
		return resolveProperty("database.user", "DATABASE_USER");
	}

	public static int getWortanzahlKiBericht(String vereinnr) {
		String value = getConfigValue(vereinnr, "gericht.ki.wortanzahl");

		if (value == null || value.isBlank()) {
			return 150;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 150;
		}
	}

	public String getDatabasePasswort() {
		String encrypted = resolveProperty("database.passwort", "DATABASE_Passwort");
		if (encrypted == null || encrypted.isBlank()) {
			return encrypted;
		}

		try {
			String decrypted = decryptPasswort("", encrypted);
			return decrypted;
		} catch (Exception e) {
			return encrypted;
		}
	}

	private String resolveProperty(String key, String envKey) {
		String value = System.getProperty(key);
		if (value != null && !value.isBlank()) {
			return value;
		}

		value = System.getenv(envKey);
		if (value != null && !value.isBlank()) {
			return value;
		}

		return properties.getProperty(key);
	}

	public String getTelegrammToken(String vereinnr) {
		String encrypted = getConfigValue(vereinnr, "telegramm.token");
		try {
			String decrypted = decryptPasswort(vereinnr, encrypted);
			return decrypted;
		} catch (Exception e) {
			return encrypted;
		}
	}

	public String getMailHost(String vereinnr) {
		return getConfigValue(vereinnr, "mail.smtp.host");
	}

	public String getMailPort(String vereinnr) {
		return getConfigValue(vereinnr, "mail.smtp.port");
	}

	public String getMailAuth(String vereinnr) {
		return getConfigValue(vereinnr, "mail.smtp.auth");
	}

	public String getMailStarttls(String vereinnr) {
		return getConfigValue(vereinnr, "mail.smtp.starttls.enable");
	}

	public String getMailUser(String vereinnr) {
		return getConfigValue(vereinnr, "mail.username");
	}

	public static String getMailPasswort(String vereinnr) {
		String encrypted = getConfigValue(vereinnr, "mail.passwort");
		try {
			String decrypted = decryptPasswort(vereinnr, encrypted);
			return decrypted;
		} catch (Exception e) {
			return encrypted;
		}
	}

	public static String getUserPasswort(String vereinnr) {
		String encrypted = getConfigValue(vereinnr, "user.passwort");
		try {
			String decrypted = decryptPasswort(vereinnr, encrypted);
			return decrypted;
		} catch (Exception e) {
			return encrypted;
		}
	}

	public static String getAdminPasswort(String vereinnr) {
		String encrypted = getConfigValue(vereinnr, "admin.passwort");
		try {
			String decrypted = decryptPasswort(vereinnr, encrypted);
			return decrypted;
		} catch (Exception e) {
			return encrypted;
		}
	}

	public static String getChatGptPasswort(String vereinnr) {
		String encrypted = getConfigValue(vereinnr, "bericht.ki.passwort");
		try {
			String decrypted = decryptPasswort(vereinnr, encrypted);
			return decrypted;
		} catch (Exception e) {
			return encrypted;
		}
	}

	public String getOrt(String vereinnr) {
		return getConfigValue(vereinnr, "spielplan.Ort");
	}

	public static String getMailEmpfaenger(String vereinnr) {
		return getConfigValue(vereinnr, "mail.bericht.empfaenger");
	}

	public static String getSpielplanLiga(String vereinnr) {
		return getConfigValue(vereinnr, "spielplan.Tabelle.Liga");
	}

	public static int findePosition(String komplett, String suche) {
		String[] teile = komplett.split(",");
		if (suche == null) {
			return -1;
		}
		for (int i = 0; i < teile.length; i++) {
			if (teile[i].toUpperCase().trim().equals(suche.trim().toUpperCase())) {
				return i + 1; // Position ist Index + 1, da die Zählung bei 1 beginnen soll
			}
		}
		return -1; // Suche nicht gefunden
	}

	public static String encryptPasswort(String vereinnr, String emailPasswort) throws Exception {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);

		String masterPasswort = passwort + vereinnr;
		SecretKeySpec secretKey = deriveKey(masterPasswort, salt);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		IvParameterSpec ivSpec = new IvParameterSpec(iv);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
		byte[] encrypted = cipher.doFinal(emailPasswort.getBytes("UTF-8"));

		byte[] combined = new byte[salt.length + iv.length + encrypted.length];
		System.arraycopy(salt, 0, combined, 0, salt.length);
		System.arraycopy(iv, 0, combined, salt.length, iv.length);
		System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

		return Base64.getEncoder().encodeToString(combined);
	}

	private static SecretKeySpec deriveKey(String passwort, byte[] salt) throws Exception {
		PBEKeySpec spec = new PBEKeySpec(passwort.toCharArray(), salt, 65536, 256);
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		byte[] key = factory.generateSecret(spec).getEncoded();
		return new SecretKeySpec(key, "AES");
	}

	public static String decryptPasswort(String vereinnr, String encrypted) throws Exception {
		byte[] combined = Base64.getDecoder().decode(encrypted);

		byte[] salt = new byte[16];
		byte[] iv = new byte[16];
		byte[] encryptedPasswort = new byte[combined.length - 32];

		System.arraycopy(combined, 0, salt, 0, 16);
		System.arraycopy(combined, 16, iv, 0, 16);
		System.arraycopy(combined, 32, encryptedPasswort, 0, encryptedPasswort.length);
		String masterPasswort = passwort + vereinnr;
		SecretKeySpec secretKey = deriveKey(masterPasswort, salt);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
		byte[] decrypted = cipher.doFinal(encryptedPasswort);

		return new String(decrypted, "UTF-8");
	}

	public static String erstellenPrompt(String vereinnr, String mannschaft, String anzahlWoerte, String vorkommnisse,
			String spielbericht, String stilrichtung, int anzahlBerichte) {
		String prompt = getConfigValue(vereinnr, "bericht.ki.prompt");

		Map<String, String> placeholders = Map.of("[anzahlWoerte]", anzahlWoerte, "[vorkommnisse]",
				" " + vorkommnisse + " ", "[spielbericht]", " " + spielbericht + " ", "[mannschaft]", mannschaft + " ",
				"[stilrichtung]", stilrichtung, "[anzahlBerichte]", String.valueOf(anzahlBerichte));

		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			prompt = prompt.replace(entry.getKey(), entry.getValue());
		}

		return prompt;
	}

	public static String erstellenPromptohneSpielbericht(String vereinnr, String turnier, String anzahlWoerte,
			String vorkommnisse, String spielbericht, String stilrichtung, int anzahlBerichte) {
		String prompt = getConfigValue(vereinnr, "bericht.ki.ohneSpielbericht");

		Map<String, String> placeholders = Map.of("[anzahlWoerte]", anzahlWoerte, "[vorkommnisse]",
				" " + vorkommnisse + " ", "[spielbericht]", " " + spielbericht + " ", "[stilrichtung]", stilrichtung,
				"[turnier]", turnier, "[anzahlBerichte]", String.valueOf(anzahlBerichte));

		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			prompt = prompt.replace(entry.getKey(), entry.getValue());
		}

		return prompt;
	}

	public static String erstellenFreiPrompt(String vereinnr, String mannschaft, String prompt, String anzahlWoerte,
			String vorkommnisse, String spielbericht, String stilrichtung, int anzahlBerichte) {

		Map<String, String> placeholders = Map.of("[anzahlWoerte]", anzahlWoerte, "[vorkommnisse]",
				" " + vorkommnisse + " ", "[spielbericht]", " " + spielbericht + " ", "[mannschaft]", mannschaft + " ",
				"[stilrichtung]", stilrichtung, "[anzahlBerichte]", String.valueOf(anzahlBerichte), "[altberichte]",
				spielbericht);

		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			prompt = prompt.replace(entry.getKey(), entry.getValue());
		}

		return prompt;
	}

	public static void clearCache(String vereinnr) {
		cache.invalidate(vereinnr);

	}

	public static String getSpielplanVerein(String vereinnr) {
		return getConfigValue(vereinnr, "spielplan.Verein");
	}

	public static String getSpielplanURL(String vereinnr) {
		return getConfigValue(vereinnr, "spielplan.URL");
	}

	public String getProgrammUrl(String vereinnr) {
		return getConfigValue(vereinnr, "programm.url");
	}

	public static boolean isTischtennis(String vereinnr) {
		return SportartVerein.TISCHTENNIS == SportartVerein.fromConfig(getConfigValue(vereinnr, "sportart.verein"));
	}

	public static boolean isTennis(String vereinnr) {
		return SportartVerein.TENNIS == SportartVerein.fromConfig(getConfigValue(vereinnr, "sportart.verein"));
	}

	public static String getSftpUrl(String vereinnr) {
		return getConfigValue(vereinnr, "sftp.url");
	}

	public static String getSftpPort(String vereinnr) {
		return getConfigValue(vereinnr, "sftp.port");
	}

	public static String getSftpUser(String vereinnr) {
		return getConfigValue(vereinnr, "sftp.user");
	}

	public static String getSftpPasswort(String vereinnr) {
		String encrypted = getConfigValue(vereinnr, "sftp.passwort");
		try {
			String decrypted = decryptPasswort(vereinnr, encrypted);
			return decrypted;
		} catch (Exception e) {
			return encrypted;
		}
	}

}
