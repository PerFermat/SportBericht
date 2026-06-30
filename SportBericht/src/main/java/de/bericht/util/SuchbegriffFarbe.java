package de.bericht.util;

import java.util.List;

import de.bericht.util.enums.SuchbegriffCss;
import de.bericht.util.enums.TerminStatus;

public class SuchbegriffFarbe {

	private final String suchbegriff;
	private final String bedingung;
	private final String cssClass;
	private final SuchbegriffCss farbe;
	private final List<TerminStatus> erlaubteStatus;

	public SuchbegriffFarbe(String suchbegriff, String bedingung, String cssClass, SuchbegriffCss farbe,
			List<TerminStatus> erlaubteStatus) {

		this.suchbegriff = suchbegriff;
		this.bedingung = bedingung;
		this.cssClass = cssClass;
		this.farbe = farbe;
		this.erlaubteStatus = erlaubteStatus;
	}

	public String getCssDefinition() {
		return farbe.createCss(cssClass);
	}

	public String getCssClass() {
		return cssClass;
	}

	public String getSuchbegriff() {
		return suchbegriff;
	}

	public String getBedingung() {
		return bedingung;
	}

	public SuchbegriffCss getFarbe() {
		return farbe;
	}

	public List<TerminStatus> getErlaubteStatus() {
		return erlaubteStatus;
	}

}