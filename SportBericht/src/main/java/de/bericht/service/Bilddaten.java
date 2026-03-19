package de.bericht.service;

public class Bilddaten {
	private String bildName;
	private String bildLink;
	private byte[] bildDaten;
	private String bildUnterschrift;
	private String bildFormat = "image/jpeg";
	private WordpressMedia mediaId;

	public String getBildFormat() {
		return bildFormat;
	}

	public String getBildName() {
		return bildName;
	}

	public String getBildLink() {
		return bildLink;
	}

	public byte[] getBildDaten() {
		return bildDaten;
	}

	public String getBildUnterschrift() {
		return bildUnterschrift;
	}

	public void setBildLink(String bildLink) {
		this.bildLink = bildLink;
	}

	public void setBildName(String bildName) {
		this.bildName = bildName;
	}

	public void setBildDaten(byte[] bildDaten) {
		this.bildDaten = bildDaten;
	}

	public void setBildUnterschrift(String bildUnterschrift) {
		this.bildUnterschrift = bildUnterschrift;
	}

	public WordpressMedia getMediaId() {
		return mediaId;
	}

	public void setMediaId(WordpressMedia mediaId) {
		this.mediaId = mediaId;
	}
}
