#!/usr/bin/env bash
#
# Erzeugt aus dem Anwenderhandbuch (HTML) das PDF.
# Aufruf: ./pdf-erzeugen.sh   (aus einem beliebigen Verzeichnis)
#
# Benötigt: weasyprint  (Installation: pip install weasyprint)

set -euo pipefail

# Immer relativ zum Verzeichnis dieses Skripts arbeiten
cd "$(dirname "$0")"

HTML="anwenderhandbuch.html"
PDF="anwenderhandbuch.pdf"

if ! command -v weasyprint >/dev/null 2>&1; then
	echo "Fehler: 'weasyprint' ist nicht installiert." >&2
	echo "Installation z. B. mit: pip install weasyprint" >&2
	exit 1
fi

if [ ! -f "$HTML" ]; then
	echo "Fehler: $HTML nicht gefunden (Skript im Handbuch-Verzeichnis ausführen)." >&2
	exit 1
fi

echo "Erzeuge $PDF aus $HTML ..."
weasyprint "$HTML" "$PDF"
echo "Fertig: $(pwd)/$PDF"
