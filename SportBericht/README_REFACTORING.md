# 📋 Refactoring-Dokumentation für TischtennisBericht

## 🎯 Zweck
Dieses Dokumentationspaket enthält umfassende Umbenennungsvorschläge für die Umstellung aller Java-Klassen und -Methoden von englischen zu deutschen Namen.

---

## 📦 Generierte Dateien

### 1. **REFACTORING_UMBENENNUNGSVORSCHLAEGE.md** (13 KB)
**Detaillierter, lesbarer Bericht für Menschen**

Inhalt:
- ✅ Alle 72 Java-Klassen organisiert nach Kategorien
- ✅ Vollständige Pfade (de/meintt/...)
- ✅ Öffentliche Methoden mit Umbenennungsvorschlägen
- ✅ Umbenennungsmuster und Best-Practices
- ✅ Refactoring-Strategien
- ✅ Git-Workflow-Anleitung
- ✅ Qualitätssicherungs-Checkliste

**Empfohlen für:** Manuelle Refactoring-Arbeit, Code-Reviews, Planung

---

### 2. **REFACTORING_UMBENENNUNGEN.csv** (79 KB)
**Strukturiertes Datenformat für Automatisierung und Tools**

Format (Semikolon-getrennt):
```
Kategorie;Alter Klassenname;Neuer Klassenname;Pfad;Alte Methode;Neue Methode;Rückgabetyp
```

Beispielzeile:
```
Controller;BerichtBean;BerichtController;controller/BerichtBean;speichern;speichern;void
```

**Empfohlen für:**
- Import in Refactoring-Tools
- Automatisierte Find-and-Replace
- Datenbank-Analysen
- Excel/Google Sheets Import

---

### 3. **REFACTORING_STATISTIKEN.txt** (6.2 KB)
**Überblick und Statistiken des Refactoring-Projekts**

Enthält:
- 📊 Gesamtstatistiken (73 Java-Dateien, 780 Methoden)
- 📦 Verteilung nach Kategorien
- 🔄 Methodentyp-Verteilung (52,6% Getter, 35,3% Setter, etc.)
- 🔗 Kritische Abhängigkeiten zum Refaktorieren
- ✅ Qualitätssicherungs-Checkliste

---

## 📊 Schnellübersicht

| Metrik | Wert |
|--------|------|
| **Gesamte Java-Dateien** | 73 |
| **Gesamte Methoden** | 780 |
| **Controller-Klassen** | 15 |
| **Service-Klassen** | 13 |
| **Utility-Klassen** | 19 |
| **Provider/Factory-Klassen** | 4 |
| **Geschätzte Bearbeitungszeit** | 3-5 Tage |

---

## 🔄 Umbenennungsmuster

| Muster | Alt → Neu | Beispiel |
|--------|-----------|----------|
| **Getter** | `get*()` → `hole*()` | `getName()` → `holeName()` |
| **Setter** | `set*()` → `setze*()` | `setName()` → `setzeName()` |
| **Boolean** | `is*()` → `ist*()` | `isValid()` → `istValid()` |
| **Has** | `has*()` → `hat*()` | `hasAccess()` → `hatZugriff()` |
| **Service** | `*Service` → `*Verwaltung` | `BilanzService` → `AbrechnungService` |
| **Bean** | `*Bean` → `*Controller` | `BerichtBean` → `BerichtController` |
| **Entity** | `*Data` → `*Daten` | `BerichtData` → `BerichtDaten` |

---

## 🚀 Empfohlene Vorgehensweise

### Phase 1: Vorbereitung
```bash
# Feature-Branch erstellen
git checkout -b refactor/german-naming

# Lokale Tests durchführen
mvn clean test
```

### Phase 2: Kern-Klassen refaktorieren
1. **ConfigManager** (viel verwendet)
2. **NamensSpeicher** (Service-Abhängigkeit)
3. **DatabaseService** (zentrale Service)
4. **BerichtHelper** (Hilfsfunktionen)

