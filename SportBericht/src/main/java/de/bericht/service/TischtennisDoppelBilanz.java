package de.bericht.service;

public class TischtennisDoppelBilanz implements Comparable<TischtennisDoppelBilanz> {

	private String mannschaft;
	private int position;
	private String name;
	private String saetze;
	private int sieg;
	private int niederlage;

	public TischtennisDoppelBilanz(String mannschaft, String art, String name, String saetze) {
		this.mannschaft = mannschaft;

		if (art.contains("-")) {
			String[] teile = art.split("-");
			if ("HEIM".equals(mannschaft)) {
				this.position = Integer.parseInt(teile[0].substring(1));
			} else {
				this.position = Integer.parseInt(teile[1].substring(1));
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
		String[] teile = name.split(" / ");
		String[] nachnamen = new String[teile.length];

		for (int i = 0; i < teile.length; i++) {
			String[] nameTeile = teile[i].split(", ");
			nachnamen[i] = nameTeile.length > 0 ? nameTeile[0] : teile[i];
		}

		return String.join("/", nachnamen);
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
	public int compareTo(TischtennisDoppelBilanz other) {
		return Integer.compare(this.position, other.position);
	}
}