package de.bericht.util;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MatchErgebnis {

	@JsonProperty("BerichtSpieler")
	private String berichtSpieler;

	@JsonProperty("Position Heim-Postition Gast")
	private String position;

	@JsonProperty("Heim")
	private String heim;

	@JsonProperty("Gast")
	private String gast;

	@JsonProperty("S1")
	private String s1;

	@JsonProperty("S2")
	private String s2;

	@JsonProperty("S3")
	private String s3;

	@JsonProperty("S4")
	private String s4;

	@JsonProperty("S5")
	private String s5;

	@JsonProperty("Sätze")
	private String saetze;

	@JsonProperty("Gesamt")
	private String gesamt;

	@JsonProperty("Gewinner")
	private String gewinner;

	@JsonProperty("BerichtSpieler_hat_gewonnen")
	private boolean berichtSpielerHatGewonnen;

	// Konstruktor mit BerichtMannschaft
	public MatchErgebnis(boolean istHeim, String position, String heim, String gast, String s1, String s2, String s3,
			String s4, String s5, String saetze, String gesamt) {
		if (istHeim) {
			this.berichtSpieler = heim;
		} else {
			this.berichtSpieler = gast;
		}
		this.position = position;
		this.heim = heim;
		this.gast = gast;
		this.s1 = s1;
		this.s2 = s2;
		this.s3 = s3;
		this.s4 = s4;
		this.s5 = s5;
		this.saetze = saetze;
		this.gesamt = gesamt;

		auswerten(istHeim);
	}

	public MatchErgebnis() {
	}

	public void auswerten(boolean istHeim) {
		try {
			String[] parts = saetze.split(":");
			int satzHeim = Integer.parseInt(parts[0].trim());
			int satzGast = Integer.parseInt(parts[1].trim());

			if (satzHeim > satzGast) {
				gewinner = "Heim";
			} else if (satzGast > satzHeim) {
				gewinner = "Gast";
			} else {
				gewinner = "Unentschieden";
			}

			// Berichts-Mannschaft kann Heim oder Gast sein
			if (berichtSpieler != null) {

				if ((gewinner.equals("Heim") && istHeim) || (gewinner.equals("Gast") && !istHeim)) {
					berichtSpielerHatGewonnen = true;
				} else {
					berichtSpielerHatGewonnen = false;
				}
			} else {
				berichtSpielerHatGewonnen = false;
			}

		} catch (Exception e) {
			gewinner = "Unbekannt";
			berichtSpielerHatGewonnen = false;
		}
	}

	// Getter & Setter
	public String toJson() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(this);
	}

	public String getBerichtSpieler() {
		return berichtSpieler;
	}

	public String getPosition() {
		return position;
	}

	public String getHeim() {
		return heim;
	}

	public String getGast() {
		return gast;
	}

	public String getS1() {
		return s1;
	}

	public String getS2() {
		return s2;
	}

	public String getS3() {
		return s3;
	}

	public String getS4() {
		return s4;
	}

	public String getS5() {
		return s5;
	}

	public String getSaetze() {
		return saetze;
	}

	public String getGesamt() {
		return gesamt;
	}

	public String getGewinner() {
		return gewinner;
	}

	public boolean isBerichtSpielerHatGewonnen() {
		return berichtSpielerHatGewonnen;
	}

	public void setBerichtSpieler(String berichtSpieler) {
		this.berichtSpieler = berichtSpieler;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public void setHeim(String heim) {
		this.heim = heim;
	}

	public void setGast(String gast) {
		this.gast = gast;
	}

	public void setS1(String s1) {
		this.s1 = s1;
	}

	public void setS2(String s2) {
		this.s2 = s2;
	}

	public void setS3(String s3) {
		this.s3 = s3;
	}

	public void setS4(String s4) {
		this.s4 = s4;
	}

	public void setS5(String s5) {
		this.s5 = s5;
	}

	public void setSaetze(String saetze) {
		this.saetze = saetze;
	}

	public void setGesamt(String gesamt) {
		this.gesamt = gesamt;
	}

	public void setGewinner(String gewinner) {
		this.gewinner = gewinner;
	}

	public void setBerichtSpielerHatGewonnen(boolean berichtSpielerHatGewonnen) {
		this.berichtSpielerHatGewonnen = berichtSpielerHatGewonnen;
	}

}
