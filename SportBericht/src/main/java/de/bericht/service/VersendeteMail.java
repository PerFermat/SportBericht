package de.bericht.service;

import java.time.LocalDateTime;

public class VersendeteMail {
	private long id;
	private LocalDateTime timestamp;
	private String empfaenger;
	private String empfaengerCc;
	private String empfaengerBcc;
	private String betreff;
	private String text;
	private String attachmentName;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getEmpfaenger() {
		return empfaenger;
	}

	public void setEmpfaenger(String empfaenger) {
		this.empfaenger = empfaenger;
	}

	public String getEmpfaengerCc() {
		return empfaengerCc;
	}

	public void setEmpfaengerCc(String empfaengerCc) {
		this.empfaengerCc = empfaengerCc;
	}

	public String getEmpfaengerBcc() {
		return empfaengerBcc;
	}

	public void setEmpfaengerBcc(String empfaengerBcc) {
		this.empfaengerBcc = empfaengerBcc;
	}

	public String getBetreff() {
		return betreff;
	}

	public void setBetreff(String betreff) {
		this.betreff = betreff;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getAttachmentName() {
		return attachmentName;
	}

	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}
}
