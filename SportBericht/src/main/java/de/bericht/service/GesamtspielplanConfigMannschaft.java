package de.bericht.service;

import java.io.Serializable;

public class GesamtspielplanConfigMannschaft implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String vereinnr;
	private Integer idSpalte;
	private String liga;
	private String mannschaft;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getIdSpalte() {
		return idSpalte;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public void setIdSpalte(Integer idSpalte) {
		this.idSpalte = idSpalte;
	}

	public String getLiga() {
		return liga;
	}

	public void setLiga(String liga) {
		this.liga = liga;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public void setMannschaft(String mannschaft) {
		this.mannschaft = mannschaft;
	}
}
