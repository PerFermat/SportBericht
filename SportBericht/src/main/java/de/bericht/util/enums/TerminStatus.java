package de.bericht.util.enums;

import jakarta.faces.model.SelectItem;

public enum TerminStatus {
	TRAININGSAUSFALLJA("Kein Jugend- und Aktiventraining"), TRAININGSAUSFALLJ("Kein Jugendtraining"),
	TRAININGSAUSFALLA("Kein Aktiventraining"), TRAINING("Training findet statt"), TERMINNEU("Termin eintragen"),
	TRAINING_NORMAL("Training normal"), TERMINOK("Termin ist eingetragen"), TERMINFEHLT("Termin fehlt"),
	NICHT_RELEVANT("Nicht relevant"), UEBERPRUEFE("Überprüfe"), SPIELTAG_OK("Spieltag OK"),
	SPIELTAG_KRITISCH("Spieltag kritisch"), HALLE_FREIGEBEN("Halle freigeben"), IGNORIEREN("ignorieren");

	private final String label;

	TerminStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public SelectItem toSelectItem() {
		return new SelectItem(label);
	}
}