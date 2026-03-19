package de.bericht.service;

import java.io.Serializable;

public class AufstellungSpieler implements Serializable {

	private static final long serialVersionUID = 1L;

	private String vereinnr;
	private String mannschaft;
	private String rang;
	private String qttr;
	private String name;
	private String a;
	private String status;

	public String getKey() {
		return (rang == null ? "" : rang) + "|" + (name == null ? "" : name);
	}

	public String getAnzeigeText() {
		StringBuilder builder = new StringBuilder();
		if (rang != null && !rang.isBlank()) {
			builder.append(rang).append(" - ");
		}
		builder.append(name == null ? "Unbekannt" : name);
		if (qttr != null) {
			builder.append(" (QTTR ").append(qttr).append(")");
		}
		if (a != null && !a.isBlank()) {
			builder.append(" [").append(a).append("]");
		}
		if (status != null && !status.isBlank()) {
			builder.append(" {").append(status).append("}");
		}
		return builder.toString();
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public String getRang() {
		return rang;
	}

	public void setRang(String rang) {
		this.rang = rang;
	}

	public String getQttr() {
		return qttr;
	}

	public void setQttr(String qttr) {
		this.qttr = qttr;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
