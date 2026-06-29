# SportBericht

**Web-basiertes Verwaltungs- und Berichtssystem für Tischtennis- und Tennisvereine.**

SportBericht ist eine Jakarta-EE-Webanwendung, mit der Vereine ihren kompletten Spielbetrieb
abbilden: Spielpläne und Ergebnisse, Aufstellungen, Spielerverfügbarkeiten, Adresslisten,
Bildverwaltung sowie die automatische Erstellung und Veröffentlichung von Spielberichten –
unterstützt durch KI (ChatGPT, Claude, DeepSeek, Gemini) und mit direkter Anbindung an
WordPress, E-Mail, Telegram und SFTP.

Die Anwendung ist **mandantenfähig**: Über eine Vereinsnummer (`vereinnr`) werden Daten,
Konfiguration, Zugangsdaten und sogar das Farbschema je Verein getrennt verwaltet.

---

## Inhaltsverzeichnis

- [Funktionen](#funktionen)
- [Technologie-Stack](#technologie-stack)
- [Architektur](#architektur)
- [Projektstruktur](#projektstruktur)
- [Voraussetzungen](#voraussetzungen)
- [Build & Deployment](#build--deployment)
- [Konfiguration](#konfiguration)
- [Datenbank](#datenbank)
- [Lizenz](#lizenz)
- [Hinweise](#hinweise)

---

## Funktionen

### Spielbetrieb
- **Spielplan & Ergebnisse** – Spielpläne und Resultate für Tischtennis und Tennis,
  inkl. Tabellen, Bilanzen und Liga-Ansicht.
- **Gesamtspielplan** – konfigurierbare Gesamtübersicht über alle Mannschaften und Runden
  mit Betreuer-Zuteilung, Tooltips und Wochen-Gruppierung.
- **Aufstellung** – Verwaltung der Mannschaftsaufstellungen inklusive Import.
- **Verfügbarkeit & Bestätigung** – Spieler melden ihre Verfügbarkeit pro Spiel zurück;
  Betreuer erhalten Bestätigungslinks und Statusübersichten.

### Berichte
- **Spielbericht-Editor** – komfortabler Editor (Quill) für Spielberichte mit Bild,
  Bildunterschrift, Überschrift und Statistik.
- **KI-Berichte** – automatische Generierung von Spielberichten und Zusammenfassungen über
  ChatGPT, Claude, DeepSeek oder Gemini; Prompts, Schreibstil und Wortanzahl je Verein
  konfigurierbar. Generierte Texte sind nachträglich editierbar.
- **Freie Berichte & Zusammenfassungen** – frei gestaltbare Berichte sowie
  KI-Zusammenfassungen einzelner Spiele oder ganzer Spieltage.
- **Historie** – Änderungsverlauf von Berichten und Adressdaten mit Diff-Darstellung.

### Veröffentlichung & Kommunikation
- **WordPress** – Veröffentlichung von Berichten und Medien über die WordPress-REST-API.
- **E-Mail** – Versand über SMTP (Jakarta Mail); Archiv versendeter Mails.
- **Telegram** – Benachrichtigungen über einen Telegram-Bot.
- **SFTP** – Upload von Inhalten (z. B. Hallenbelegungsplänen) auf externe Webspaces.

### Verwaltung & Werkzeuge
- **Adressliste** – Mitgliederverwaltung mit Such-/Filterfunktion, Info-Popups,
  Änderungshistorie und Excel-Unterstützung.
- **Bilderverwaltung** – Galerie sowie Bildbearbeitung (Ausschnitt/Selektion).
- **Spielcodes / Zwischenablage** – Erzeugung und Verarbeitung von Spielcodes inkl.
  QR-/Barcode-Generierung.
- **Hallenbelegung** – Einlesen von PDF-Plänen und KI-gestützte Aufbereitung.
- **Dynamisches Theming** – pro Verein einstellbarer Farb-Hue über ein dynamisch
  generiertes Stylesheet.
- **Konfiguration** – umfangreiche Vereinskonfiguration über die Oberfläche, inkl.
  HTML-Snippets und API-Zugangsdaten.

---

## Technologie-Stack

| Bereich            | Technologie |
|--------------------|-------------|
| Sprache / Build    | Java 17, Maven (WAR-Packaging) |
| Plattform          | Jakarta EE 10, CDI (Weld) |
| Web-Framework       | Jakarta Faces (JSF) 4.0, PrimeFaces 14 (Theme „saga") |
| Applikationsserver | WildFly (Undertow) |
| Datenbank          | MariaDB (über JDBC / WildFly-DataSource) |
| KI                 | OpenAI (`openai-java`), Claude, DeepSeek, Gemini (via OkHttp) |
| Dokumente/Medien   | Apache PDFBox, OpenPDF, Apache POI, ZXing (QR/Barcode) |
| Integrationen      | Jakarta Mail (SMTP), JSch (SFTP), WordPress REST, Telegram Bot |
| Text/HTML          | CommonMark (Markdown), jsoup, OWASP HTML Sanitizer |
| Caching            | Caffeine |
| Kalender           | Jollyday + Schulferien-API |
| Tests              | JUnit 5, Mockito |
| Hilfsmittel        | Lombok |

---

## Architektur

Die Anwendung folgt einem klassischen JSF/CDI-Schichtmodell:

- **Controller (`de.bericht.controller`)** – CDI-Beans (`@Named`, überwiegend `@ViewScoped`),
  die je einer XHTML-Seite zugeordnet sind (z. B. `SpielplanBean`, `BerichtBean`,
  `VerfuegbarBean`, `AdresslisteBean`).
- **Service (`de.bericht.service`)** – fachliche Logik und Datenzugriff: Spielpläne,
  Ergebnisse, Tabellen, E-Mail, Datenbank, Schema-Initialisierung u. v. m. Für Tischtennis
  und Tennis existieren jeweils spezialisierte Implementierungen.
- **Provider/Factory (`de.bericht.provider`)** – Auswahl der passenden Implementierung je
  Sportart bzw. KI-System (z. B. `SpielplanFactory`, `KiProviderFactory`).
- **Util (`de.bericht.util`)** – Querschnittsfunktionen, KI-Clients, Konfigurations-Manager,
  Caching, Bildverarbeitung, WordPress-Client.
- **Filter (`de.bericht.filter`)** – `IndexForwardFilter` für Einstieg/Weiterleitung.

**Mandantenfähigkeit:** Nahezu alle Daten und Einstellungen werden über die Vereinsnummer
`vereinnr` getrennt. Die Anmeldung erfolgt mit *Verein + Name + Passwort*; optional wird per
Cookie-Token dauerhaft angemeldet (Tabelle `login_token`). Wird der Name in der Adressliste
erkannt, werden zusätzliche Funktionen freigeschaltet.

Das Datenbankschema wird beim Start automatisch via `DatabaseSchemaInitializer`
(„CREATE TABLE IF NOT EXISTS …") angelegt – eine manuelle Migration ist nicht erforderlich.

---

## Projektstruktur

```
SportBericht/
├── pom.xml
├── src/main/java/de/bericht/
│   ├── controller/   # JSF-Backing-Beans (eine je Seite)
│   ├── service/      # Geschäftslogik & Datenzugriff (Tischtennis/Tennis)
│   ├── provider/     # Factories/Provider je Sportart bzw. KI
│   ├── util/         # KI-Clients, Config, Caching, Bild-/Textwerkzeuge
│   └── filter/       # Servlet-Filter
└── src/main/webapp/
    ├── *.xhtml                 # Seiten (index, spielplan, bericht, verfuegbar, …)
    ├── WEB-INF/                # web.xml, beans.xml, faces-config.xml
    └── resources/
        ├── css/styles.css      # zentrales Stylesheet (CSS-Variablen/Theme)
        ├── js/                 # script.js
        └── images/
```

---

## Voraussetzungen

- **JDK 17**
- **Maven 3.8+**
- **WildFly** (mit Undertow; konfiguriert für Jakarta EE 10)
- **MariaDB** (Datenbank + Benutzer für den Verein)

---

## Build & Deployment

### Bauen

```bash
mvn clean package
```

Erzeugt `target/SportBericht-6.0.war`.

### Deployen

Das WAR in WildFly deployen (z. B. nach `$WILDFLY_HOME/standalone/deployments/` kopieren
oder über die Management-Konsole). Anschließend ist die Anwendung erreichbar unter:

```
http://<host>:8080/SportBericht-6.0/
```

> Das mitgelieferte `copy.sh` unterstützt das lokale Deployment.

---

## Konfiguration

### Datenbankanbindung

Die Anwendung sucht standardmäßig eine WildFly-DataSource unter dem JNDI-Namen
`java:/jdbc/TischtennisDS`. Der Name lässt sich überschreiben:

- System-Property `database.jndi`, oder
- Umgebungsvariable `DATABASE_JNDI`

Ist keine DataSource verfügbar, kann ein direkter JDBC-Fallback (MariaDB) genutzt werden.

### Vereins- und Integrationskonfiguration

Vereinsspezifische Einstellungen liegen in der Tabelle `config` (Schlüssel/Wert je
`vereinnr`) und werden über die Oberfläche bzw. den `ConfigManager` verwaltet. Dazu zählen
u. a.:

- **KI:** `ki.chatgpt.api`, `ki.claudeai.api`, `ki.deepseek.api`, `ki.gemini.api`,
  Modell- und Prompt-Einstellungen (`ki.model.*`, `ki.prompt.*`, `ki.bericht.wortanzahl`)
- **E-Mail (SMTP):** `mail.smtp.host`, `mail.smtp.port`, `mail.username`, `mail.passwort`,
  `mail.smtp.auth`, `mail.smtp.starttls.enable`, Empfänger (`mail.*.empfaenger`)
- **SFTP:** `sftp.url`, `sftp.port`, `sftp.user`, `sftp.passwort`
- **Telegram:** `messenger.telegram.api`, `messenger.telegram.chatid`
- **WordPress/Homepage:** `homepage.verein`, `bericht.zeitung.url`, `programm.URL`
- **Darstellung:** `style.farbe` (Farb-Hue für das dynamische Theme)
- **Administration/Freigaben:** `admin.passwort`, `bericht.freigabe`, `config.berechtigung`

> **Sicherheit:** API-Schlüssel und Passwörter werden in der Datenbank gehalten. Diese
> Zugangsdaten gehören nicht ins Repository und sind pro Verein über die Konfiguration zu
> pflegen.

---

## Datenbank

Das Schema wird automatisch initialisiert. Wesentliche Tabellen (Auszug):

| Tabelle | Inhalt |
|---------|--------|
| `config`, `config_html*`, `config_gesamtspielplan*` | Vereinskonfiguration, HTML-Snippets, Gesamtspielplan-Setup |
| `berichte`, `berichte_historie` | Spielberichte inkl. Versionshistorie |
| `adressliste`, `adressliste_historie` | Mitglieder-/Adressdaten mit Historie |
| `aufstellung`, `mannschaft` | Aufstellungen und Mannschaften |
| `spieler_verfuegbarkeit`, `spielplan_*` | Verfügbarkeiten, Betreuer, Spieler, Kommentare, Tabellen |
| `spielcodes`, `short_links` | Spielcodes und Kurzlinks |
| `Hallenbelegung`, `spiellokal` | Hallenbelegung und Spielstätten |
| `VersendeteMails`, `login_token`, `log_tabelle` | Mail-Archiv, dauerhafte Logins, Logging |
| `Vorname`, `Nachname`, `school_holidays`, `web_pages`, `stilelemente`, `doppel` | Stammdaten, Ferien, Web-Cache, Stil, Doppel |

---

## Lizenz

**Privat / proprietär – Alle Rechte vorbehalten.**

Diese Software ist privates, urheberrechtlich geschütztes Eigentum. Es wird keine Lizenz zur
Nutzung, Vervielfältigung, Änderung oder Verbreitung gewährt. Jede Verwendung bedarf der
vorherigen ausdrücklichen schriftlichen Genehmigung des Rechteinhabers. Einzelheiten siehe
[LICENSE](LICENSE).

---

## Hinweise

- Die Oberfläche und Inhalte sind in **deutscher Sprache**.
- Unterstützte Sportarten: **Tischtennis** und **Tennis** (umschaltbar je Verein über die
  Konfiguration, siehe `SportartVerein`).
- Rechtliche Angaben sind in der Anwendung unter **Impressum** hinterlegt.
