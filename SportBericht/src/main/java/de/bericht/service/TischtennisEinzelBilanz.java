package de.bericht.service;

public class TischtennisEinzelBilanz implements Comparable<TischtennisEinzelBilanz> {

	private String mannschaft;
	private int position;
	private String name;
	private String saetze;
	private int sieg;
	private int niederlage;

	public TischtennisEinzelBilanz(String mannschaft, String art, String name, String saetze) {
		if (art.contains("-")) {
			String[] teile = art.split("-");
			if ("HEIM".equals(mannschaft)) {
				this.position = Integer.parseInt(teile[0]);
			} else {
				this.position = Integer.parseInt(teile[1]);
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

			if (heimPunkte > gastPunkte && "HEIM".equals(mannschaft)) {
				sieg = 1;
			}
			if (heimPunkte < gastPunkte && "HEIM".equals(mannschaft)) {
				niederlage = 1;
			}
			if (heimPunkte < gastPunkte && "GAST".equals(mannschaft)) {
				sieg = 1;
			}
			if (heimPunkte > gastPunkte && "GAST".equals(mannschaft)) {
				niederlage = 1;
			}
		}
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public String getName() {
		String[] teile = name.split(", ");
		if (teile.length > 1) {
			return teile[1] + " " + teile[0];
		}
		return name;
	}

	public int getSiege() {
		return sieg;
	}

	public int getNiederlagen() {
		return niederlage;
	}

	public void addNiederlagen(int niederlagen) {
		this.niederlage += niederlagen;
	}

	public void addSiege(int siege) {
		this.sieg += siege;
	}

	@Override
	public int compareTo(TischtennisEinzelBilanz other) {
		return Integer.compare(this.position, other.position);
	}
}