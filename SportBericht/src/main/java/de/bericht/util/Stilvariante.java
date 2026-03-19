package de.bericht.util;

import java.util.List;

class Stilvariante {
	public String variante;
	public int berichtnr;
	public List<Stil> stile;

	public Stilvariante(String variante, int berichtnr, List<Stil> stile) {
		this.variante = variante;
		this.berichtnr = berichtnr;
		this.stile = stile;
	}

}