package de.bericht.provider;

import java.io.IOException;
import java.util.List;

import de.bericht.util.MatchErgebnis;
import de.bericht.util.MatchSummary;

public interface SpielergebnisProvider {

	String getSpielErgebnis();

	String summaryToJson() throws IOException;

	String listToJson(List<MatchErgebnis> matches) throws IOException;

	MatchSummary getSummary();

}
