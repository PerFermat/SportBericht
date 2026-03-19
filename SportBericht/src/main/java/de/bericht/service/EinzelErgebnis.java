package de.bericht.service;

public class EinzelErgebnis implements Comparable<EinzelErgebnis> {
	private String mannschaft;
	private int position;
	private String name;
	private String saetze;
	private int sieg;
	private int niederlage;

	public EinzelErgebnis(String mannschaft, String art, String name, String saetze) {

		if (art.contains("-")) {
			String[] teile = art.split("-"); // Teilt den String an "-"

			if (mannschaft.equals("HEIM")) {
				int position = Integer.parseInt(teile[0]); // Konvertiert den ersten Teil in eine Ganzzahl
				this.position = position;
			} else {
				int position = Integer.parseInt(teile[1]); // Konvertiert den zweiten Teil in eine Ganzzahl
				this.position = position;
			}
		} else {
			this.position = 0;
		}
		this.mannschaft = mannschaft;
		this.name = name;
		this.saetze = saetze;
		if (saetze.contains(":")) {
			String[] punkte = saetze.split(":");
			int heimPunkte = Integer.parseInt(punkte[0]);
			int gastPunkte = Integer.parseInt(punkte[1]);

			if (heimPunkte > gastPunkte && mannschaft.equals("HEIM")) {
				sieg = 1;
			}

			if (heimPunkte < gastPunkte && mannschaft.equals("HEIM")) {
				niederlage = 1;
			}

			if (heimPunkte < gastPunkte && mannschaft.equals("GAST")) {
				sieg = 1;
			}

			if (heimPunkte > gastPunkte && mannschaft.equals("GAST")) {
				niederlage = 1;
			}
		}
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public String getName() {
		// 1. Verwendung von split() und StringBuilder
		String[] teile = name.split(", "); // Trennt am Komma und Leerzeichen
		if (teile.length > 1) {
			String nameaufbereitet = new StringBuilder(teile[1]).append(" ").append(teile[0]).toString();
			return nameaufbereitet;
		} else {
			return name;
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSiege() {
		return sieg;
	}

	public void setSiege(int siege) {
		this.sieg = siege;
	}

	public int getNiederlagen() {
		return niederlage;
	}

	public void setNiederlagen(int niederlagen) {
		this.niederlage = niederlagen;
	}

	public void addNiederlagen(int niederlagen) {
		this.niederlage += niederlagen;
	}

	public void addSiege(int siege) {
		this.sieg += siege;
	}

	@Override
	public int compareTo(EinzelErgebnis other) {
		return Integer.compare(this.position, other.position);
	}
}
