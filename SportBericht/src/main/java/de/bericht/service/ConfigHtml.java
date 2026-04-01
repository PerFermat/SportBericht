package de.bericht.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConfigHtml implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String vereinnr;
	private String art;
	private String dateiname;
	private final List<ConfigHtmlUrl> urls = new ArrayList<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getArt() {
		return art;
	}

	public void setArt(String art) {
		this.art = art;
	}

	public String getDateiname() {
		return dateiname;
	}

	public void setDateiname(String dateiname) {
		this.dateiname = dateiname;
	}

	public List<ConfigHtmlUrl> getUrls() {
		return urls;
	}
}
