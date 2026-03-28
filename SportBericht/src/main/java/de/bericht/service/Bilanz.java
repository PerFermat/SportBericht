package de.bericht.service;

public abstract class Bilanz implements Comparable<Bilanz> {

	protected String rang;
	protected String name;
	protected String gesamt;

	@Override
	public int compareTo(Bilanz arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getRang() {
		return rang;
	}

	public String getName() {
		return name;
	}

	public String getGesamt() {
		return gesamt;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setGesamt(String gesamt) {
		this.gesamt = gesamt;
	}

}
