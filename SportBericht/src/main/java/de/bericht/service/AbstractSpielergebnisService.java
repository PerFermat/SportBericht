package de.bericht.service;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.bericht.provider.SpielergebnisProvider;
import de.bericht.util.MatchSummary;

public abstract class AbstractSpielergebnisService implements SpielergebnisProvider {

	protected MatchSummary summary;
	protected final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String summaryToJson() throws IOException {
		return objectMapper.writeValueAsString(summary);
	}

	@Override
	public String listToJson(List<?> matches) throws IOException {
		return objectMapper.writeValueAsString(matches);
	}

	@Override
	public MatchSummary getSummary() {
		return summary;
	}

	protected void setSummary(MatchSummary summary) {
		this.summary = summary;
	}
}