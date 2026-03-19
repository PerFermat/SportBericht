package de.bericht.util;

import java.io.Serializable;

public class VerfuegbarkeitEintrag implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private String mannschaft;
	private String verfuegbarkeit;
	private String kommentar;
	private boolean andereMannschaft;
	private String rang;
	private String qttr;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}

	public String getVerfuegbarkeit() {
		return verfuegbarkeit;
	}

	public void setVerfuegbarkeit(String verfuegbarkeit) {
		this.verfuegbarkeit = verfuegbarkeit;
	}

	public String getKommentar() {
		return kommentar;
	}

	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}

	public boolean isAndereMannschaft() {
		return andereMannschaft;
	}

	public void setAndereMannschaft(boolean andereMannschaft) {
		this.andereMannschaft = andereMannschaft;
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

	public String getAnzeigeName() {

		StringBuilder builder = new StringBuilder();
		if (rang != null && !rang.isBlank()) {
			builder.append(rang).append(" ");
		}
		builder.append(name == null ? "" : name);
		if (qttr != null && !qttr.isBlank()) {
			builder.append(" (QTTR ").append(qttr).append(")");
		}
		return builder.toString().trim();
	}
}