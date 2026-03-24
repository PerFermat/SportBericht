package de.bericht.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import de.bericht.util.ConfigManager;

public final class DatabaseSchemaInitializer {

	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
	private static final String DEFAULT_JNDI_NAME = "java:/jdbc/TischtennisDS";
	private static volatile DataSource dataSource;

	private DatabaseSchemaInitializer() {
	}

	public static void initializeIfNeeded() {
		if (INITIALIZED.get()) {
			return;
		}

		synchronized (DatabaseSchemaInitializer.class) {
			if (INITIALIZED.get()) {
				return;
			}

			try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
				for (String sql : createStatements()) {
					stmt.execute(sql);
				}
				for (String sql : alterStatements()) {
					stmt.execute(sql);
				}

				INITIALIZED.set(true);
			} catch (SQLException e) {
				throw new IllegalStateException("Fehler beim Initialisieren des Datenbankschemas", e);
			}
		}
	}

	private static Connection openConnection() throws SQLException {
		DataSource ds = getDataSourceOrNull();
		if (ds != null) {
			return ds.getConnection();
		}

		ConfigManager config = ConfigManager.getInstance();
		return DriverManager.getConnection(buildJdbcUrl(config), config.getDatabaseUser(),
				config.getDatabasePassword());
	}

	private static DataSource getDataSourceOrNull() {
		if (dataSource != null) {
			return dataSource;
		}

		synchronized (DatabaseSchemaInitializer.class) {
			if (dataSource != null) {
				return dataSource;
			}

			String jndiName = resolveJndiName();
			try {
				InitialContext ctx = new InitialContext();
				dataSource = (DataSource) ctx.lookup(jndiName);
				return dataSource;
			} catch (NamingException e) {
				System.out.println("Keine WildFly-DataSource gefunden unter '" + jndiName
						+ "'. Fallback auf config.properties / System-Properties.");
				return null;
			}
		}
	}

	private static String resolveJndiName() {
		String sys = System.getProperty("database.jndi");
		if (sys != null && !sys.isBlank()) {
			return sys.trim();
		}

		String env = System.getenv("DATABASE_JNDI");
		if (env != null && !env.isBlank()) {
			return env.trim();
		}

		return DEFAULT_JNDI_NAME;
	}

	private static String buildJdbcUrl(ConfigManager config) {
		String url = config.getDatabaseUrl();
		if (url == null || url.isBlank()) {
			throw new IllegalStateException("Database-URL ist nicht gesetzt.");
		}

		if (url.contains("?")) {
			if (!url.contains("useUnicode=")) {
				url += "&useUnicode=true&characterEncoding=UTF-8";
			}
			return url;
		}

		return url + "?useUnicode=true&characterEncoding=UTF-8";
	}

	private static List<String> createStatements() {
		return List.of(
				"CREATE TABLE IF NOT EXISTS Nachname (Name VARCHAR(600) NOT NULL, PRIMARY KEY (Name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS Vorname (Name VARCHAR(600) NOT NULL, Geschlecht CHAR(1) DEFAULT NULL, PRIMARY KEY (Name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS adressliste (id INT(10) UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(100) NOT NULL, vorname VARCHAR(100) NOT NULL, geburtstag DATE DEFAULT NULL, strasse VARCHAR(150) DEFAULT NULL, plz VARCHAR(10) DEFAULT NULL, wohnort VARCHAR(150) DEFAULT NULL, telefon_privat VARCHAR(150) DEFAULT NULL, telefon_gesch VARCHAR(150) DEFAULT NULL, telefon_mobil VARCHAR(150) DEFAULT NULL, email_privat VARCHAR(150) DEFAULT NULL, email_gesch VARCHAR(150) DEFAULT NULL, bemerkung TEXT DEFAULT NULL, erstellt_am TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, aktualisiert_am TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, vereinnr VARCHAR(10) DEFAULT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS adressliste_historie (historie_id INT(11) NOT NULL AUTO_INCREMENT, original_id INT(11) NOT NULL, vereinnr VARCHAR(30) NOT NULL, name VARCHAR(150) DEFAULT NULL, vorname VARCHAR(150) DEFAULT NULL, geburtstag DATE DEFAULT NULL, strasse VARCHAR(255) DEFAULT NULL, plz VARCHAR(20) DEFAULT NULL, wohnort VARCHAR(150) DEFAULT NULL, telefon_privat VARCHAR(50) DEFAULT NULL, telefon_gesch VARCHAR(50) DEFAULT NULL, telefon_mobil VARCHAR(50) DEFAULT NULL, email_privat VARCHAR(255) DEFAULT NULL, email_gesch VARCHAR(255) DEFAULT NULL, bemerkung TEXT DEFAULT NULL, erstellt_am TIMESTAMP NULL DEFAULT NULL, aktualisiert_am TIMESTAMP NULL DEFAULT NULL, verschoben_am TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (historie_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS aufstellung (id INT(11) NOT NULL AUTO_INCREMENT, vereinnr VARCHAR(10) DEFAULT NULL, mannschaft VARCHAR(10) DEFAULT NULL, rang VARCHAR(10) DEFAULT NULL, qttr VARCHAR(10) DEFAULT NULL, name VARCHAR(100) DEFAULT NULL, a VARCHAR(10) DEFAULT NULL, status VARCHAR(50) DEFAULT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
				"CREATE TABLE IF NOT EXISTS berichte (ergebnisLink VARCHAR(255) NOT NULL, berichtText TEXT DEFAULT NULL, bild LONGBLOB DEFAULT NULL, bildUnterschrift TEXT DEFAULT NULL, vereinnr VARCHAR(10) NOT NULL DEFAULT '13014', ueberschrift VARCHAR(256) DEFAULT NULL, Spielstatistik TEXT DEFAULT NULL, mitSpielberichte TINYINT(1) DEFAULT NULL, timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, liga VARCHAR(255) DEFAULT NULL, heim VARCHAR(255) DEFAULT NULL, gast VARCHAR(255) DEFAULT NULL, datum VARCHAR(50) DEFAULT NULL, ergebnis VARCHAR(100) DEFAULT NULL, PRIMARY KEY (vereinnr, ergebnisLink)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS berichte_historie (id BIGINT(20) NOT NULL AUTO_INCREMENT, ergebnisLink VARCHAR(255) NOT NULL, berichtText TEXT DEFAULT NULL, bild LONGBLOB DEFAULT NULL, bildUnterschrift VARCHAR(255) DEFAULT NULL, timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, vereinnr VARCHAR(10) NOT NULL DEFAULT '13014', ueberschrift VARCHAR(256) DEFAULT NULL, Spielstatistik TEXT DEFAULT NULL, liga VARCHAR(255) DEFAULT NULL, heim VARCHAR(255) DEFAULT NULL, gast VARCHAR(255) DEFAULT NULL, datum VARCHAR(50) DEFAULT NULL, ergebnis VARCHAR(100) DEFAULT NULL, PRIMARY KEY (id), KEY idx_vereinnr_ergebnisLink_timestamp (vereinnr, ergebnisLink, timestamp)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS config (vereinnr VARCHAR(10) NOT NULL, eintrag VARCHAR(255) NOT NULL, wert TEXT DEFAULT NULL, PRIMARY KEY (vereinnr, eintrag)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
				"CREATE TABLE IF NOT EXISTS doppel (ergebnisLink VARCHAR(255) NOT NULL, uuid VARCHAR(255) NOT NULL, timestamp TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, vereinnr VARCHAR(10) NOT NULL DEFAULT '13014', PRIMARY KEY (vereinnr, ergebnisLink, uuid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS log_tabelle (ergebnislink VARCHAR(255) NOT NULL, timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), name VARCHAR(255) DEFAULT NULL, aktion VARCHAR(255) DEFAULT NULL, mailErfolgreich VARCHAR(1) DEFAULT NULL, vereinnr VARCHAR(10) NOT NULL DEFAULT '13014', info TEXT DEFAULT NULL, PRIMARY KEY (vereinnr, ergebnislink, timestamp), KEY idx_log_ergebnis_aktion (vereinnr, ergebnislink, aktion)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS mannschaft (id INT(11) NOT NULL AUTO_INCREMENT, vereinnr VARCHAR(50) NOT NULL, mannschaft VARCHAR(255) DEFAULT NULL, mannschaft_url VARCHAR(500) DEFAULT NULL, liga VARCHAR(255) DEFAULT NULL, liga_url VARCHAR(500) DEFAULT NULL, rang VARCHAR(50) DEFAULT NULL, punkte VARCHAR(50) DEFAULT NULL, PRIMARY KEY (id, vereinnr), KEY idx_vereinnr (vereinnr)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS short_links (code VARCHAR(50) NOT NULL, target_url TEXT NOT NULL, created_at TIMESTAMP NOT NULL, click_count INT(11) NOT NULL DEFAULT 0, last_at TIMESTAMP NULL DEFAULT NULL, PRIMARY KEY (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS spielcodes (id INT(11) NOT NULL AUTO_INCREMENT, unique_key CHAR(64) NOT NULL, liga VARCHAR(100) NOT NULL, vereinnr VARCHAR(100) NOT NULL, mannschaft VARCHAR(255) NOT NULL, wochentag VARCHAR(50) NOT NULL, datum VARCHAR(50) NOT NULL, uhrzeit VARCHAR(50) NOT NULL, heimmannschaft VARCHAR(255) NOT NULL, gastmannschaft VARCHAR(255) NOT NULL, spiel_code VARCHAR(255) NOT NULL, pin VARCHAR(255) NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_unique_key (unique_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS spieler_verfuegbarkeit (vereinnr VARCHAR(20) NOT NULL, datum VARCHAR(20) NOT NULL, uhrzeit VARCHAR(20) NOT NULL, name VARCHAR(255) NOT NULL, mannschaft VARCHAR(255) DEFAULT NULL, verfuegbarkeit VARCHAR(255) DEFAULT NULL, kommentar TEXT DEFAULT NULL, PRIMARY KEY (vereinnr, datum, uhrzeit, name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS spiellokal (mannschaft VARCHAR(255) NOT NULL, bezeichnung VARCHAR(255) NOT NULL, name VARCHAR(255) DEFAULT NULL, strasse VARCHAR(255) DEFAULT NULL, plz_ort VARCHAR(255) DEFAULT NULL, PRIMARY KEY (mannschaft, bezeichnung)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS spielplan_betreuer (unique_key VARCHAR(255) NOT NULL, betreuer TEXT DEFAULT NULL, bestaetigt TINYINT(1) DEFAULT NULL, kommentar TEXT DEFAULT NULL, mailTimestamp DATETIME DEFAULT NULL, PRIMARY KEY (unique_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS spielplan_spieler (id BIGINT(20) NOT NULL AUTO_INCREMENT, unique_key VARCHAR(255) NOT NULL, vereinnr VARCHAR(20) NOT NULL, mannschaft VARCHAR(50) DEFAULT NULL, rang VARCHAR(20) DEFAULT NULL, name VARCHAR(255) DEFAULT NULL, ausgewaehlt TINYINT(1) DEFAULT 0, kommentar VARCHAR(255) DEFAULT NULL, PRIMARY KEY (id), KEY idx_spielplan_spieler_unique (unique_key, vereinnr)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS spielplan_spielkommentar (id BIGINT(20) NOT NULL AUTO_INCREMENT, unique_key VARCHAR(255) NOT NULL, vereinnr VARCHAR(20) NOT NULL, kommentar TEXT DEFAULT NULL, PRIMARY KEY (id), UNIQUE KEY uk_spielkommentar (unique_key, vereinnr)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS spielplan_tabelle (unique_key VARCHAR(64) NOT NULL, vereinnr VARCHAR(10) DEFAULT NULL, wochentag_datum VARCHAR(255) DEFAULT NULL, wochentag VARCHAR(40) DEFAULT NULL, datum VARCHAR(20) DEFAULT NULL, zeit VARCHAR(20) DEFAULT NULL, liga VARCHAR(255) DEFAULT NULL, heim VARCHAR(255) DEFAULT NULL, gast VARCHAR(255) DEFAULT NULL, ergebnis VARCHAR(100) DEFAULT NULL, ergebnis_link TEXT DEFAULT NULL, heim_link TEXT DEFAULT NULL, gast_link TEXT DEFAULT NULL, created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, spiellokal VARCHAR(255) DEFAULT NULL, PRIMARY KEY (unique_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS stilelemente (id INT(11) NOT NULL AUTO_INCREMENT, sprachebene VARCHAR(100) DEFAULT NULL, stimmung VARCHAR(100) DEFAULT NULL, tonfall VARCHAR(100) DEFAULT NULL, humor VARCHAR(100) DEFAULT NULL, wertung VARCHAR(100) DEFAULT NULL, struktur VARCHAR(100) DEFAULT NULL, ergebnis ENUM('Sieg','Niederlage','Unentschieden','immer') DEFAULT NULL, wirkung TEXT DEFAULT NULL, vorschautext TEXT DEFAULT NULL, PRIMARY KEY (id), KEY idx_wirkung (wirkung(100)), KEY idx_ergebnis_wirkung (ergebnis, wirkung(100))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS config_gesamtspielplan (id INT NOT NULL AUTO_INCREMENT, vereinnr VARCHAR(10) NOT NULL, spalte INT NOT NULL, liga_anzeige TEXT DEFAULT NULL, mannschaft_anzeige TEXT DEFAULT NULL, betreuer TINYINT(1) NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_config_gesamtspielplan_vereinnr_spalte (vereinnr, spalte), KEY idx_config_gesamtspielplan_vereinnr (vereinnr)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
				"CREATE TABLE IF NOT EXISTS config_gesamtspielplan_mannschaft (id INT NOT NULL AUTO_INCREMENT, vereinnr VARCHAR(10) NOT NULL, id_spalte INT NOT NULL, liga TEXT DEFAULT NULL, mannschaft TEXT DEFAULT NULL, PRIMARY KEY (id), KEY idx_config_gesamtspielplan_mannschaft_vereinnr (vereinnr), KEY idx_config_gesamtspielplan_mannschaft_spalte (id_spalte), CONSTRAINT fk_config_gesamtspielplan_mannschaft_spalte FOREIGN KEY (id_spalte) REFERENCES config_gesamtspielplan(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
				"CREATE TABLE IF NOT EXISTS config_gesamtspielplan_runde (id INT NOT NULL AUTO_INCREMENT, vereinnr VARCHAR(10) NOT NULL, name TEXT NOT NULL, datum_von DATE DEFAULT NULL, datum_bis DATE DEFAULT NULL, PRIMARY KEY (id), KEY idx_config_gesamtspielplan_runde_vereinnr (vereinnr)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",				
				"CREATE TABLE IF NOT EXISTS web_pages (id INT(11) NOT NULL AUTO_INCREMENT, url VARCHAR(1024) NOT NULL, html_content LONGTEXT NOT NULL, created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (id), UNIQUE KEY url (url) USING HASH) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",
				"CREATE TABLE IF NOT EXISTS VersendeteMails (id BIGINT(20) NOT NULL AUTO_INCREMENT, timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, empfaenger TEXT NOT NULL, empfaenger_cc TEXT DEFAULT NULL, empfaenger_bcc TEXT DEFAULT NULL, betreff VARCHAR(500), text LONGTEXT, attachmentName TEXT , PRIMARY KEY (id), KEY idx_versendete_mails_timestamp (timestamp)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
	}

	private static List<String> alterStatements() {
		return List.of(
				"ALTER TABLE berichte ADD COLUMN IF NOT EXISTS timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
				"ALTER TABLE berichte ADD COLUMN IF NOT EXISTS liga VARCHAR(255) DEFAULT NULL",
				"ALTER TABLE berichte ADD COLUMN IF NOT EXISTS heim VARCHAR(255) DEFAULT NULL",
				"ALTER TABLE berichte ADD COLUMN IF NOT EXISTS gast VARCHAR(255) DEFAULT NULL",
				"ALTER TABLE berichte ADD COLUMN IF NOT EXISTS datum VARCHAR(50) DEFAULT NULL",
				"ALTER TABLE berichte ADD COLUMN IF NOT EXISTS ergebnis VARCHAR(100) DEFAULT NULL",
				"ALTER TABLE berichte MODIFY COLUMN timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
				"ALTER TABLE berichte_historie ADD COLUMN IF NOT EXISTS liga VARCHAR(255) DEFAULT NULL",
				"ALTER TABLE berichte_historie ADD COLUMN IF NOT EXISTS heim VARCHAR(255) DEFAULT NULL",
				"ALTER TABLE berichte_historie ADD COLUMN IF NOT EXISTS gast VARCHAR(255) DEFAULT NULL",
				"ALTER TABLE berichte_historie ADD COLUMN IF NOT EXISTS datum VARCHAR(50) DEFAULT NULL",
				"ALTER TABLE berichte_historie ADD COLUMN IF NOT EXISTS ergebnis VARCHAR(100) DEFAULT NULL",
				"ALTER TABLE berichte_historie MODIFY COLUMN timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
	}

}