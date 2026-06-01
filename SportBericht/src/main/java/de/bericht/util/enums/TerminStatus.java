package de.bericht.util.enums;

import jakarta.faces.model.SelectItem;

public enum TerminStatus {
	TRAININGSAUSFALL("Trainingsausfall eingetragen"), TRAINING("Training eingetragen"),
	TRAINING_NORMAL("Training normal"), TERMIN("Termin eingetragen"), NICHT_RELEVANT("Nicht relevant"),
	UEBERPRUEFE("Überprüfe"), SPIELTAG_OK("Spieltag OK"), SPIELTAG_KRITISCH("Spieltag kritisch"),
	HALLE_FREIGEBEN("Halle freigeben"), IGNORIEREN("ignorieren");

	private final String label;

	TerminStatus(String label) {
		this.label = label;
	}

	public SelectItem toSelectItem() {
		return new SelectItem(label);
	}
}