### Phase 3: Refactoring pro Kategorie
1. Utility-Klassen (easiest)
2. Service-Klassen (medium)
3. Controller-Klassen (hard)

### Phase 4: Verifizierung
```bash
# Nach jeder Klasse
mvn clean compile

# Nach jeder Kategorie
mvn clean test

# Vor dem Merge
mvn clean verify
```

---

## 🔍 Wichtige Klassen zum Umbenennen

### 1. **ConfigManager** → **KonfigurationsverwaltungStatisch**
```java
// Alt
ConfigManager.getConfigValue(vereinnr, "key");

// Neu
KonfigurationsverwaltungStatisch.hole_Konfigurationswert(vereinnr, "key");
```

### 2. **BerichtBean** → **BerichtController**
```java
// Alt
public String speichernUndWeiter() { ... }

// Neu
public String speichernUndWeiter() { ... }  // Funktioniert gleich, aber neuer Klassenname
```

### 3. **BilanzService** → **AbrechnungService**
```java
// Alt
List<Bilanz> bilanz = bilanzService.getBilanz(...);

// Neu
List<Abrechnung> abrechnung = abrechnungService.hole_Abrechnung(...);
```

---

## ✅ Qualitätssicherungs-Checkliste

- [ ] **Kompilation** erfolgreich: `mvn clean compile`
- [ ] **Unit-Tests** bestanden: `mvn test`
- [ ] **Integrations-Tests** bestanden
- [ ] **Keine Warnings** in IDE
- [ ] **Code-Review** durchgeführt
- [ ] **Javadoc** aktualisiert
- [ ] **Breaking Changes** dokumentiert
- [ ] **Abhängigkeiten** überprüft

---

## 📚 Verwendete Ressourcen

### CSV-Import (für Excel/Sheets):
- Trennzeichen: **Semikolon** (;)
- Zeichensatz: **UTF-8**
- Spalten: 7 (Kategorie, OldClass, NewClass, Path, OldMethod, NewMethod, ReturnType)

### Markdown (zum Lesen):
- Gut für: Detaillierte Planung, Code-Reviews
- Browser-Anzeige: ✅ GitHub, GitLab, etc.

---

## 🎓 Best Practices für Refactoring

1. **Nutzen Sie IDE-Refactoring-Tools**
   - VS Code: F2 zum Umbenennen
   - IntelliJ: Refactor → Rename

2. **Immer kompilieren und testen**
   - Nach jeder Klasse testen
   - Vor dem Commit kompilieren

3. **Kleine, fokussierte Commits**
   ```bash
   git commit -m "refactor: rename ConfigManager to KonfigurationsverwaltungStatisch"
   ```

4. **Regelmäßiger Merge mit Main**
   - Reduziert Merge-Konflikte

5. **Code-Review einplanen**
   - Mindestens 2 Reviewer

---

## 📞 Fragen & Support

Bei Fragen zum Refactoring:
1. Prüfen Sie die Umbenennungsmuster in **REFACTORING_STATISTIKEN.txt**
2. Schauen Sie in **REFACTORING_UMBENENNUNGSVORSCHLAEGE.md** unter "Best Practices"
3. Überprüfen Sie die spezifische Klasse in der CSV-Datei

---

## 📅 Timeline-Beispiel

| Tag | Aufgabe | Klassen |
|-----|---------|---------|
| **Tag 1** | Utility-Klassen | ConfigManager, NamensSpeicher, etc. |
| **Tag 2** | Service-Klassen Core | DatabaseService, ConfigService |
| **Tag 3** | Service-Klassen Rest | BilanzService, SpielplanService, etc. |
| **Tag 4** | Controller-Klassen | BerichtBean, ConfigBean, etc. |
| **Tag 5** | Testen, Dokumentation, Merge | Complete verification & PR |

---

**Generiert:** 31. Januar 2026  
**Projekt:** TischtennisBericht  
**Status:** Ready for Refactoring  
**Version:** 1.0
