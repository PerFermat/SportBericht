package de.bericht.service;

import java.io.Serializable;

public class FtpTerminEintrag implements Serializable {
	private static final long serialVersionUID = 1L;
	private String status = "Überprüfe";
	private String datum;
	private String text;

	public FtpTerminEintrag(String d, String t) {
		datum = d;
		text = t;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDatum() {
		return datum;
	}

	public String getText() {
		return text;
	}
}
