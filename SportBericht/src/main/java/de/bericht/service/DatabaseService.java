package de.bericht.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import de.bericht.util.AdressEintrag;
import de.bericht.util.BerichtData;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigBedeutung;
import de.bericht.util.ConfigEintrag;
import de.bericht.util.ConfigKategorie;
import de.bericht.util.ConfigManager;
import de.bericht.util.ErgebnisCache;
import de.bericht.util.Stil;
import de.bericht.util.TennisGruppeKurz;

public class DatabaseService {

	// Verbindungsparameter werden aus der config-Datei geladen
	private static ConfigManager config;
	private final Random random = new Random();
	private static final String DEFAULT_JNDI_NAME = "java:/jdbc/TischtennisDS";
	private static volatile DataSource dataSource;
	private static boolean spielplanTablePrepared = false;
	static {

		try {
			config = ConfigManager.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public DatabaseService(String vereinnr) {
	}

	public DatabaseService() {
	}

	public BerichtData loadBerichtData(String vereinnr, String ergebnisLink) {
		String sql = "SELECT berichtText, bild , bildUnterschrift, ueberschrift, mitSpielberichte FROM berichte WHERE ergebnisLink = ? and vereinnr = ?";
		BerichtData data = new BerichtData();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, ergebnisLink);
			pstmt.setString(2, vereinnr);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				data.setBerichtText(rs.getString("berichtText"));
				data.setBild(rs.getBytes("bild"));
				data.setBildUnterschrift(rs.getString("bildUnterschrift"));
				data.setUeberschrift(rs.getString("ueberschrift"));
				data.setMitSpielberichte(rs.getBoolean("mitSpielberichte"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return data;
	}

	public String loadUeberschrift(String vereinnr, String ueberschrift) {
		String sql = "SELECT berichtText FROM berichte WHERE ueberschrift = ? and vereinnr = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, ueberschrift);
			pstmt.setString(2, vereinnr);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				if (rs.getString("berichtText") == null) {
					return "unbekannt";
				}
				return rs.getString("berichtText");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";
	}

	public List<Spiel> listeFreieBerichte(String vereinnr) {
		String sql = "SELECT ergebnisLink, ueberschrift FROM berichte WHERE vereinnr = ? and substr(ergebnisLink , 1 , 4) <> 'http' and ergebnisLink <> '-'";
		List<Spiel> spiele = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String ergebnisLink = rs.getString("ergebnisLink");
				String ueberschrift = rs.getString("ueberschrift");
				String datum = ergebnisLink.length() >= 10 ? ergebnisLink.substring(0, 10) : "";
				String heim = ueberschrift;
				TischtennisSpiel spiel = new TischtennisSpiel(vereinnr, "", "", datum, "", "", heim, "", "",
						ergebnisLink);
				spiele.add(spiel);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spiele;
	}

	public List<Spiel> listeBerichteMitSpielMetadaten(String vereinnr) {
		String sql = "SELECT ergebnisLink, ueberschrift, liga, heim, gast, datum, ergebnis, mitSpielberichte FROM berichte "
				+ "WHERE vereinnr = ? AND ergebnisLink <> '-'";
		List<Spiel> spiele = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String ergebnisLink = rs.getString("ergebnisLink");
				if (ergebnisLink == null || ergebnisLink.isBlank()) {
					continue;
				}

				if (ergebnisLink.startsWith("http")) {
					String liga = rs.getString("liga");
					String heim = rs.getString("heim");
					String gast = rs.getString("gast");
					String datum = rs.getString("datum");
					String ergebnis = rs.getString("ergebnis");

					if (istLeer(liga) || istLeer(heim) || istLeer(gast) || istLeer(datum) || istLeer(ergebnis)) {
						continue;
					}

					TischtennisSpiel spiel = new TischtennisSpiel(vereinnr, "", "", datum, "", liga, heim, gast,
							ergebnis, ergebnisLink);
					spiel.setMitSpielberichte(rs.getBoolean("mitSpielberichte"));					
					spiele.add(spiel);
				} else {
					String ueberschrift = rs.getString("ueberschrift");
					String datum = ergebnisLink.length() >= 10 ? ergebnisLink.substring(0, 10) : "";
					TischtennisSpiel spiel = new TischtennisSpiel(vereinnr, "", "", datum, "", "", ueberschrift, "", "",
							ergebnisLink);
					spiel.setMitSpielberichte(rs.getBoolean("mitSpielberichte"));
					spiele.add(spiel);
				}
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spiele;
	}

	private boolean istLeer(String text) {
		return text == null || text.isBlank();
	}

	public List<Spiel> listeSpielberichteInFreieBerichte(String vereinnr) {
		String sql = "SELECT ergebnisLink, ueberschrift FROM berichte WHERE vereinnr = ? AND mitSpielberichte = ?";
		List<Spiel> spiele = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setBoolean(2, true);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String ergebnisLink = rs.getString("ergebnisLink");
				String ueberschrift = rs.getString("ueberschrift");
				String heim = ueberschrift;
				String datum = ergebnisLink.length() >= 10 ? ergebnisLink.substring(0, 10) : "31.12.2999";
				TischtennisSpiel spiel = new TischtennisSpiel(vereinnr, "", "", datum, "", "", heim, "", "",
						ergebnisLink);
				spiele.add(spiel);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spiele;
	}

	public List<BilderEintrag> listeBilder(String vereinnr) {
		String sql = "SELECT ergebnisLink, ueberschrift, bildUnterschrift FROM berichte "
				+ "WHERE vereinnr = ? AND bild IS NOT NULL AND LENGTH(bild) > 0";
		List<BilderEintrag> bilder = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				BilderEintrag eintrag = new BilderEintrag();
				eintrag.setErgebnisLink(rs.getString("ergebnisLink"));
				eintrag.setUeberschrift(rs.getString("ueberschrift"));
				eintrag.setBildUnterschrift(rs.getString("bildUnterschrift"));
				bilder.add(eintrag);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return bilder;
	}

	public void deleteSpiel(String vereinnr, String ergebnisLink) {
		String sql = "DELETE FROM spielplan WHERE vereinnr = ? AND ergebnisLink = ?";

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);

			int affectedRows = pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<String> listeWirkung(String vereinnr, String gewonnen) {
		String sql = "";
		if ("alle".equals(gewonnen)) {
			sql = "SELECT wirkung FROM stilelemente";
		} else {
			sql = "SELECT wirkung FROM stilelemente WHERE ergebnis = ? or ergebnis = 'immer'";
		}
		List<String> wirkungListe = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			if (!"alle".equals(gewonnen)) {
				pstmt.setString(1, gewonnen);
			}
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				wirkungListe.add(rs.getString("wirkung"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return wirkungListe;
	}

	public Stil leseWirkung(String vereinnr, String wirkung) {
		String sql = "SELECT sprachebene,stimmung,tonfall,humor,wertung,struktur FROM stilelemente Where wirkung = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, wirkung);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				return new Stil(rs.getString("sprachebene"), rs.getString("stimmung"), rs.getString("tonfall"),
						rs.getString("humor"), rs.getString("wertung"), rs.getString("struktur"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String leseWirkungBeispiel(String vereinnr, String wirkung) {
		String sql = "SELECT vorschautext FROM stilelemente Where wirkung = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, wirkung);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				return rs.getString("vorschautext");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";
	}

	// Liest den gespeicherten Berichtstext und das Bild (als BLOB) aus der
	// Datenbank
	public int anzahlKI(String vereinnr, String ergebnisLink, String art) {
		String sql = "SELECT count(*) as Anzahl FROM log_tabelle WHERE vereinnr = ? and ergebnisLink = ? and aktion = \"KI-Bericht "
				+ art + "\"";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("Anzahl");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	// Liest den gespeicherten Berichtstext und das Bild (als BLOB) aus der
	// Datenbank
	public boolean isKI(String vereinnr, String ergebnisLink) {
		String sql = "SELECT count(*) as Anzahl FROM log_tabelle WHERE vereinnr = ? and ergebnisLink = ? and name = 'KI'";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				if (rs.getInt("Anzahl") == 0) {
					return false;
				}

				return true;
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Liest den gespeicherten Berichtstext und das Bild (als BLOB) aus der
	// Datenbank
	public int anzahlWordpress(String vereinnr, String ergebnisLink, String name) {
		String sql = "SELECT count(*) as Anzahl FROM log_tabelle WHERE vereinnr = ? and ergebnisLink = ? AND aktion LIKE CONCAT('Veröffentlichen ', ?, ' OK%')";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setString(3, name);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("Anzahl");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	// Liest den gespeicherten Berichtstext und das Bild (als BLOB) aus der
	// Datenbank
	public String anomisiereVorname(String vereinnr, String name) {
		String sql = "(" + "  SELECT v2.Name " + "  FROM Vorname v1 "
				+ "  JOIN Vorname v2 ON v2.Geschlecht = v1.Geschlecht AND v2.Name != v1.Name " + "  WHERE v1.Name = ? "
				+ "  ORDER BY RAND() " + "  LIMIT 1 " + ") " + "UNION " + "(" + "  SELECT v3.Name "
				+ "  FROM Vorname v3 " + "  ORDER BY RAND() " + "  LIMIT 1 " + ") " + "LIMIT 1;";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, name);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("Name");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}

	// Liest den gespeicherten Berichtstext und das Bild (als BLOB) aus der
	// Datenbank
	public String anomisiereNachname(String vereinnr, String name) {
		String sql = "  SELECT Name " + "  FROM Nachname " + "  ORDER BY RAND() " + "  LIMIT 1 " + ";";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("Name");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}

	public int anzahlFreigabe(String vereinnr, String ergebnisLink) {
		String sql = "SELECT count(*) as Anzahl FROM log_tabelle WHERE vereinnr = ? and ergebnisLink = ? AND aktion = 'Freigegeben'";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("Anzahl");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int anzahlBlaettle(String vereinnr, String ergebnisLink) {
		String sql = "SELECT count(*) as Anzahl FROM log_tabelle WHERE vereinnr = ? and  ergebnisLink = ? AND aktion = 'Veröffentlichung Blättle'";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("Anzahl");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	// Liest den gespeicherten Berichtstext und das Bild (als BLOB) aus der
	// Datenbank
	public int eMailVersand(String vereinnr, String ergebnisLink) {
		String sql = "SELECT count(*) as anzahlMail FROM log_tabelle WHERE vereinnr = ? and ergebnisLink = ? and aktion like 'Email%' and mailErfolgreich = 'J'";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("anzahlMail");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public String leseConfig(String vereinnr, String eintrag) {
		String sql = "SELECT wert FROM config WHERE vereinnr = ? and eintrag = ? Limit 1";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, eintrag);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("wert");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "N";
	}

	/**
	 * Überprüft, ob sich ein bestimmter ErgebnisLink in Bearbeitung befindet. "In
	 * Bearbeitung" bedeutet, dass es in der letzten Minute genau einen Eintrag mit
	 * diesem ErgebnisLink gab (d.h., nur eine eindeutige UUID).
	 *
	 * @param ergebnisLink Der zu überprüfende Ergebnis-Link.
	 * @return true, wenn sich der ErgebnisLink in Bearbeitung befindet, false
	 *         sonst.
	 */
	public boolean inBearbeitung(String vereinnr, String ergebnisLink, String uuid) {
		String tableName = "doppel";
		String sql = "SELECT COUNT(DISTINCT uuid) FROM " + tableName
				+ " WHERE vereinnr = ? and ergebnisLink = ? AND timestamp >= ? AND uuid <> ?";
		LocalDateTime vorEinerMinute = LocalDateTime.now().minus(10, ChronoUnit.SECONDS);
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setTimestamp(3, Timestamp.valueOf(vorEinerMinute));
			pstmt.setString(4, uuid);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				int anzahlUuids = rs.getInt(1);
				return anzahlUuids == 1;
			}
		} catch (SQLException e) {
			System.err.println("Fehler beim Überprüfen des Bearbeitungsstatus: " + e.getMessage());
		}
		return false; // Im Fehlerfall oder wenn kein Ergebnis gefunden wird, false zurückgeben
	}

	// Nur Text speichern (überschreibt Text, lässt Bild und Unterschrift
	// unverändert)
	public void saveBerichtData(String vereinnr, String ergebnisLink, String berichtText, String ueberschift) {
		saveOrUpdateBericht(vereinnr, ergebnisLink, berichtText, true, null, false, null, false, ueberschift, true,
				true);
	}

	// Nur Text speichern (überschreibt Text, lässt Bild und Unterschrift
	// unverändert)
	public void saveBerichtData(String vereinnr, String ergebnisLink, String berichtText) {
		saveOrUpdateBericht(vereinnr, ergebnisLink, berichtText, true, null, false, null, false, null, false, true);
	}

	// Nur Bild speichern (überschreibt Bild, lässt Text und Unterschrift
	// unverändert)
	public void saveBerichtData(String vereinnr, String ergebnisLink, byte[] bild) {
		saveOrUpdateBericht(vereinnr, ergebnisLink, null, false, bild, true, null, false, null, false, true);
	}

	// Text + Bild + Unterschrift speichern (überschreibt alles)
	public void saveBerichtData(String vereinnr, String ergebnisLink, String berichtText, byte[] bild,
			String bildUnterschrift, String ueberschrift) {
		saveOrUpdateBericht(vereinnr, ergebnisLink, berichtText, true, bild, true, bildUnterschrift, true, ueberschrift,
				true, true);
	}

	// Text + Bild + Unterschrift speichern (überschreibt alles)
	public void saveBerichtDataOhneHist(String vereinnr, String ergebnisLink, String berichtText, byte[] bild,
			String bildUnterschrift, String ueberschrift) {
		saveOrUpdateBericht(vereinnr, ergebnisLink, berichtText, true, bild, true, bildUnterschrift, true, ueberschrift,
				true, false);
	}

	public void saveOrUpdateBericht(String vereinnr, String ergebnisLink, String neuerText, boolean berichtTextSetzen,
			byte[] neuesBild, boolean bildSetzen, String neueUnterschrift, boolean unterschriftSetzen,
			String neueUeberschrift, boolean ueberschriftSetzen, boolean speichernHistorie) {

		String selectSql = "SELECT berichtText, bild, bildUnterschrift, ueberschrift, liga, heim, gast, datum, ergebnis FROM berichte WHERE vereinnr = ? and ergebnisLink = ?";
		String insertHistorieSql = "INSERT INTO berichte_historie (vereinnr, ergebnisLink, berichtText, bild, bildUnterschrift, ueberschrift, liga, heim, gast, datum, ergebnis, timestamp) VALUES (? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
		String upsertSql = "INSERT INTO berichte (vereinnr, ergebnisLink, berichtText, bild, bildUnterschrift , ueberschrift) VALUES (? , ?, ?, ?, ? , ?) "
				+ "ON DUPLICATE KEY UPDATE berichtText = VALUES(berichtText), bild = VALUES(bild), bildUnterschrift = VALUES(bildUnterschrift) , ueberschrift = VALUES(ueberschrift)";

		try (Connection conn = openConnection()) {

			if (speichernHistorie) {

				String alterText = null;
				byte[] altesBild = null;
				String alteUnterschrift = null;
				String alteUeberschrift = null;
				String alteLiga = null;
				String alteHeim = null;
				String alteGast = null;
				String altesDatum = null;
				String altesErgebnis = null;

				boolean existiert = false;
				boolean hatSichGeaendert = false;

				try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
					selectStmt.setString(1, vereinnr);
					selectStmt.setString(2, ergebnisLink);
					try (ResultSet rs = selectStmt.executeQuery()) {
						if (rs.next()) {
							existiert = true;
							alterText = rs.getString("berichtText");
							altesBild = rs.getBytes("bild");
							alteUnterschrift = rs.getString("bildUnterschrift");
							alteUeberschrift = rs.getString("ueberschrift");
							alteLiga = rs.getString("liga");
							alteHeim = rs.getString("heim");
							alteGast = rs.getString("gast");
							altesDatum = rs.getString("datum");
							altesErgebnis = rs.getString("ergebnis");

						}
					}
				}

				// Fallback: Falls Datensatz nicht existiert, sind alte Werte null (wie
				// initialisiert)
				// Entscheide jetzt, was die neuen finalen Werte sind (entweder alte oder neue,
				// je nach Flag)
				String finalText = berichtTextSetzen ? neuerText : alterText;
				byte[] finalBild = bildSetzen ? neuesBild : altesBild;
				String finalUnterschrift = unterschriftSetzen ? neueUnterschrift : alteUnterschrift;
				String finalUeberschrift = ueberschriftSetzen ? neueUeberschrift : alteUeberschrift;

				// Prüfen, ob sich irgendwas geändert hat (auch wenn neuer Datensatz: true)
				if (!existiert || !Objects.equals(alterText, finalText) || !Arrays.equals(altesBild, finalBild)
						|| !Objects.equals(alteUnterschrift, finalUnterschrift)
						|| !Objects.equals(alteUeberschrift, finalUeberschrift)) {
					hatSichGeaendert = true;
				}

				if (hatSichGeaendert) {
					// Historisieren falls Datensatz existiert
					if (existiert) {
						try (PreparedStatement insertHistStmt = conn.prepareStatement(insertHistorieSql)) {
							insertHistStmt.setString(1, vereinnr);
							insertHistStmt.setString(2, ergebnisLink);
							insertHistStmt.setString(3, alterText);
							insertHistStmt.setBytes(4, altesBild);
							insertHistStmt.setString(5, alteUnterschrift);
							insertHistStmt.setString(6, alteUeberschrift);
							insertHistStmt.setString(7, alteLiga);
							insertHistStmt.setString(8, alteHeim);
							insertHistStmt.setString(9, alteGast);
							insertHistStmt.setString(10, altesDatum);
							insertHistStmt.setString(11, altesErgebnis);

							insertHistStmt.executeUpdate();
						}
					}

					// Update / Insert mit finalen Werten (kann auch null sein)
					try (PreparedStatement upsertStmt = conn.prepareStatement(upsertSql)) {
						upsertStmt.setString(1, vereinnr);
						upsertStmt.setString(2, ergebnisLink);
						if (finalText != null) {
							upsertStmt.setString(3, finalText);
						} else {
							upsertStmt.setNull(3, java.sql.Types.VARCHAR);
						}
						if (finalBild != null) {
							upsertStmt.setBytes(4, finalBild);
						} else {
							upsertStmt.setNull(4, java.sql.Types.BLOB);
						}
						if (finalUnterschrift != null) {
							upsertStmt.setString(5, finalUnterschrift);
						} else {
							upsertStmt.setNull(5, java.sql.Types.VARCHAR);
						}
						if (finalUeberschrift != null) {
							upsertStmt.setString(6, finalUeberschrift);
						} else {
							upsertStmt.setNull(6, java.sql.Types.VARCHAR);
						}
						upsertStmt.executeUpdate();
					}
				}
			} else {
				// Update / Insert mit finalen Werten (kann auch null sein)
				PreparedStatement upsertStmt = conn.prepareStatement(upsertSql);
				upsertStmt.setString(1, vereinnr);
				upsertStmt.setString(2, ergebnisLink);
				upsertStmt.setString(3, neuerText);
				upsertStmt.setBytes(4, neuesBild);
				upsertStmt.setString(5, neueUnterschrift);
				upsertStmt.setString(6, neueUeberschrift);
				upsertStmt.executeUpdate();
			}
			BerichtHelper.refreshCachedBerichtData(vereinnr, ergebnisLink);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void saveSpielMetadaten(String vereinnr, String ergebnisLink, String liga, String heim, String gast,
			String datum, String ergebnis) {
		String upsertSql = "INSERT INTO berichte (vereinnr, ergebnisLink, liga, heim, gast, datum, ergebnis) VALUES (?, ?, ?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE liga = VALUES(liga), heim = VALUES(heim), gast = VALUES(gast), datum = VALUES(datum), ergebnis = VALUES(ergebnis)";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setString(3, liga);
			pstmt.setString(4, heim);
			pstmt.setString(5, gast);
			pstmt.setString(6, datum);
			pstmt.setString(7, ergebnis);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void saveLogData(String vereinnr, String ergebnisLink, String name, String aktion, String mailErfolgreich) {
		String sql = "INSERT INTO log_tabelle (vereinnr, ergebnisLink, timestamp, name, aktion, mailErfolgreich) VALUES (?, ?, ?, ?, ?, ?)";

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			// Aktuelle Zeit in deutscher Zeitzone (inkl. Sommer-/Winterzeit)
			ZonedDateTime berlinTime = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
			Timestamp timestamp = Timestamp.from(berlinTime.toInstant());

			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setTimestamp(3, timestamp);
			pstmt.setString(4, name);
			pstmt.setString(5, aktion);
			pstmt.setString(6, mailErfolgreich);
			pstmt.executeUpdate();

			ErgebnisCache.setze(vereinnr, "Freigabe", this, ergebnisLink, name);
			ErgebnisCache.setze(vereinnr, "Blaettle", this, ergebnisLink, name);
			ErgebnisCache.setze(vereinnr, "Wordpress", this, ergebnisLink, name);
			BerichtHelper.refreshCachedBerichtData(vereinnr, ergebnisLink);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void saveLogData(String vereinnr, String ergebnisLink, String name, String aktion, String mailErfolgreich,
			String info) {
		String sql = "INSERT INTO log_tabelle (vereinnr, ergebnisLink, timestamp, name, aktion, mailErfolgreich, info) VALUES (?, ?, ?, ?, ?, ? ,?)";

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			// Aktuelle Zeit in deutscher Zeitzone (inkl. Sommer-/Winterzeit)
			ZonedDateTime berlinTime = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
			Timestamp timestamp = Timestamp.from(berlinTime.toInstant());

			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setTimestamp(3, timestamp);
			pstmt.setString(4, name);
			pstmt.setString(5, aktion);
			pstmt.setString(6, mailErfolgreich);
			pstmt.setString(7, info);
			pstmt.executeUpdate();

			ErgebnisCache.setze(vereinnr, "Freigabe", this, ergebnisLink, name);
			ErgebnisCache.setze(vereinnr, "Blaettle", this, ergebnisLink, name);
			ErgebnisCache.setze(vereinnr, "Wordpress", this, ergebnisLink, name);
			BerichtHelper.refreshCachedBerichtData(vereinnr, ergebnisLink);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void deleteLogData(String vereinnr, String ergebnisLink, String aktion) {
		String sql = "Delete FROM log_tabelle where vereinnr = ? and ergebnisLink = ? and aktion= ? ";

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setString(3, aktion);
			pstmt.executeUpdate();
			ErgebnisCache.setze(vereinnr, "Freigabe", this, ergebnisLink, "");
			ErgebnisCache.setze(vereinnr, "Blaettle", this, ergebnisLink, "");
			ErgebnisCache.setze(vereinnr, "Freigabe", this, ergebnisLink, "");
			BerichtHelper.refreshCachedBerichtData(vereinnr, ergebnisLink);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void deleteBericht(String vereinnr, String ergebnisLink) {
		String sql = "Delete FROM berichte where vereinnr = ? and ergebnisLink = ? ";

		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void updateMitSpielberichte(String vereinnr, String ergebnisLink, boolean mitSpielberichte) {
		String sql = "UPDATE berichte SET mitSpielberichte = ? WHERE vereinnr = ? AND ergebnisLink = ?";

		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setBoolean(1, mitSpielberichte);
			pstmt.setString(2, vereinnr);
			pstmt.setString(3, ergebnisLink);

			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<LogEntry> getLogEntries(String vereinnr, String ergebnisLink) {
		List<LogEntry> logEntries = new ArrayList<>();
		String sql = "SELECT ergebnislink, name, aktion, mailErfolgreich, timestamp, info "
				+ "FROM log_tabelle WHERE vereinnr = ? AND ergebnislink = ? ORDER BY timestamp DESC";

		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);

			// Kalender für deutsche Zeitzone
			Calendar berlinCal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					LogEntry entry = new LogEntry();
					entry.setErgebnisLink(rs.getString("ergebnislink"));
					entry.setName(rs.getString("name"));
					entry.setMail(rs.getString("aktion"));
					entry.setMailErfolgreich(rs.getString("mailErfolgreich"));
					entry.setInfo(rs.getString("info"));
					// Hier die Zeitzone korrekt anwenden:
					entry.setTimestamp(rs.getTimestamp("timestamp", berlinCal));
					logEntries.add(entry);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return logEntries;
	}

	public void verarbeiteEintrag(String vereinnr, String ergebnisLink, String uuid) {
		if (eintragExistiert(vereinnr, ergebnisLink, uuid)) {
			aktualisiereTimestamp(vereinnr, ergebnisLink, uuid);
		} else {
			fuegeNeuenEintragHinzu(vereinnr, ergebnisLink, uuid);
		}
	}

	private boolean eintragExistiert(String vereinnr, String ergebnisLink, String uuid) {
		String tableName = "doppel";
		String sql = "SELECT 1 FROM " + tableName + " WHERE vereinnr = ? and ergebnisLink = ? AND uuid = ?";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setString(3, uuid);
			ResultSet rs = pstmt.executeQuery();
			return rs.next(); // Gibt true zurück, wenn mindestens ein Datensatz gefunden wurde
		} catch (SQLException e) {
			System.err.println("Fehler beim Überprüfen des Eintrags: " + e.getMessage());
			return false;
		}
	}

	private void fuegeNeuenEintragHinzu(String vereinnr, String ergebnisLink, String uuid) {
		String tableName = "doppel";
		String sql = "INSERT INTO " + tableName + " (vereinnr, ergebnisLink, uuid, timestamp) VALUES (?, ?, ? , ?)";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setString(3, uuid);
			pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Fehler beim Einfügen des neuen Eintrags: " + e.getMessage());
		}
	}

	private void aktualisiereTimestamp(String vereinnr, String ergebnisLink, String uuid) {
		String tableName = "doppel";
		String sql = "UPDATE " + tableName + " SET timestamp = ? WHERE vereinnr = ? and ergebnisLink = ? AND uuid = ?";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
			pstmt.setString(2, vereinnr);
			pstmt.setString(3, ergebnisLink);
			pstmt.setString(4, uuid);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Fehler beim Aktualisieren des Timestamps: " + e.getMessage());
		}
	}

	public boolean deleteUUID(String vereinnr, String ergebnisLink, String uuid) {
		String tableName = "doppel";
		String sql = "DELETE FROM " + tableName + " WHERE vereinnr = ? AND ergebnisLink = ? AND uuid = ?";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			pstmt.setString(3, uuid);
			int rowsAffected = pstmt.executeUpdate();
			return rowsAffected > 0; // Gibt true zurück, wenn mindestens eine Zeile gelöscht wurde
		} catch (SQLException e) {
			System.err.println("Fehler beim Löschen des Eintrags: " + e.getMessage());
			// Hier könnte man noch Logging oder eine andere Fehlerbehandlung hinzufügen
			return false;
		}
	}

	public int getHomepageEntries(String vereinnr, String ergebnisLink, String homepage) {
		String sql = "SELECT aktion FROM log_tabelle WHERE vereinnr = ? and ergebnisLink = ? and aktion like 'Veröffentlichen "
				+ homepage + " OK (PostId=%' ORDER BY timestamp  DESC LIMIT 1 ";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, ergebnisLink);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					return extractPostId(vereinnr, rs.getString("aktion"));
				}
			}
		} catch (SQLException e) {
			return -1;
		}
		return -1;
	}

	private int extractPostId(String vereinnr, String aktion) {
		if (aktion == null || !aktion.contains("PostId=")) {
			return -1;
		}
		try {
			String temp = aktion.substring(aktion.indexOf("PostId=") + 7); // "PostId=" hat 7 Zeichen
			temp = temp.split("\\)")[0]; // Entfernt alles nach der schließenden Klammer
			return Integer.parseInt(temp.trim()); // Konvertiert in Integer
		} catch (NumberFormatException e) {
			return -1; // Falls keine valide Zahl gefunden wurde
		}
	}

	public List<String> getHistorieTimestamps(String vereinnr, String ergebnisLink) {
		List<String> timestamps = new ArrayList<>();
		String sql = "SELECT timestamp FROM berichte_historie WHERE vereinnr = ? and ergebnisLink = ? ORDER BY timestamp DESC";

		try (Connection conn = openConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, vereinnr);
			stmt.setString(2, ergebnisLink);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					timestamps.add(rs.getTimestamp("timestamp").toString());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return timestamps;
	}

	public BerichtData getBerichtDataFromHistorie(String vereinnr, String ergebnisLink, String timestamp) {
		String sql = "SELECT berichtText, bild, bildUnterschrift, ueberschrift FROM berichte_historie WHERE vereinnr = ? and ergebnisLink = ? AND timestamp = ?";
		BerichtData data = new BerichtData();
		try (Connection conn = openConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, vereinnr);
			stmt.setString(2, ergebnisLink);
			stmt.setTimestamp(3, Timestamp.valueOf(timestamp)); // timestamp im Format "yyyy-MM-dd HH:mm:ss"

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					data.setBerichtText(rs.getString("berichtText"));
					data.setBild(rs.getBytes("bild"));
					data.setBildUnterschrift(rs.getString("bildUnterschrift"));
					data.setUeberschrift(rs.getString("ueberschrift"));
					return data;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null; // nichts gefunden
	}

	public List<String> ladeAlleVereine() {
		String sql = "SELECT DISTINCT vereinnr FROM config ORDER BY vereinnr";
		List<String> vereinsListe = new ArrayList<>();
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				vereinsListe.add(rs.getString("vereinnr"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return vereinsListe;
	}

	public List<ConfigEintrag> ladeConfigEintraege(String vereinnr) {
		String sql = "SELECT eintrag, wert FROM config WHERE vereinnr = ? ORDER BY eintrag";
		List<ConfigEintrag> eintraege = new ArrayList<>();

		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, vereinnr);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String eintrag = rs.getString("eintrag");
				String wert = rs.getString("wert");
				eintraege.add(new ConfigEintrag(vereinnr, eintrag, wert));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return eintraege;
	}
	public Map<String, ConfigBedeutung> ladeConfigBedeutungen() {
		String sql = "SELECT config_eintrag, bedeutung, inhaltformat, wertebereich FROM config_Bedeutung";
		Map<String, ConfigBedeutung> bedeutungen = new HashMap<>();

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String configEintrag = rs.getString("config_eintrag");
				ConfigBedeutung bedeutung = new ConfigBedeutung(configEintrag, rs.getString("bedeutung"),
						rs.getString("inhaltformat"), rs.getString("wertebereich"));
				bedeutungen.put(configEintrag, bedeutung);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return bedeutungen;
	}

	public List<ConfigKategorie> ladeConfigKategorien() {
		String sql = "SELECT config_eintrag, kategorie FROM config_Kategorie ORDER BY config_eintrag, kategorie";
		List<ConfigKategorie> kategorien = new ArrayList<>();

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				kategorien.add(new ConfigKategorie(rs.getString("config_eintrag"), rs.getString("kategorie")));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return kategorien;
	}

	public void speichereConfigEintraege(String vereinnr, List<ConfigEintrag> eintraege) {
		String sql = "UPDATE config SET wert = ? WHERE vereinnr = ? AND eintrag = ?";

		try (Connection conn = openConnection();

		) {

			for (ConfigEintrag eintrag : eintraege) {
				try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
					pstmt.setString(1, eintrag.getWert());
					pstmt.setString(2, vereinnr);
					pstmt.setString(3, eintrag.getEintrag());
					pstmt.executeUpdate();
				}
			}

			ConfigManager.clearCache(vereinnr);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insertConfigEintrag(String vereinnr, String eintrag, String wert) {
		String sql = "INSERT IGNORE INTO config (vereinnr, eintrag, wert) VALUES (?, ?, ?)";

		try (Connection conn = openConnection();

		) {

			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, vereinnr);
				pstmt.setString(2, eintrag);
				pstmt.setString(3, wert);
				pstmt.executeUpdate();
			}

			ConfigManager.clearCache(vereinnr);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insertOrUpdateConfigEintrag(String vereinnr, String eintrag, String wert) {
		String sql = "INSERT INTO config (vereinnr, eintrag, wert) " + "VALUES (?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE wert = VALUES(wert)";

		try (Connection conn = openConnection();

		) {

			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, vereinnr);
				pstmt.setString(2, eintrag);
				pstmt.setString(3, wert);
				pstmt.executeUpdate();
			}

			ConfigManager.clearCache(vereinnr);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void deleteConfigEintrag(String vereinnr, String eintrag) {
		String sql = "Delete from config where vereinnr = ? and eintrag = ?";

		try (Connection conn = openConnection();

		) {

			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, vereinnr);
				pstmt.setString(2, eintrag);
				pstmt.executeUpdate();
			}

			ConfigManager.clearCache(vereinnr);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String bestimmenVereinnr(String ort) {
		String sql = "SELECT vereinnr FROM config WHERE eintrag = 'login.Ort' AND LOWER(wert) = LOWER(?)";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, ort);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				return rs.getString("vereinnr");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void kopierenLogData(String ergebnisLink, String altLink, String was) {

		String sql = "INSERT INTO TischtennisBericht.log_tabelle ("
				+ "    ergebnisLink, timestamp, name, aktion, mailErfolgreich, vereinnr" + ") " + "SELECT " + "    ?, "
				+ "    NOW(6) + INTERVAL (@i := @i + 1) MICROSECOND, " + "    name, " + "    aktion, "
				+ "    mailErfolgreich, " + "    vereinnr " + "FROM TischtennisBericht.log_tabelle "
				+ "JOIN (SELECT @i := 0) init " + "WHERE ergebnisLink = ? " + "  AND aktion LIKE ?";

		try (Connection conn = openConnection();

		) {

			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

				// Neuer / Ziel-Link
				pstmt.setString(1, altLink);

				// Alter / Quell-Link
				pstmt.setString(2, ergebnisLink);

				// Aktion-Filter
				pstmt.setString(3, was + "%");

				pstmt.executeUpdate();
			}

			// Cache löschen wenn notwendig
			ConfigManager.clearCache("GLOBAL");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void upsertConfigBedeutung(String configEintrag, String bedeutung, String inhaltformat, String wertebereich) {
		String sql = "INSERT INTO config_Bedeutung (config_eintrag, bedeutung, inhaltformat, wertebereich) "
				+ "VALUES (?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE bedeutung = VALUES(bedeutung), inhaltformat = VALUES(inhaltformat), wertebereich = VALUES(wertebereich)";

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, configEintrag);
			pstmt.setString(2, bedeutung);
			pstmt.setString(3, inhaltformat);
			pstmt.setString(4, wertebereich);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void replaceConfigKategorien(String configEintrag, List<String> kategorien) {
		String deleteSql = "DELETE FROM config_Kategorie WHERE config_eintrag = ?";
		String insertSql = "INSERT INTO config_Kategorie (config_eintrag, kategorie) VALUES (?, ?)";

		try (Connection conn = openConnection()) {
			try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
				deleteStmt.setString(1, configEintrag);
				deleteStmt.executeUpdate();
			}

			if (kategorien == null) {
				return;
			}

			try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
				for (String kategorie : kategorien) {
					if (kategorie == null || kategorie.isBlank()) {
						continue;
					}
					insertStmt.setString(1, configEintrag);
					insertStmt.setString(2, kategorie.trim());
					insertStmt.addBatch();
				}
				insertStmt.executeBatch();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	
	public void saveOrUpdateSpielstatistik(String vereinnr, String ergebnisLink, String spielstatistik) {

		String selectSql = "SELECT spielstatistik FROM berichte WHERE vereinnr = ? AND ergebnisLink = ?";
		String insertSql = "INSERT INTO berichte (vereinnr, ergebnisLink, spielstatistik, berichtText, bild, bildUnterschrift, ueberschrift) "
				+ "VALUES (?, ?, ?, NULL, NULL, NULL, NULL)";
		String updateSql = "UPDATE berichte SET spielstatistik = ? WHERE vereinnr = ? AND ergebnisLink = ?";

		try (Connection conn = openConnection();

		) {

			boolean existiert = false;

			// Prüfen, ob Datensatz existiert
			try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
				selectStmt.setString(1, vereinnr);
				selectStmt.setString(2, ergebnisLink);
				try (ResultSet rs = selectStmt.executeQuery()) {
					if (rs.next()) {
						existiert = true;
					}
				}
			}

			if (existiert) {
				// UPDATE nur Spielstatistik
				try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
					updateStmt.setString(1, spielstatistik);
					updateStmt.setString(2, vereinnr);
					updateStmt.setString(3, ergebnisLink);
					updateStmt.executeUpdate();
				}
			} else {
				// INSERT Satz anlegen → andere Felder NULL
				try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
					insertStmt.setString(1, vereinnr);
					insertStmt.setString(2, ergebnisLink);
					insertStmt.setString(3, spielstatistik);
					insertStmt.executeUpdate();
				}
			}

			BerichtHelper.refreshCachedBerichtData(vereinnr, ergebnisLink);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String loadSpielstatistik(String vereinnr, String ergebnisLink) {

		String sql = "SELECT spielstatistik FROM berichte WHERE vereinnr = ? AND ergebnisLink = ?";

		try (Connection conn = openConnection();

				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, vereinnr);
			stmt.setString(2, ergebnisLink);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					String wert = rs.getString("spielstatistik");
					return (wert != null) ? wert : "Keine Spielstatistik vorhanden";
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return "Keine Spielstatistik vorhanden";
	}

	public List<String> listeUeberschriften(String vereinnr, int tage) {

		String sql = "SELECT b.ueberschrift " + "FROM berichte b " + "JOIN ( "
				+ "   SELECT ergebnislink, MAX(`timestamp`) AS last_ts " + "   FROM log_tabelle "
				+ "   WHERE vereinnr = ? " + "   GROUP BY ergebnislink " + ") x ON b.ergebnislink = x.ergebnislink "
				+ "WHERE x.last_ts >= NOW() - INTERVAL ? DAY " + "order by x.last_ts DESC";

		List<String> ueberschriften = new ArrayList<>();

		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, vereinnr);
			pstmt.setInt(2, tage);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				if (rs.getString("ueberschrift") != null) {
					ueberschriften.add(rs.getString("ueberschrift"));
				}
			}
			rs.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ueberschriften;
	}

	/**
	 * Lädt die Ziel-URL für einen Short-Link-Code aus der Datenbank.
	 *
	 * @param code der Kurzlink-Code, z.B. "abc123"
	 * @return die Ziel-URL inkl. Parameter, z.B. "bericht.xhtml?p1=..." oder null,
	 *         falls der Code nicht gefunden wurde
	 */
	public String loadTargetUrl(String code) {
		String sqlSelect = "SELECT target_url FROM short_links WHERE code = ?";
		String sqlUpdate = "UPDATE short_links SET click_count = click_count + 1, last_at = NOW() WHERE code = ?";

		try (Connection conn = openConnection();
				PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect);
				PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {

			pstmtSelect.setString(1, code);
			try (ResultSet rs = pstmtSelect.executeQuery()) {
				if (rs.next()) {
					String url = rs.getString("target_url");
					if (url != null) {
						// Klick-Zähler + letzter Zugriff aktualisieren
						pstmtUpdate.setString(1, code);
						pstmtUpdate.executeUpdate();

						return url;
					}
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null; // Code nicht gefunden
	}

	/**
	 * Generiert einen neuen Short-Link-Code und speichert die Ziel-URL, oder gibt
	 * den existierenden Code zurück, wenn die URL schon existiert.
	 *
	 * @param targetUrl die vollständige Ziel-URL, z.B. "bericht.xhtml?p1=..."
	 * @return der Kurzlink-Code, z.B. "a9f3c2d1"
	 */
	public String createShortLink(String targetUrl) {
		// 1️⃣ Prüfen, ob URL bereits existiert
		String existingCode = findCodeByTargetUrl(targetUrl);
		if (existingCode != null) {
			return existingCode; // schon vorhanden → zurückgeben
		}

		// 2️⃣ Neuer Code erzeugen
		String code = generateUniqueCode();

		// SQL mit zusätzlicher Spalte für Timestamp
		String sql = "INSERT INTO short_links (code, target_url, created_at) VALUES (?, ?, ?)";

		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, code);
			pstmt.setString(2, targetUrl);
			pstmt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis())); // Timestamp setzen

			pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
			return null; // Insert fehlgeschlagen
		}

		return code;
	}

	/**
	 * Prüft, ob die URL bereits existiert und gibt den zugehörigen Code zurück.
	 */
	private String findCodeByTargetUrl(String targetUrl) {
		String sql = "SELECT code FROM short_links WHERE target_url = ?";

		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, targetUrl);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return rs.getString("code");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null; // URL noch nicht vorhanden
	}

	/**
	 * Generiert einen kurzen, eindeutigen Code. Prüft bei Kollisionen erneut.
	 */
	private String generateUniqueCode() {
		String code;
		int maxTries = 5;

		for (int i = 0; i < maxTries; i++) {
			code = randomCode(8); // 8 Zeichen lang
			if (!exists(code)) {
				return code;
			}
		}

		// Wenn trotz mehrfacher Versuche kein eindeutiger Code gefunden wird,
		// UUID-Backup
		return java.util.UUID.randomUUID().toString().replace("-", "");
	}

	private boolean exists(String code) {
		String sql = "SELECT 1 FROM short_links WHERE code = ?";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, code);

			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return true; // Im Zweifel "existiert", um Kollision zu vermeiden
	}

	private String randomCode(int length) {
		final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}
		return sb.toString();
	}

	public List<Map<String, String>> ladeSpielcodesRohdaten(String vereinnr) {
		String sql = "SELECT t.liga, t.datum, t.zeit, t.heim, t.gast, s.spiel_code, s.pin "
				+ "FROM spielplan_tabelle t " + "LEFT JOIN spielcodes s ON s.unique_key = t.unique_key "
				+ "WHERE t.vereinnr = ?";
		List<Map<String, String>> rows = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Map<String, String> row = new HashMap<>();
					row.put("liga", rs.getString("liga"));
					row.put("datum", rs.getString("datum"));
					row.put("zeit", rs.getString("zeit"));
					row.put("heim", rs.getString("heim"));
					row.put("gast", rs.getString("gast"));
					row.put("spiel_code", rs.getString("spiel_code"));
					row.put("pin", rs.getString("pin"));
					rows.add(row);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	public List<TischtennisSpiel> ladeSpielplanAusTabelle(String vereinnr) {
		String sql = "SELECT wochentag_datum, wochentag, datum, zeit, liga, heim, gast, ergebnis, ergebnis_link FROM spielplan_tabelle WHERE vereinnr = ?";
		List<TischtennisSpiel> spiele = new ArrayList<>();
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		Pattern timePattern = Pattern.compile("^(\\d{1,2}:\\d{2})");
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String datum = rs.getString("datum");
					if (!isValidDate(datum, dateFormatter)) {
						continue;
					}
					spiele.add(
							new TischtennisSpiel(vereinnr, rs.getString("wochentag_datum"), rs.getString("wochentag"),
									datum, rs.getString("zeit"), rs.getString("liga"), rs.getString("heim"),
									rs.getString("gast"), rs.getString("ergebnis"), rs.getString("ergebnis_link")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		spiele.sort(Comparator.comparing((TischtennisSpiel spiel) -> LocalDate.parse(spiel.getDatum(), dateFormatter))
				.thenComparing(spiel -> parseTimePrefix(spiel.getZeit(), timePattern)));

		return spiele;
	}

	private boolean isValidDate(String value, DateTimeFormatter dateFormatter) {
		if (value == null || value.isBlank()) {
			return false;
		}
		try {
			LocalDate.parse(value.trim(), dateFormatter);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	private LocalTime parseTimePrefix(String zeit, Pattern timePattern) {
		if (zeit == null) {
			return LocalTime.MAX;
		}
		Matcher matcher = timePattern.matcher(zeit.trim());
		if (!matcher.find()) {
			return LocalTime.MAX;
		}
		try {
			return LocalTime.parse(matcher.group(1));
		} catch (DateTimeParseException e) {
			return LocalTime.MAX;
		}
	}

	public List<Map<String, String>> ladeVerfuegbarkeitSpiele(String vereinnr) {
		String sql = "SELECT datum, wochentag, zeit, liga, heim, gast FROM spielplan_tabelle WHERE vereinnr = ?";
		return ladeStringRows(sql, vereinnr);
	}

	public List<Map<String, String>> ladeAufstellungRows(String vereinnr) {
		String sql = "SELECT vereinnr, mannschaft, rang, qttr, name, a, status FROM aufstellung WHERE vereinnr = ?";
		return ladeStringRows(sql, vereinnr);
	}

	public List<Map<String, String>> ladeSpielerVerfuegbarkeitNachName(String vereinnr, String name) {
		String sql = "SELECT datum, uhrzeit, verfuegbarkeit, kommentar FROM spieler_verfuegbarkeit WHERE vereinnr = ? AND name = ?";
		return ladeStringRows(sql, vereinnr, name);
	}

	public void loescheSpielerVerfuegbarkeit(String vereinnr, String datum, String uhrzeit, String name) {
		String deleteSql = "DELETE FROM spieler_verfuegbarkeit WHERE vereinnr = ? AND datum = ? AND uhrzeit = ? AND name = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, datum);
			pstmt.setString(3, uhrzeit);
			pstmt.setString(4, name);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void speichereSpielerVerfuegbarkeit(String vereinnr, String datum, String uhrzeit, String name,
			String mannschaft, String verfuegbarkeit, String kommentar) {
		String sql = "INSERT INTO spieler_verfuegbarkeit (vereinnr, datum, uhrzeit, name, mannschaft, verfuegbarkeit, kommentar) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE mannschaft = VALUES(mannschaft), verfuegbarkeit = VALUES(verfuegbarkeit), kommentar = VALUES(kommentar)";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, datum);
			pstmt.setString(3, uhrzeit);
			pstmt.setString(4, name);
			pstmt.setString(5, mannschaft);
			pstmt.setString(6, verfuegbarkeit);
			pstmt.setString(7, kommentar);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<Map<String, String>> ladeAlleSpielerVerfuegbarkeiten(String vereinnr) {
		String sql = "SELECT datum, uhrzeit, name, mannschaft, verfuegbarkeit, kommentar FROM spieler_verfuegbarkeit WHERE vereinnr = ?";
		return ladeStringRows(sql, vereinnr);
	}

	public void speichereBetreuerOhneBestaetigung(String uniqueKey, String betreuer) {
		String upsertSql = "INSERT INTO spielplan_betreuer (unique_key, betreuer, bestaetigt, kommentar, mailTimestamp) "
				+ "VALUES (?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE " + "betreuer = VALUES(betreuer), "
				+ "bestaetigt = VALUES(bestaetigt), " + "kommentar = VALUES(kommentar), "
				+ "mailTimestamp = VALUES(mailTimestamp)";
		try (Connection conn = openConnection(); PreparedStatement upsertStmt = conn.prepareStatement(upsertSql)) {
			upsertStmt.setString(1, uniqueKey);
			upsertStmt.setString(2, betreuer);
			upsertStmt.setNull(3, java.sql.Types.BOOLEAN);
			upsertStmt.setNull(4, java.sql.Types.VARCHAR);
			upsertStmt.setNull(5, java.sql.Types.TIMESTAMP);
			upsertStmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<String> ladeBetreuerNamenAusAdressliste(String vereinnr) {
		List<String> namen = new ArrayList<>();
		String sql = "SELECT vorname, name FROM adressliste WHERE vereinnr = ? ORDER BY name, vorname";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String vorname = rs.getString("vorname");
					String nachname = rs.getString("name");
					String fullName = ((vorname == null ? "" : vorname.trim()) + " "
							+ (nachname == null ? "" : nachname.trim())).trim();
					if (!fullName.isBlank() && !namen.contains(fullName)) {
						namen.add(fullName);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		namen.sort(String::compareTo);
		return namen;
	}

	public List<Map<String, String>> ladeGesamtspielplanRows(String vereinnr, String vereinPrefix) {
		String sql = "SELECT t.unique_key, t.datum, t.wochentag, t.zeit, t.liga, t.heim, t.gast, t.ergebnis, b.betreuer, b.bestaetigt, b.kommentar "
				+ "FROM spielplan_tabelle t " + "LEFT JOIN spielplan_betreuer b ON b.unique_key = t.unique_key "
				+ "WHERE t.vereinnr = ? and ( t.heim LIKE ? OR t.gast LIKE ? )";
		return ladeStringRows(sql, vereinnr, vereinPrefix + "%", vereinPrefix + "%");
	}
	public List<GesamtspielplanConfigSpalte> ladeGesamtspielplanConfigSpalten(String vereinnr) {
		List<GesamtspielplanConfigSpalte> result = new ArrayList<>();
		String sql = "SELECT id, vereinnr, spalte, liga_anzeige, mannschaft_anzeige, betreuer FROM config_gesamtspielplan WHERE vereinnr = ? ORDER BY spalte ASC";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);
				) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				GesamtspielplanConfigSpalte spalte = new GesamtspielplanConfigSpalte();
				spalte.setId(rs.getInt("id"));
				spalte.setVereinnr(rs.getString("vereinnr"));
				spalte.setSpalte(rs.getInt("spalte"));
				spalte.setLigaAnzeige(rs.getString("liga_anzeige"));
				spalte.setMannschaftAnzeige(rs.getString("mannschaft_anzeige"));
				spalte.setBetreuer(rs.getBoolean("betreuer"));
				result.add(spalte);
			}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<GesamtspielplanConfigMannschaft> ladeGesamtspielplanConfigMannschaften(String vereinnr) {
		List<GesamtspielplanConfigMannschaft> result = new ArrayList<>();
		String sql = "SELECT id, vereinnr, id_spalte, liga, mannschaft FROM config_gesamtspielplan_mannschaft WHERE vereinnr = ? ORDER BY id_spalte ASC, id ASC";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);
				) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				GesamtspielplanConfigMannschaft mannschaft = new GesamtspielplanConfigMannschaft();
				mannschaft.setId(rs.getInt("id"));
				mannschaft.setVereinnr(rs.getString("vereinnr"));
				mannschaft.setIdSpalte(rs.getInt("id_spalte"));
				mannschaft.setLiga(rs.getString("liga"));
				mannschaft.setMannschaft(rs.getString("mannschaft"));
				result.add(mannschaft);
			}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<GesamtspielplanConfigRunde> ladeGesamtspielplanConfigRunden(String vereinnr) {
		List<GesamtspielplanConfigRunde> result = new ArrayList<>();
		String sql = "SELECT id, vereinnr, name, datum_von, datum_bis FROM config_gesamtspielplan_runde WHERE vereinnr = ? ORDER BY datum_von ASC, id ASC";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					GesamtspielplanConfigRunde runde = new GesamtspielplanConfigRunde();
					runde.setId(rs.getInt("id"));
					runde.setVereinnr(rs.getString("vereinnr"));
					runde.setName(rs.getString("name"));
					Date datumVon = rs.getDate("datum_von");
					Date datumBis = rs.getDate("datum_bis");
					runde.setDatumVon(datumVon == null ? null : datumVon.toLocalDate());
					runde.setDatumBis(datumBis == null ? null : datumBis.toLocalDate());
					result.add(runde);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void speichereGesamtspielplanKonfiguration(String vereinnr, List<GesamtspielplanConfigSpalte> spalten,
			List<GesamtspielplanConfigRunde> runden) {
		String deleteKinder = "DELETE FROM config_gesamtspielplan_mannschaft WHERE vereinnr = ?";
		String deleteSpalten = "DELETE FROM config_gesamtspielplan WHERE vereinnr = ?";
		String deleteRunden = "DELETE FROM config_gesamtspielplan_runde WHERE vereinnr = ?";		
		String insertSpalte = "INSERT INTO config_gesamtspielplan (vereinnr, spalte, liga_anzeige, mannschaft_anzeige, betreuer) VALUES (?, ?, ?, ?, ?)";
		String insertMannschaft = "INSERT INTO config_gesamtspielplan_mannschaft (vereinnr, id_spalte, liga, mannschaft) VALUES (?, ?, ?, ?)";
		String insertRunde = "INSERT INTO config_gesamtspielplan_runde (vereinnr, name, datum_von, datum_bis) VALUES (?, ?, ?, ?)";
		
		try (Connection conn = openConnection()) {
			conn.setAutoCommit(false);
			try (PreparedStatement deleteKinderStmt = conn.prepareStatement(deleteKinder);
					PreparedStatement deleteRundenStmt = conn.prepareStatement(deleteRunden);					
					PreparedStatement deleteSpaltenStmt = conn.prepareStatement(deleteSpalten);
					PreparedStatement insertSpalteStmt = conn.prepareStatement(insertSpalte, Statement.RETURN_GENERATED_KEYS);
					PreparedStatement insertMannschaftStmt = conn.prepareStatement(insertMannschaft);
					PreparedStatement insertRundeStmt = conn.prepareStatement(insertRunde)) {


				deleteKinderStmt.setString(1, vereinnr);
				deleteKinderStmt.executeUpdate();
				deleteSpaltenStmt.setString(1, vereinnr);
				deleteSpaltenStmt.executeUpdate();
				deleteRundenStmt.setString(1, vereinnr);
				deleteRundenStmt.executeUpdate();
				

				int index = 1;
				for (GesamtspielplanConfigSpalte spalte : spalten) {
						insertSpalteStmt.setString(1, vereinnr);
						insertSpalteStmt.setInt(2, index++);
						insertSpalteStmt.setString(3, spalte.getLigaAnzeige());
						insertSpalteStmt.setString(4, spalte.getMannschaftAnzeige());
						insertSpalteStmt.setBoolean(5, spalte.isBetreuer());
						insertSpalteStmt.executeUpdate();
					try (ResultSet keys = insertSpalteStmt.getGeneratedKeys()) {
						if (!keys.next()) {
							continue;
						}
						int idSpalte = keys.getInt(1);
						for (GesamtspielplanConfigMannschaft mannschaft : spalte.getMannschaften()) {
								insertMannschaftStmt.setString(1, vereinnr);
								insertMannschaftStmt.setInt(2, idSpalte);
								insertMannschaftStmt.setString(3, mannschaft.getLiga());
								insertMannschaftStmt.setString(4, mannschaft.getMannschaft());
							insertMannschaftStmt.addBatch();
						}
					}
				}
				insertMannschaftStmt.executeBatch();
				for (GesamtspielplanConfigRunde runde : runden) {
					String name = runde == null ? null : runde.getName();
					if (name == null || name.isBlank()) {
						continue;
					}
					insertRundeStmt.setString(1, vereinnr);
					insertRundeStmt.setString(2, name.trim());
					LocalDate datumVon = runde.getDatumVon();
					LocalDate datumBis = runde.getDatumBis();
					if (datumVon == null) {
						insertRundeStmt.setNull(3, java.sql.Types.DATE);
					} else {
						insertRundeStmt.setDate(3, Date.valueOf(datumVon));
					}
					if (datumBis == null) {
						insertRundeStmt.setNull(4, java.sql.Types.DATE);
					} else {
						insertRundeStmt.setDate(4, Date.valueOf(datumBis));
					}
					insertRundeStmt.addBatch();
				}
				insertRundeStmt.executeBatch();
				
				conn.commit();
			} catch (SQLException ex) {
				conn.rollback();
				throw ex;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void speichereGesamtspielplanKonfiguration(String vereinnr, List<GesamtspielplanConfigSpalte> spalten) {
		speichereGesamtspielplanKonfiguration(vereinnr, spalten, List.of());
	}

	public List<String> ladeGesamtspielplanLigen(String vereinnr, String vereinPrefix) {
		List<String> result = new ArrayList<>();
		String sql = "SELECT DISTINCT liga FROM spielplan_tabelle WHERE vereinnr = ? AND (heim LIKE ? OR gast LIKE ?) AND liga IS NOT NULL AND TRIM(liga) <> '' ORDER BY liga";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, vereinPrefix + "%");
			pstmt.setString(3, vereinPrefix + "%");
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String liga = rs.getString("liga");
					if (liga != null && !liga.isBlank()) {
						result.add(liga);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<String> ladeGesamtspielplanMannschaften(String vereinnr, String vereinPrefix) {
		List<String> result = new ArrayList<>();
		String sql = "SELECT heim AS team FROM spielplan_tabelle WHERE vereinnr = ? AND heim LIKE ? "
				+ "UNION SELECT gast AS team FROM spielplan_tabelle WHERE vereinnr = ? AND gast LIKE ? ORDER BY team";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, vereinPrefix + "%");
			pstmt.setString(3, vereinnr);
			pstmt.setString(4, vereinPrefix + "%");
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String team = rs.getString("team");
					if (team != null && !team.isBlank()) {
						result.add(team);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}


	public List<Map<String, String>> ladeAufstellungLigaRangName(String vereinnr) {
		String sql = "SELECT mannschaft, rang, name FROM aufstellung WHERE vereinnr = ?";
		return ladeStringRows(sql, vereinnr);
	}

	public List<Map<String, String>> ladeVerfuegbarkeitMitKommentar(String vereinnr) {
		String sql = "SELECT datum, uhrzeit, name, verfuegbarkeit, kommentar FROM spieler_verfuegbarkeit WHERE vereinnr = ?";
		return ladeStringRows(sql, vereinnr);
	}

	private List<Map<String, String>> ladeStringRows(String sql, String... params) {
		List<Map<String, String>> rows = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				pstmt.setString(i + 1, params[i]);
			}
			try (ResultSet rs = pstmt.executeQuery()) {
				int cols = rs.getMetaData().getColumnCount();
				while (rs.next()) {
					Map<String, String> row = new HashMap<>();
					for (int c = 1; c <= cols; c++) {
						row.put(rs.getMetaData().getColumnLabel(c), rs.getString(c));
					}
					rows.add(row);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	public AufstellungSpielInfo ladeAufstellungSpielInfo(String vereinnr, String uniqueKey) {
		String sql = "SELECT unique_key, datum, wochentag, zeit, liga, heim, gast, ergebnis "
				+ "FROM spielplan_tabelle WHERE vereinnr = ? AND unique_key = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, uniqueKey);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					AufstellungSpielInfo info = new AufstellungSpielInfo();
					info.setUniqueKey(rs.getString("unique_key"));
					info.setDatum(rs.getString("datum"));
					info.setWochentag(rs.getString("wochentag"));
					info.setZeit(rs.getString("zeit"));
					info.setLiga(rs.getString("liga"));
					info.setHeim(rs.getString("heim"));
					info.setGast(rs.getString("gast"));
					info.setErgebnis(rs.getString("ergebnis"));
					return info;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, Object> ladeBetreuerStatus(String uniqueKey) {
		Map<String, Object> result = new HashMap<>();
		String sql = "SELECT betreuer, bestaetigt FROM spielplan_betreuer WHERE unique_key = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, uniqueKey);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					result.put("betreuer", rs.getString("betreuer"));
					Object bestaetigtObj = rs.getObject("bestaetigt");
					result.put("bestaetigt", bestaetigtObj == null ? null : rs.getBoolean("bestaetigt"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<AufstellungSpieler> ladeAufstellungSpieler(String vereinnr) {
		List<AufstellungSpieler> spielerListe = new ArrayList<>();
		String sql = "SELECT vereinnr, mannschaft, rang, qttr, name, a, status FROM aufstellung WHERE vereinnr = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					AufstellungSpieler spieler = new AufstellungSpieler();
					spieler.setVereinnr(rs.getString("vereinnr"));
					spieler.setMannschaft(rs.getString("mannschaft"));
					spieler.setRang(rs.getString("rang"));
					String qttrWert = rs.getString("qttr");
					if (!rs.wasNull()) {
						spieler.setQttr(qttrWert);
					}
					spieler.setName(rs.getString("name"));
					spieler.setA(rs.getString("a"));
					spieler.setStatus(rs.getString("status"));
					spielerListe.add(spieler);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spielerListe;
	}

	public List<Map<String, Object>> ladeSpielerAuswahl(String uniqueKey, String vereinnr) {
		List<Map<String, Object>> result = new ArrayList<>();
		String sql = "SELECT rang, name, ausgewaehlt, kommentar FROM spielplan_spieler WHERE unique_key = ? AND vereinnr = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, uniqueKey);
			pstmt.setString(2, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> row = new HashMap<>();
					row.put("rang", rs.getString("rang"));
					row.put("name", rs.getString("name"));
					row.put("ausgewaehlt", rs.getBoolean("ausgewaehlt"));
					row.put("kommentar", rs.getString("kommentar"));
					result.add(row);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public String ladeSpielKommentar(String uniqueKey, String vereinnr) {
		String sql = "SELECT kommentar FROM spielplan_spielkommentar WHERE unique_key = ? AND vereinnr = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, uniqueKey);
			pstmt.setString(2, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return rs.getString("kommentar");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";
	}

	public boolean speichereAufstellung(String uniqueKey, String vereinnr, List<AufstellungSpieler> spielerListe,
			Map<String, Boolean> spielerAusgewaehlt, Map<String, String> spielerKommentare, String spielKommentar) {
		String deleteSql = "DELETE FROM spielplan_spieler WHERE unique_key = ? AND vereinnr = ?";
		String insertSql = "INSERT INTO spielplan_spieler (unique_key, vereinnr, mannschaft, rang, name, ausgewaehlt, kommentar) VALUES (?, ?, ?, ?, ?, ?, ?)";
		String upsertSpielKommentarSql = "INSERT INTO spielplan_spielkommentar (unique_key, vereinnr, kommentar) VALUES (?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE kommentar = VALUES(kommentar)";
		try (Connection conn = openConnection();) {
			conn.setAutoCommit(false);
			try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
					PreparedStatement insertStmt = conn.prepareStatement(insertSql);
					PreparedStatement spielKommentarStmt = conn.prepareStatement(upsertSpielKommentarSql)) {
				deleteStmt.setString(1, uniqueKey);
				deleteStmt.setString(2, vereinnr);
				deleteStmt.executeUpdate();

				for (AufstellungSpieler spieler : spielerListe) {
					String key = spieler.getKey();
					insertStmt.setString(1, uniqueKey);
					insertStmt.setString(2, vereinnr);
					insertStmt.setString(3, spieler.getMannschaft());
					insertStmt.setString(4, spieler.getRang());
					insertStmt.setString(5, spieler.getName());
					insertStmt.setBoolean(6, Boolean.TRUE.equals(spielerAusgewaehlt.get(key)));
					insertStmt.setString(7, spielerKommentare.getOrDefault(key, ""));
					insertStmt.addBatch();
				}
				insertStmt.executeBatch();

				spielKommentarStmt.setString(1, uniqueKey);
				spielKommentarStmt.setString(2, vereinnr);
				spielKommentarStmt.setString(3, spielKommentar == null ? "" : spielKommentar);
				spielKommentarStmt.executeUpdate();
				conn.commit();
				return true;
			} catch (SQLException ex) {
				conn.rollback();
				throw ex;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public GesamtspielplanEintrag ladeBestaetigungEintrag(String uniqueKey) {
		String sql = "SELECT t.unique_key, t.liga, t.datum, t.zeit, t.heim, t.gast, t.vereinnr, b.betreuer, b.bestaetigt, b.kommentar "
				+ "FROM spielplan_tabelle t LEFT JOIN spielplan_betreuer b ON b.unique_key = t.unique_key "
				+ "WHERE t.unique_key = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, uniqueKey);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					GesamtspielplanEintrag eintrag = new GesamtspielplanEintrag();
					eintrag.setUniqueKey(rs.getString("unique_key"));
					eintrag.setDatum(rs.getString("datum"));
					eintrag.setZeit(rs.getString("zeit"));
					eintrag.setHeim(rs.getString("heim"));
					eintrag.setGast(rs.getString("gast"));
					eintrag.setLiga(rs.getString("liga"));
					eintrag.setBetreuer(rs.getString("betreuer"));
					Object bestaetigtObj = rs.getObject("bestaetigt");
					eintrag.setBestaetigt(bestaetigtObj == null ? null : rs.getBoolean("bestaetigt"));
					eintrag.setKommentar(rs.getString("kommentar"));
					return eintrag;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String ladeVereinnrFuerSpiel(String uniqueKey) {
		String sql = "SELECT vereinnr FROM spielplan_tabelle WHERE unique_key = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, uniqueKey);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return rs.getString("vereinnr");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void upsertSpielplanBetreuer(String uniqueKey, String betreuer, Boolean bestaetigt, String kommentar,
			LocalDateTime mailTimestamp) {
		String upsertSql = "INSERT INTO spielplan_betreuer (unique_key, betreuer, bestaetigt, kommentar, mailTimestamp) VALUES (?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE betreuer = VALUES(betreuer), bestaetigt = VALUES(bestaetigt), kommentar = VALUES(kommentar), mailTimestamp = VALUES(mailTimestamp)";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {
			pstmt.setString(1, uniqueKey);
			pstmt.setString(2, betreuer);
			if (bestaetigt == null) {
				pstmt.setNull(3, java.sql.Types.BOOLEAN);
			} else {
				pstmt.setBoolean(3, bestaetigt);
			}
			if (kommentar == null) {
				pstmt.setNull(4, java.sql.Types.VARCHAR);
			} else {
				pstmt.setString(4, kommentar);
			}
			if (mailTimestamp == null) {
				pstmt.setNull(5, java.sql.Types.TIMESTAMP);
			} else {
				pstmt.setTimestamp(5, Timestamp.valueOf(mailTimestamp));
			}
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void tauscheSpielbetreuer(String aktuellesSpielKey, String neuerBetreuer, String anderesSpielKey,
			String alterBetreuer) {
		String upsertSql = "INSERT INTO spielplan_betreuer (unique_key, betreuer, bestaetigt, kommentar) VALUES (?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE betreuer = VALUES(betreuer), bestaetigt = VALUES(bestaetigt), kommentar = VALUES(kommentar)";
		try (Connection conn = openConnection();
				PreparedStatement aktuellesSpielStmt = conn.prepareStatement(upsertSql);
				PreparedStatement anderesSpielStmt = conn.prepareStatement(upsertSql)) {
			aktuellesSpielStmt.setString(1, aktuellesSpielKey);
			aktuellesSpielStmt.setString(2, neuerBetreuer);
			aktuellesSpielStmt.setNull(3, java.sql.Types.BOOLEAN);
			aktuellesSpielStmt.setNull(4, java.sql.Types.VARCHAR);
			aktuellesSpielStmt.executeUpdate();

			anderesSpielStmt.setString(1, anderesSpielKey);
			anderesSpielStmt.setString(2, alterBetreuer);
			anderesSpielStmt.setNull(3, java.sql.Types.BOOLEAN);
			anderesSpielStmt.setNull(4, java.sql.Types.VARCHAR);
			anderesSpielStmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<GesamtspielplanEintrag> ladeTauschSpiele(String vereinnr, String betreuer, String aktuellesUniqueKey) {
		List<GesamtspielplanEintrag> spiele = new ArrayList<>();
		String sql = "SELECT t.unique_key, t.datum, t.zeit, t.heim, t.gast "
				+ "FROM spielplan_tabelle t INNER JOIN spielplan_betreuer b ON b.unique_key = t.unique_key "
				+ "WHERE t.vereinnr = ? AND b.betreuer = ? AND t.unique_key <> ? "
				+ "AND STR_TO_DATE(t.datum, '%d.%m.%Y') >= CURDATE() "
				+ "ORDER BY STR_TO_DATE(t.datum, '%d.%m.%Y'), t.zeit";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			pstmt.setString(2, betreuer);
			pstmt.setString(3, aktuellesUniqueKey);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					GesamtspielplanEintrag eintrag = new GesamtspielplanEintrag();
					eintrag.setUniqueKey(rs.getString("unique_key"));
					eintrag.setDatum(rs.getString("datum"));
					eintrag.setZeit(rs.getString("zeit"));
					eintrag.setHeim(rs.getString("heim"));
					eintrag.setGast(rs.getString("gast"));
					spiele.add(eintrag);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spiele;
	}

	public void saveSpielplanEntries(String vereinnr, List<Spiel> spiele, String liga) {
		if (spiele == null || spiele.isEmpty()) {
			return;
		}

		String insertSql = "INSERT INTO spielplan_tabelle "
				+ " (unique_key, vereinnr, wochentag_datum, wochentag, datum, zeit, liga, spiellokal, heim, gast, ergebnis, ergebnis_link, heim_link, gast_link) "
				+ "VALUES (? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE wochentag_datum=VALUES(wochentag_datum), wochentag=VALUES(wochentag),  "
				+ "datum=VALUES(datum), zeit=VALUES(zeit), spiellokal=VALUES(spiellokal), ergebnis=VALUES(ergebnis), ergebnis_link=VALUES(ergebnis_link), "
				+ "heim_link=VALUES(heim_link), gast_link=VALUES(gast_link)";

		try (Connection conn = openConnection()) {

			try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
				for (Spiel spiel : spiele) {
					pstmt.setString(1, generateUniqueKey(spiel, vereinnr));
					pstmt.setString(2, vereinnr);
					pstmt.setString(3, spiel.getDatumGesamt());
					pstmt.setString(4, spiel.getWochentag());
					pstmt.setString(5, spiel.getDatum());
					pstmt.setString(6, spiel.getZeit());
					pstmt.setString(7, TennisGruppeKurz.kuerzeGruppe(liga));
					pstmt.setString(8, "");
					pstmt.setString(9, spiel.getHeim());
					pstmt.setString(10, spiel.getGast());
					pstmt.setString(11, spiel.getErgebnis());
					pstmt.setString(12, spiel.getErgebnisLink());
					pstmt.setString(13, "");
					pstmt.setString(14, "");
					pstmt.addBatch();
				}
				pstmt.executeBatch();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private String generateUniqueKey(Spiel spiel, String vereinnr) {
		String keyInput = String.join("|", vereinnr, sanitize(spiel.getHeim()), sanitize(spiel.getGast()),
				sanitize(spiel.getLiga()));
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(keyInput.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 ist nicht verfügbar", e);
		}
	}

	private String sanitize(String value) {
		return value == null ? "" : value.trim();
	}

	public List<AdressEintrag> ladeAdressEintraege(String vereinnr) {
		List<AdressEintrag> result = new ArrayList<>();
		String sql = "SELECT id, vereinnr, name, vorname, geburtstag, strasse, plz, wohnort, telefon_privat, telefon_gesch, telefon_mobil,"
				+ " email_privat, email_gesch, bemerkung, erstellt_am, aktualisiert_am"
				+ " FROM adressliste WHERE vereinnr = ? ORDER BY name, vorname";
		try (Connection conn = openConnection();

				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, vereinnr);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					result.add(mapAdressEintrag(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public AdressEintrag erstelleAdressEintrag(String vereinnr, AdressEintrag eintrag) {
		String sql = "INSERT INTO adressliste (vereinnr, name, vorname, geburtstag, strasse, plz, wohnort, telefon_privat, telefon_gesch, telefon_mobil,"
				+ " email_privat, email_gesch, bemerkung) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (Connection conn = openConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, vereinnr);
			setAdressCommonFields(eintrag, pstmt, 2);
			int anzahl = pstmt.executeUpdate();
			if (anzahl > 0) {
				try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						eintrag.setId(generatedKeys.getInt(1));
					}
				}
				LocalDateTime jetzt = LocalDateTime.now();
				eintrag.setVereinnr(vereinnr);
				eintrag.setErstelltAm(jetzt);
				eintrag.setAktualisiertAm(jetzt);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return eintrag;
	}

	public boolean aktualisiereAdressEintrag(String vereinnr, AdressEintrag eintrag) {
		String historySql = "INSERT INTO adressliste_historie (original_id, vereinnr, name, vorname, geburtstag, strasse, plz, wohnort,"
				+ " telefon_privat, telefon_gesch, telefon_mobil, email_privat, email_gesch, bemerkung, erstellt_am, aktualisiert_am)"
				+ " SELECT id, vereinnr, name, vorname, geburtstag, strasse, plz, wohnort, telefon_privat, telefon_gesch, telefon_mobil,"
				+ " email_privat, email_gesch, bemerkung, erstellt_am, aktualisiert_am FROM adressliste WHERE id = ? AND vereinnr = ?";
		String updateSql = "UPDATE adressliste SET name = ?, vorname = ?, geburtstag = ?, strasse = ?, plz = ?, wohnort = ?,"
				+ " telefon_privat = ?, telefon_gesch = ?, telefon_mobil = ?, email_privat = ?, email_gesch = ?, bemerkung = ?,"
				+ " aktualisiert_am = CURRENT_TIMESTAMP WHERE id = ? AND vereinnr = ?";
		try (Connection conn = openConnection();

		) {
			conn.setAutoCommit(false);
			try (PreparedStatement historyStmt = conn.prepareStatement(historySql);
					PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
				historyStmt.setInt(1, eintrag.getId());
				historyStmt.setString(2, vereinnr);
				historyStmt.executeUpdate();

				setAdressCommonFields(eintrag, updateStmt, 1);
				updateStmt.setInt(13, eintrag.getId());
				updateStmt.setString(14, vereinnr);
				int anzahl = updateStmt.executeUpdate();
				conn.commit();
				if (anzahl > 0) {
					eintrag.setVereinnr(vereinnr);
					eintrag.setAktualisiertAm(LocalDateTime.now());
				}
				return anzahl > 0;
			} catch (SQLException ex) {
				conn.rollback();
				throw ex;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean loescheAdressEintrag(String vereinnr, Integer id) {
		String historySql = "INSERT INTO adressliste_historie (original_id, vereinnr, name, vorname, geburtstag, strasse, plz, wohnort,"
				+ " telefon_privat, telefon_gesch, telefon_mobil, email_privat, email_gesch, bemerkung, erstellt_am, aktualisiert_am)"
				+ " SELECT id, vereinnr, name, vorname, geburtstag, strasse, plz, wohnort, telefon_privat, telefon_gesch, telefon_mobil,"
				+ " email_privat, email_gesch, bemerkung, erstellt_am, aktualisiert_am FROM adressliste WHERE id = ? AND vereinnr = ?";
		String deleteSql = "DELETE FROM adressliste WHERE id = ? AND vereinnr = ?";
		try (Connection conn = openConnection();

		) {
			conn.setAutoCommit(false);
			try (PreparedStatement historyStmt = conn.prepareStatement(historySql);
					PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
				historyStmt.setInt(1, id);
				historyStmt.setString(2, vereinnr);
				historyStmt.executeUpdate();

				deleteStmt.setInt(1, id);
				deleteStmt.setString(2, vereinnr);
				int anzahl = deleteStmt.executeUpdate();
				conn.commit();
				return anzahl > 0;
			} catch (SQLException ex) {
				conn.rollback();
				throw ex;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void setAdressCommonFields(AdressEintrag eintrag, PreparedStatement pstmt, int offset) throws SQLException {
		pstmt.setString(offset, eintrag.getName());
		pstmt.setString(offset + 1, eintrag.getVorname());
		if (eintrag.getGeburtstag() == null) {
			pstmt.setNull(offset + 2, java.sql.Types.DATE);
		} else {
			pstmt.setDate(offset + 2, Date.valueOf(eintrag.getGeburtstag()));
		}
		pstmt.setString(offset + 3, eintrag.getStrasse());
		pstmt.setString(offset + 4, eintrag.getPlz());
		pstmt.setString(offset + 5, eintrag.getWohnort());
		pstmt.setString(offset + 6, eintrag.getTelefonPrivat());
		pstmt.setString(offset + 7, eintrag.getTelefonGesch());
		pstmt.setString(offset + 8, eintrag.getTelefonMobil());
		pstmt.setString(offset + 9, eintrag.getEmailPrivat());
		pstmt.setString(offset + 10, eintrag.getEmailGesch());
		pstmt.setString(offset + 11, eintrag.getBemerkung());
	}

	private AdressEintrag mapAdressEintrag(ResultSet rs) throws SQLException {
		AdressEintrag eintrag = new AdressEintrag();
		eintrag.setId(rs.getInt("id"));
		eintrag.setVereinnr(rs.getString("vereinnr"));
		eintrag.setName(rs.getString("name"));
		eintrag.setVorname(rs.getString("vorname"));
		Date geburtstag = rs.getDate("geburtstag");
		if (geburtstag != null) {
			eintrag.setGeburtstag(geburtstag.toLocalDate());
		}
		eintrag.setStrasse(rs.getString("strasse"));
		eintrag.setPlz(rs.getString("plz"));
		eintrag.setWohnort(rs.getString("wohnort"));
		eintrag.setTelefonPrivat(rs.getString("telefon_privat"));
		eintrag.setTelefonGesch(rs.getString("telefon_gesch"));
		eintrag.setTelefonMobil(rs.getString("telefon_mobil"));
		eintrag.setEmailPrivat(rs.getString("email_privat"));
		eintrag.setEmailGesch(rs.getString("email_gesch"));
		eintrag.setBemerkung(rs.getString("bemerkung"));
		eintrag.setErstelltAm(timestampToLocalDateTime(rs.getTimestamp("erstellt_am")));
		eintrag.setAktualisiertAm(timestampToLocalDateTime(rs.getTimestamp("aktualisiert_am")));
		return eintrag;
	}

	private LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return timestamp.toLocalDateTime();
	}

	private Connection openConnection() throws SQLException {
		DataSource ds = getDataSourceOrNull();
		if (ds != null) {
			return ds.getConnection();
		}

		return DriverManager.getConnection(buildJdbcUrl(), config.getDatabaseUser(), config.getDatabasePassword());
	}

	private DataSource getDataSourceOrNull() {
		if (dataSource != null) {
			return dataSource;
		}

		synchronized (DatabaseService.class) {
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

	private String resolveJndiName() {
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

	private String buildJdbcUrl() {
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

	public void speichernMail(String empfaenger, String empfaengerCc, String betreff, String htmlText,
			String attachmentName) {
		String sql = "INSERT INTO VersendeteMails "
				+ "(empfaenger, empfaenger_cc, betreff, text, attachmentName) VALUES (?, ?, ?, ?, ?)";

		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, empfaenger);
			pstmt.setString(2, empfaengerCc);
			pstmt.setString(3, betreff);
			pstmt.setString(4, htmlText);
			pstmt.setString(5, attachmentName);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Versendete Mail konnte nicht gespeichert werden.", e);
		}
	}

	public List<VersendeteMail> ladeVersendeteMails() {
		String sql = "SELECT id, timestamp, empfaenger, empfaenger_cc, empfaenger_bcc, betreff, text, attachmentName "
				+ "FROM VersendeteMails ORDER BY timestamp DESC";
		List<VersendeteMail> mails = new ArrayList<>();
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				VersendeteMail mail = new VersendeteMail();
				mail.setId(rs.getLong("id"));
				Timestamp timestamp = rs.getTimestamp("timestamp");
				if (timestamp != null) {
					mail.setTimestamp(timestamp.toLocalDateTime());
				}
				mail.setEmpfaenger(rs.getString("empfaenger"));
				mail.setEmpfaengerCc(rs.getString("empfaenger_cc"));
				mail.setEmpfaengerBcc(rs.getString("empfaenger_bcc"));
				mail.setBetreff(rs.getString("betreff"));
				mail.setText(rs.getString("text"));
				mail.setAttachmentName(rs.getString("attachmentName"));
				mails.add(mail);
			}
			rs.close();
		} catch (SQLException e) {
			throw new IllegalStateException("Versendete Mails konnten nicht geladen werden.", e);
		}
		return mails;
	}

	public void loescheVersendeteMail(long id) {
		String sql = "DELETE FROM VersendeteMails WHERE id = ?";
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, id);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Versendete Mail konnte nicht gelöscht werden.", e);
		}
	}
	

	public SpielcodeEintrag ladeSpielcodeEintrag(String uniqueKey, String vereinnr) {
		String sql = "SELECT t.liga, t.datum, t.zeit, t.heim, t.gast, s.spiel_code, s.pin "
				+ "FROM spielplan_tabelle t "
				+ "LEFT JOIN spielcodes s ON s.unique_key = t.unique_key "
				+ "WHERE t.unique_key = ?";
		if (vereinnr != null && !vereinnr.isBlank()) {
			sql += " AND t.vereinnr = ?";
		}
		try (Connection conn = openConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, uniqueKey);
			if (vereinnr != null && !vereinnr.isBlank()) {
				pstmt.setString(2, vereinnr);
			}
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					SpielcodeEintrag eintrag = new SpielcodeEintrag();
					eintrag.setLiga(rs.getString("liga"));
					eintrag.setDatum(rs.getString("datum"));
					eintrag.setZeit(rs.getString("zeit"));
					eintrag.setHeim(rs.getString("heim"));
					eintrag.setGast(rs.getString("gast"));
					String spielCode = rs.getString("spiel_code");
					String pin = rs.getString("pin");
					eintrag.setSpielCode(spielCode);
					eintrag.setPin(pin);
					eintrag.setSpielcodeGefunden((spielCode != null && !spielCode.isBlank())
							|| (pin != null && !pin.isBlank()));
					return eintrag;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}


}
