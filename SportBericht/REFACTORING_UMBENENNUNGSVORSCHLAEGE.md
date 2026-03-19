# Refactoring-Umbenennungsvorschläge für TischtennisBericht

## Übersicht
Dieses Dokument enthält Umbenennungsvorschläge für alle Java-Klassen und öffentlichen Methoden im Projekt `TischtennisBericht`.

**Analysierte Komponenten:**
- 72 Java-Klassen
- 15 Controller-Klassen (Bean-Klassen)
- 13 Service-Klassen  
- 21 Model/Entity-Klassen
- 19 Utility-Klassen
- 4 Provider/Factory-Klassen

---

## CONTROLLER-KLASSEN (in de/meintt/controller/)

### KLASSE: AenderungBean → AenderungController
Pfad: `de/meintt/controller/AenderungBean`

**Wichtige Methoden:**
- `spielplanAnzeige()` → `zeigeSpielplan()`

### KLASSE: BerichtBean → BerichtController
Pfad: `de/meintt/controller/BerichtBean`

**Geschäftslogik-Methoden:**
- `bildLoeschen()` → `loescheBild()`
- `emailSenden()` → `sende_Email()`
- `korrektur()` → `korrigiere()`
- `navigateToBerichtKI()` → `navigationZuKI_Bericht()`
- `spielHtmlLesen()` → `spielHTML_Lesen()`
- `spielplanAnzeige()` → `zeigeSpielplan()`
- `speichern()` → `speichern()`
- `speichernUndWeiter()` → `speichernUndWeiter()`
- `stripHtmlWithJsoup()` → `entferneHTML()`
- `verbessernKI()` → `verbessereKI_Bericht()`

### KLASSE: BerichtkiBean → KI_BerichtController
Pfad: `de/meintt/controller/BerichtkiBean`

**Geschäftslogik-Methoden:**
- `decodeUrl()` → `dekodiere_URL()`
- `enthaeltUmlaute()` → `enthaelt_Umlaute()`
- `generieren()` → `generiere()`
- `onSlide()` → `beiBewegung()`
- `setBerichtIndex()` → `setzeBerichtIndex()`
- `speichern()` → `speichern()`

### KLASSE: ConfigBean → ConfigController
Pfad: `de/meintt/controller/ConfigBean`

**Geschäftslogik-Methoden:**
- `encrypt()` → `verschluessele()`
- `init()` → `initialisiere()`
- `insertHomepage()` → `einfuegen_Homepage()`
- `speichern()` → `speichern()`

### KLASSE: ErrorBean → FehlerController
Pfad: `de/meintt/controller/ErrorBean`

**Geschäftslogik-Methoden:**
- `getStacktrace()` → `holeStacktrace()`
- `getMessage()` → `holeNachricht()`
- `getType()` → `holeTyp()`
- `isExceptionAvailable()` → `istFehlerVerfuegbar()`

### KLASSE: FreieBerichteBean → FreieBerichteController
Pfad: `de/meintt/controller/FreieBerichteBean`

**Geschäftslogik-Methoden:**
- `init()` → `initialisiere()`
- `loeschen()` → `loeschen()`
- `onDateSelect()` → `beiDatumAuswahl()`
- `updConfig()` → `aktualisiereConfig()`

### KLASSE: HistorienBean → HistorienController
Pfad: `de/meintt/controller/HistorienBean`

**Geschäftslogik-Methoden:**
- `init()` → `initialisiere()`

### KLASSE: ImageBean → BildController
Pfad: `de/meintt/controller/ImageBean`

### KLASSE: ImpressumBean → ImpressumController
Pfad: `de/meintt/controller/ImpressumBean`

### KLASSE: KommentarBean → KommentarController
Pfad: `de/meintt/controller/KommentarBean`

**Geschäftslogik-Methoden:**
- `init()` → `initialisiere()`

### KLASSE: SpielplanBean → SpielplanController
Pfad: `de/meintt/controller/SpielplanBean`

**Geschäftslogik-Methoden:**
- `init()` → `initialisiere()`

### KLASSE: TestBean → TestController
Pfad: `de/meintt/controller/TestBean`

### KLASSE: ZusammenBean → ZusammenfassungController
Pfad: `de/meintt/controller/ZusammenBean`

**Geschäftslogik-Methoden:**
- `init()` → `initialisiere()`
- `spielplanAnzeige()` → `zeigeSpielplan()`

