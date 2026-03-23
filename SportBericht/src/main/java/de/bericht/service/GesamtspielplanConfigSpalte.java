package de.bericht.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GesamtspielplanConfigSpalte implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String vereinnr;
	private int spalte;
	private String ligaAnzeige;
	private String mannschaftAnzeige;
	private boolean betreuer;
	private final List<GesamtspielplanConfigMannschaft> mannschaften = new ArrayList<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getSpalte() {
		return spalte;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public void setSpalte(int spalte) {
		this.spalte = spalte;
	}

	public String getLigaAnzeige() {
		return ligaAnzeige;
	}

	public void setLigaAnzeige(String ligaAnzeige) {
		this.ligaAnzeige = ligaAnzeige;
	}

	public String getMannschaftAnzeige() {
		return mannschaftAnzeige;
	}

	public void setMannschaftAnzeige(String mannschaftAnzeige) {
		this.mannschaftAnzeige = mannschaftAnzeige;
	}

	public boolean isBetreuer() {
		return betreuer;
	}

	public void setBetreuer(boolean betreuer) {
		this.betreuer = betreuer;
	}

	public List<GesamtspielplanConfigMannschaft> getMannschaften() {
		return mannschaften;
	}
}
