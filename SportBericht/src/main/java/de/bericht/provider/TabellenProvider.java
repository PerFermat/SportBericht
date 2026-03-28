package de.bericht.provider;

import java.util.List;

import de.bericht.service.Tabelle;

public interface TabellenProvider {

	List<Tabelle> getTabelle();

	String ausgabe(List<Tabelle> spiele);

}