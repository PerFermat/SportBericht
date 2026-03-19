package de.bericht.service;

public class DoppelErgebnis implements Comparable<DoppelErgebnis> {
	private String mannschaft;
	private int position;
	private String name;
	private String saetze;
	private int sieg;
	private int niederlage;

	public DoppelErgebnis(String mannschaft, String art, String name, String saetze) {
		this.mannschaft = mannschaft;
		if (art.contains("-")) {
			String[] teile = art.split("-");
			if (mannschaft.equals("HEIM")) {
				int position = Integer.parseInt(teile[0].substring(1)); // Entfernt das "D" aus dem ersten Teil
				this.position = position;
			} else {
				int position = Integer.parseInt(teile[1].substring(1)); // Entfernt das "D" aus dem ersten Teil
				this.position = position;
			}
		} else {
			this.position = 0;
		}
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

	public String getNameAlt() {
		String[] teile = name.split(" / "); // Trennt am "/ "
		String[] nachnamen = new String[teile.length];
		for (int i = 0; i < teile.length; i++) {

			nachnamen[i] = teile[i].split(" ")[teile.length - 1]; // Extrahiert den Nachnamen
		}
		String neuerName = String.join("/", nachnamen); // Fügt die Nachnamen mit "/" zusammen

		return neuerName;
	}

	public String getName() {
		String[] teile = name.split(" / "); // Trennt am "/ "
		String[] nachnamen = new String[teile.length];
		for (int i = 0; i < teile.length; i++) {

			nachnamen[i] = teile[i].split(", ")[0]; // Extrahiert den Nachnamen
		}
		String neuerName = String.join("/", nachnamen); // Fügt die Nachnamen mit "/" zusammen

		return neuerName;
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
	public int compareTo(DoppelErgebnis other) {
		return Integer.compare(this.position, other.position);
	}
}