### KLASSE: ZusammenGesamtBean → GesamtzusammenfassungController
Pfad: `de/meintt/controller/ZusammenGesamtBean`

**Geschäftslogik-Methoden:**
- `init()` → `initialisiere()`

---

## SERVICE-KLASSEN (in de/meintt/service/)

### KLASSE: BilanzService → AbrechnungService
Pfad: `de/meintt/service/BilanzService`

**Hauptmethoden:**
- `ausgabe()` → `gib_Ausgabe()`
- `getBilanz()` → `hole_Abrechnung()`

### KLASSE: ConfigService → KonfigurationService
Pfad: `de/meintt/service/ConfigService`

### KLASSE: DatabaseService → DatenbankenService
Pfad: `de/meintt/service/DatabaseService`

### KLASSE: EmailService → EmailService
Pfad: `de/meintt/service/EmailService`

### KLASSE: MannschaftService → MannschaftService
Pfad: `de/meintt/service/MannschaftService`

**Hauptmethoden:**
- `ausgabe()` → `gib_Ausgabe()`
- `getMannschaften()` → `hole_Mannschaften()`
- `setMannschaften()` → `setze_Mannschaften()`

### KLASSE: SpielplanService → SpielplanService
Pfad: `de/meintt/service/SpielplanService`

**Hauptmethoden:**
- `generierenSpielplan()` → `generiere_Spielplan()`
- `generiereVorschauBericht()` → `generiere_Vorschau_Bericht()`
- `getSpielplan()` → `hole_Spielplan()`
- `getSpielplanFreigabe()` → `hole_Spielplan_Freigabe()`

### KLASSE: SpielplanServiceClick → SpielplanKlickService
Pfad: `de/meintt/service/SpielplanServiceClick`

### KLASSE: SpielergebnisService → SpielergebnisService
Pfad: `de/meintt/service/SpielergebnisService`

### KLASSE: SpielergebnisClickTTService → SpielergebnisTT_KlickService
Pfad: `de/meintt/service/SpielergebnisClickTTService`

### KLASSE: StatusService → StatusService
Pfad: `de/meintt/service/StatusService`

### KLASSE: TabelleService → TabelleService
Pfad: `de/meintt/service/TabelleService`

### KLASSE: TelegrammService → TelegrammService
Pfad: `de/meintt/service/TelegrammService`

### KLASSE: KiAenderung → KI_Aenderungsanbieter
Pfad: `de/meintt/service/KiAenderung`

### KLASSE: KiZusammenfassenText → KI_Textzusammenfasser
Pfad: `de/meintt/service/KiZusammenfassenText`

---

## MODEL/ENTITY-KLASSEN (in de/meintt/service/)

### KLASSE: Bilanz → Abrechnung
Pfad: `de/meintt/service/Bilanz`

**Getter/Setter für:**
- `rang` / `name` / `einsaetze` / `p1` / `p2` / `p3` / `p4` / `p5` / `p6` / `gesamt`

### KLASSE: Spiel → Spieleintrag
Pfad: `de/meintt/service/Spiel`

### KLASSE: EinzelErgebnis → EinzelspielerErgebnis
Pfad: `de/meintt/service/EinzelErgebnis`

### KLASSE: DoppelErgebnis → DoppelspielerErgebnis
Pfad: `de/meintt/service/DoppelErgebnis`

### KLASSE: Mannschaft → MannschaftsEintrag
Pfad: `de/meintt/service/Mannschaft`

### KLASSE: Tabelle → TabellenEintrag
Pfad: `de/meintt/service/Tabelle`

### KLASSE: LogEntry → ProtokollEintrag
Pfad: `de/meintt/service/LogEntry`

### KLASSE: BerichtText → BerichtInhalt
Pfad: `de/meintt/service/BerichtText`

---

## UTILITY-KLASSEN (in de/meintt/util/)

### KLASSE: ConfigManager → KonfigurationsverwaltungStatisch
Pfad: `de/meintt/util/ConfigManager`

**Hauptmethoden:**
- `getConfigValue()` → `hole_Konfigurationswert()`
- `getSpielplanLiga()` → `hole_Spielplan_Liga()`
- `getSpielplanURL()` → `hole_Spielplan_URL()`

### KLASSE: ConfigEintrag → KonfigurationsEintrag
Pfad: `de/meintt/util/ConfigEintrag`

### KLASSE: NamensSpeicher → NamenspeicherVerwaltung
Pfad: `de/meintt/util/NamensSpeicher`

