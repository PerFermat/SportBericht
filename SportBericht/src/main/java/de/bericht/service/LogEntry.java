package de.bericht.service;

import java.sql.Timestamp;

public class LogEntry {
	private String ergebnisLink;
	private String name;
	private String mail;
	private String mailErfolgreich;
	private String info;
	private Timestamp timestamp; // Angenommen, in der Datenbank gibt es ein Feld 'timestamp' (DEFAULT
									// CURRENT_TIMESTAMP)

	// Getter & Setter
	public String getErgebnisLink() {
		return ergebnisLink;
	}

	public void setErgebnisLink(String ergebnisLink) {
		this.ergebnisLink = ergebnisLink;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMailErfolgreich() {
		return mailErfolgreich;
	}

	public void setMailErfolgreich(String mailErfolgreich) {
		this.mailErfolgreich = mailErfolgreich;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}
