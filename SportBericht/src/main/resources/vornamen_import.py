import mysql.connector

# 🔧 KONFIGURATION – hier deine Zugangsdaten anpassen
DB_CONFIG = {
    'host': '87.106.133.140',
    'user': 'michaelremote',
    'passwort': 'Neue$K0ll3genKur$B1sl@ng',
    'database': 'TischtennisBericht',
    'port': 3306  # Optional, Standardport für MariaDB
}

def namen_aus_datei(dateiname, geschlecht):
    with open(dateiname, 'r', encoding='utf-8') as file:
        return [(zeile.strip(), geschlecht) for zeile in file if zeile.strip()]

def importiere_namen(namen_liste):
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        sql = "INSERT IGNORE INTO Vorname (Name, Geschlecht) VALUES (%s, %s)"
        cursor.executemany(sql, namen_liste)
        conn.commit()

        print(f"{cursor.rowcount} Namen wurden eingefügt.")
    except mysql.connector.Error as err:
        print(f"Fehler: {err}")
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    weiblich = namen_aus_datei("weiblich.txt", "w")
    maennlich = namen_aus_datei("maennlich.txt", "m")

    alle_namen = weiblich + maennlich
    importiere_namen(alle_namen)