**Hauptmethoden:**
- `formatName()` → `formatiere_Namen()`
- `hinzufuegen()` → `hinzufuegen()`

### KLASSE: ImageProcessor → BildverarbeitungService
Pfad: `de/meintt/util/ImageProcessor`

### KLASSE: Spielbericht → BerichtInformation
Pfad: `de/meintt/util/Spielbericht`

### KLASSE: WpComment → WordPressKommentar
Pfad: `de/meintt/util/WpComment`

**Getter/Setter für:**
- `id` / `post` / `authorName` / `content` / `status` / `postId` / `date`

### KLASSE: WordPressAPIClient → WordPressAPI_Klient
Pfad: `de/meintt/util/WordPressAPIClient`

**Hauptmethoden:**
- `approveComment()` → `genehmige_Kommentar()`
- `createPost()` → `erstelle_Post()`
- `getCategoryIdByName()` → `hole_Kategorie_ID_nach_Name()`
- `markCommentAsSpam()` → `markiere_Kommentar_als_Spam()`
- `sanitizeJsonResponse()` → `bereinige_JSON_Antwort()`
- `trashComment()` → `verschiebe_Kommentar_in_Papierkorb()`
- `uploadMedia()` → `hochlade_Medium()`
- `uploadMediaAndInsertIntoPost()` → `hochlade_Medium_und_einfuegen_in_Post()`

### KLASSE: WordpressMedia → WordPressMedium
Pfad: `de/meintt/util/WordpressMedia`

### KLASSE: IgnorierteWoerte → IgnorierteWoerterListe
Pfad: `de/meintt/util/IgnorierteWoerte`

**Hauptmethoden:**
- `hinzufuegen()` → `hinzufuegen()`
- `istIgnoriert()` → `ist_Ignoriert()`

### KLASSE: ErgebnisCache → ErgebnisCache
Pfad: `de/meintt/util/ErgebnisCache`

### KLASSE: WebCache → WebseiteCache
Pfad: `de/meintt/util/WebCache`

**Hauptmethoden:**
- `clearCache()` → `loesche_Cache()`
- `getPage()` → `hole_Seite()`

### KLASSE: SpielMapped → GemappterSpiel
Pfad: `de/meintt/util/SpielMapped`

### KLASSE: SpielUtils → SpielWerkzeuge
Pfad: `de/meintt/util/SpielUtils`

### KLASSE: BerichtHelper → BerichtHilfsfunktionen
Pfad: `de/meintt/util/BerichtHelper`

**Hauptmethoden:**
- `hasFreigabe()` → `hat_Freigabe()`
- `vereinsnummer()` → `ermittle_Vereinsnummer()`

### KLASSE: BerichtData → BerichtDaten
Pfad: `de/meintt/util/BerichtData`

### KLASSE: KiText → KI_Textgenerator
Pfad: `de/meintt/util/KiText`

### KLASSE: KiZusammenfassenText → KI_Textzusammenfasser
Pfad: `de/meintt/util/KiZusammenfassenText`

### KLASSE: ApiKIChatGPT → ChatGPT_KI_API
Pfad: `de/meintt/util/ApiKIChatGPT`

### KLASSE: ApiKIDeepSeek → DeepSeek_KI_API
Pfad: `de/meintt/util/ApiKIDeepSeek`

### KLASSE: GPTClient → GPT_Klient
Pfad: `de/meintt/util/GPTClient`

**Hauptmethoden:**
- `gibAntwort()` → `gib_Antwort()`

### KLASSE: OpenAIModelFetcher → OpenAI_Modellabholer
Pfad: `de/meintt/util/OpenAIModelFetcher`

### KLASSE: ZahlValidator → ZahlenValidator
Pfad: `de/meintt/util/ZahlValidator`

### KLASSE: DynamicCssServlet → DynamischerCSS_Servlet
Pfad: `de/meintt/util/DynamicCssServlet`

### KLASSE: JaxRsActivator → JAX_RS_Aktivator
Pfad: `de/meintt/util/JaxRsActivator`

### KLASSE: StilGenerator → StilGenerator
Pfad: `de/meintt/util/StilGenerator`

**Hauptmethoden:**
- `stilvariationen()` → `stil_Variationen()`

### KLASSE: Stilvariante → StilvariationOption
Pfad: `de/meintt/util/Stilvariante`

### KLASSE: Stil → StilDefinition
Pfad: `de/meintt/util/Stil`

