package de.bericht.service;

import java.io.Serializable;
import java.time.LocalDate;

public class GesamtspielplanConfigRunde implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String vereinnr;
	private String name;
	private LocalDate datumVon;
	private LocalDate datumBis;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getDatumVon() {
		return datumVon;
	}

	public void setDatumVon(LocalDate datumVon) {
		this.datumVon = datumVon;
	}

	public LocalDate getDatumBis() {
		return datumBis;
	}

	public void setDatumBis(LocalDate datumBis) {
		this.datumBis = datumBis;
	}
}
