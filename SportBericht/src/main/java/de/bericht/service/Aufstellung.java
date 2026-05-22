package de.bericht.service;

public class Aufstellung {

	int fehlercode;
	private String mannschaft;
	private String rang;
	private String qttr;
	private String name;
	private String a;
	private String status;

	public Aufstellung(int fehlercode, String mannschaft, String rang, String qttr, String name, String a,
			String status) {
		super();
		this.fehlercode = fehlercode;
		this.mannschaft = mannschaft;
		this.rang = rang;
		this.qttr = qttr;
		this.name = name;
		this.a = a;
		this.status = status;
	}

	public int getFehlercode() {
		return fehlercode;
	}

	public String getRang() {
		return rang;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public String getQttr() {
		return qttr;
	}

	public String getName() {
		return name;
	}

	public String getA() {
		return a;
	}

	public String getStatus() {
		return status;
	}

	public void setFehlercode(int fehlercode) {
		this.fehlercode = fehlercode;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public void setQttr(String qttr) {
		this.qttr = qttr;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setA(String a) {
		this.a = a;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return rang + " " + qttr + " " + name + " " + a + " " + status;
	}

}
