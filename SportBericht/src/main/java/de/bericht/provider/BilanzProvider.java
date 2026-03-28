package de.bericht.provider;

import java.util.List;

import de.bericht.service.Bilanz;

public interface BilanzProvider {

	List<Bilanz> getBilanz();

	String ausgabe(List<Bilanz> spiele);

}