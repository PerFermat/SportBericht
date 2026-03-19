package de.bericht.service;

public class WordpressMedia {
	private int mediaId;
	private String url; // Die Bild-URL
	private String html; // Optional: fertiger HTML-Block für Posts

	public WordpressMedia(int mediaId, String url) {
		this.mediaId = mediaId;
		this.url = url;
	}

	public WordpressMedia(int mediaId, String url, String html) {
		this.mediaId = mediaId;
		this.url = url;
		this.html = html;
	}

	public WordpressMedia() {
		this(-1, "", "");
	}

	public int getMediaId() {
		return mediaId;
	}

	public String getUrl() {
		return url;
	}

	public String getHtml() {
		return html;
	}
}
