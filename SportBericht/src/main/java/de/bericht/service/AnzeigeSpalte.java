package de.bericht.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AnzeigeSpalte implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String key;
	private final String mannschaft;
	private final String liga;
	private final boolean jugend;
	private final List<String> sourceKeys;

	public AnzeigeSpalte(String key, String mannschaft, String liga, boolean jugend, List<String> sourceKeys) {
		this.key = key;
		this.mannschaft = mannschaft;
		this.liga = liga;
		this.jugend = jugend;
		this.sourceKeys = new ArrayList<>(sourceKeys);
	}

	public String getKey() {
		return key;
	}

	public String getMannschaft() {
		return mannschaft;
	}

	public String getLiga() {
		return liga;
	}

	public boolean isJugend() {
		return jugend;
	}

	public List<String> getSourceKeys() {
		return sourceKeys;
	}
}
