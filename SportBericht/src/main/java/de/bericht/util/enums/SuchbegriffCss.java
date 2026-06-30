package de.bericht.util.enums;

public enum SuchbegriffCss {

	HALLE("#ffebee", "#c62828"), GELB("#fff8e1", "#ff8f00"), TISCHTENNIS("#e8f5e9", "#2e7d32"),
	HEIMSPIEL("#e3f2fd", "#1565c0"), MANUELL("#D1D1D1", "#595959");

	private final String backgroundColor;
	private final String borderColor;

	SuchbegriffCss(String backgroundColor, String borderColor) {
		this.backgroundColor = backgroundColor;
		this.borderColor = borderColor;
	}

	public String createCss(String cssClass) {
		return "." + cssClass + "{background-color:" + backgroundColor + ";border-left:6px solid " + borderColor + "}";
	}

}
