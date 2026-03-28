package de.bericht.service;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "rang", "name", "lk", "nation", "einzel", "doppel", "gesamt" })
public class TennisBilanz extends Bilanz {
	private String lk;
	private String nation;
	private String einzel;
	private String doppel;

	public TennisBilanz(String rang, String lk, String name, String nation, String einzel, String doppel,
			String gesamt) {
		this.rang = rang;
		this.lk = lk;
		this.name = name;
		this.nation = nation;
		this.einzel = einzel;
		this.doppel = doppel;
		this.gesamt = gesamt;
	}

	public String getLk() {
		return lk;
	}

	public String getNation() {
		return nation;
	}

	public String getEinzel() {
		return einzel;
	}

	public String getDoppel() {
		return doppel;
	}

	public void setLk(String lk) {
		this.lk = lk;
	}

	public void setNation(String nation) {
		this.nation = nation;
	}

	public void setEinzel(String einzel) {
		this.einzel = einzel;
	}

	public void setDoppel(String doppel) {
		this.doppel = doppel;
	}

}
