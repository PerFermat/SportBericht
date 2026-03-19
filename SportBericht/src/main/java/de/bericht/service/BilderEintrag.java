package de.bericht.service;

import java.io.Serializable;

public class BilderEintrag implements Serializable {
	private static final long serialVersionUID = 1L;
	private String ergebnisLink;
	private String ueberschrift;
	private String bildUnterschrift;
	private String bildUrl;

	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	public String getUeberschrift() {
		return ueberschrift;
	}

	public void setUeberschrift(String ueberschrift) {
		this.ueberschrift = ueberschrift;
	}

	public String getBildUnterschrift() {
		return bildUnterschrift;
	}

	public void setBildUnterschrift(String bildUnterschrift) {
		this.bildUnterschrift = bildUnterschrift;
	}

	public String getBildUrl() {
		return bildUrl;
	}

	public void setBildUrl(String bildUrl) {
		this.bildUrl = bildUrl;
	}
}