### KLASSE: Fehler → FehlerInformation
Pfad: `de/meintt/util/Fehler`

### KLASSE: MatchErgebnis → WettkampfErgebnis
Pfad: `de/meintt/util/MatchErgebnis`

### KLASSE: MatchSummary → WettkampfZusammenfassung
Pfad: `de/meintt/util/MatchSummary`

### KLASSE: Bilddaten → Bildinformation
Pfad: `de/meintt/util/Bilddaten`

---

## PROVIDER/FACTORY-KLASSEN (in de/meintt/provider/)

### KLASSE: SpielergebnisProvider → SpielergebnisProvider
Pfad: `de/meintt/provider/SpielergebnisProvider`

### KLASSE: SpielplanProvider → SpielplanProvider
Pfad: `de/meintt/provider/SpielplanProvider`

### KLASSE: SpielergebnisFactory → SpielergebnisFabrik
Pfad: `de/meintt/provider/SpielergebnisFactory`

### KLASSE: SpielplanFactory → SpielplanFabrik
Pfad: `de/meintt/provider/SpielplanFactory`

### KLASSE: ShortLinkServlet → KurzLink_Servlet
Pfad: `de/meintt/provider/ShortLinkServlet`

---

## ÜBERGEORDNETE UMBENENNUNGSMUSTER

### Getter-Methoden
- `get*()` → `hole*()`
- Beispiele:
  - `getBerichtText()` → `holeBerichtText()`
  - `getDatum()` → `holeDatum()`

### Setter-Methoden
- `set*()` → `setze*()`
- Beispiele:
  - `setBerichtText()` → `setzeBerichtText()`
  - `setDatum()` → `setzeDatum()`

### Boolean-Prüfungen
- `is*()` → `ist*()`
- `has*()` → `hat*()`
- Beispiele:
  - `isSpielplan()` → `istSpielplan()`
  - `hasFreigabe()` → `hatFreigabe()`

### Service/Factory-Klassen
- `*Service` → `*Verwaltung` / `*Service`
- `*Factory` → `*Fabrik`
- Beispiele:
  - `BilanzService` → `AbrechnungService`
  - `SpielplanService` → `SpielplanVerwaltung`

### Controller-Klassen (Beans)
- `*Bean` → `*Controller` / `*Dialog`
- Beispiele:
  - `BerichtBean` → `BerichtController`
  - `ConfigBean` → `ConfigController`

### Data Transfer Objects (DTOs)
- `*Data` → `*Daten`
- `*Entry` → `*Eintrag`
- Beispiele:
  - `BerichtData` → `BerichtDaten`
  - `LogEntry` → `ProtokollEintrag`

### API-Klassen
- `Api*` → `*_API`
- `*Client` → `*_Klient`
- Beispiele:
  - `ApiKIChatGPT` → `ChatGPT_KI_API`
  - `WordPressAPIClient` → `WordPressAPI_Klient`

---

## ANWENDUNGSHINWEISE

### Refactoring-Strategie

1. **Klasse für Klasse:** Benennen Sie zuerst die Klasse um, dann die Methoden
2. **Test-Coverage:** Stellen Sie sicher, dass Tests vorhanden sind
3. **Schrittweise:** Führen Sie die Änderungen in mehreren Commits durch
4. **Abhängigkeiten überprüfen:** Nutzen Sie "Find All References" in VS Code
5. **Compilierung überprüfen:** Nach jeder Klasse kompilieren und testen

### Automatisierung mit VS Code

- Nutzen Sie **F2** zum Umbenennen von Symbolen
- Nutzen Sie **Strg+Shift+L** zur Mehrfach-Selektion
- Nutzen Sie **Find and Replace** (Strg+H) für Pattern-Ersetzungen

### Git-Best-Practices

```bash
# Feature-Branch für Refactoring erstellen
git checkout -b refactor/german-naming

# Nach Abschluss:
git commit -m "refactor: rename classes and methods to German names"
git push origin refactor/german-naming
```

---

## ZUSAMMENFASSUNG

- **Gesamte Klassen:** 72
- **Umzubenennende Klassen:** 72 (100%)
- **Geschätzte Änderungen:** ~500+ Methoden
- **Fokus:** Bessere Lesbarkeit für deutschsprachige Entwickler
- **Aufwand:** Mittelhoch (ca. 3-5 Tage mit gründlichem Testing)

---

**Erstellt:** 31. Januar 2026
**Projekt:** TischtennisBericht
**Python-Analyse-Version:** 1.0
