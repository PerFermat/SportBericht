package de.bericht.service;

import java.io.Serializable;

public class FtpManuellerTagEintrag implements Serializable {
	private static final long serialVersionUID = 1L;
	private String tag;
	private String text;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}