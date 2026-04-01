package de.bericht.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConfigHtmlModel implements Serializable {
	private static final long serialVersionUID = 1L;
	private Integer id;
	private String art;
	private String dateiname;
	private boolean expanded;
	private final List<ConfigHtmlUrlModel> urls = new ArrayList<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public List<ConfigHtmlUrlModel> getUrls() {
		return urls;
	}
}