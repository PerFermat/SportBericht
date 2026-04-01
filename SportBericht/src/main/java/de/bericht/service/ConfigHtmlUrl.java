package de.bericht.service;

import java.io.Serializable;

public class ConfigHtmlUrl implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private Integer idHtml;
	private String ueberschrift;
	private String url;
	private String typ;
	private boolean liga;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getIdHtml() {
		return idHtml;
	}

	public void setIdHtml(Integer idHtml) {
		this.idHtml = idHtml;
	}

	public String getUeberschrift() {
		return ueberschrift;
	}

	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = ueberschrift;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTyp() {
		return typ;
	}

	public void setTyp(String typ) {
		this.typ = typ;
	}

	public boolean isLiga() {
		return liga;
	}

	public void setLiga(boolean liga) {
		this.liga = liga;
	}
}
