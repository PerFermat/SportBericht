package de.bericht.util;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdressEintrag implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;
	private String vereinnr;
	private String name;
	private String vorname;
	private LocalDate geburtstag;
	private String strasse;
	private String plz;
	private String wohnort;
	private String telefonPrivat;
	private String telefonGesch;
	private String telefonMobil;
	private String emailPrivat;
	private String emailGesch;
	private String bemerkung;
	private LocalDateTime erstelltAm;
	private LocalDateTime aktualisiertAm;

	public AdressEintrag copy() {
		AdressEintrag copy = new AdressEintrag();
		copy.setId(id);
		copy.setVereinnr(vereinnr);
		copy.setName(name);
		copy.setVorname(vorname);
		copy.setGeburtstag(geburtstag);
		copy.setStrasse(strasse);
		copy.setPlz(plz);
		copy.setWohnort(wohnort);
		copy.setTelefonPrivat(telefonPrivat);
		copy.setTelefonGesch(telefonGesch);
		copy.setTelefonMobil(telefonMobil);
		copy.setEmailPrivat(emailPrivat);
		copy.setEmailGesch(emailGesch);
		copy.setBemerkung(bemerkung);
		copy.setErstelltAm(erstelltAm);
		copy.setAktualisiertAm(aktualisiertAm);
		return copy;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVorname() {
		return vorname;
	}

	public void setVorname(String vorname) {
		this.vorname = vorname;
	}

	public LocalDate getGeburtstag() {
		return geburtstag;
	}

	public void setGeburtstag(LocalDate geburtstag) {
		this.geburtstag = geburtstag;
	}

	public String getStrasse() {
		return strasse;
	}

	public void setStrasse(String strasse) {
		this.strasse = strasse;
	}

	public String getPlz() {
		return plz;
	}

	public void setPlz(String plz) {
		this.plz = plz;
	}

	public String getWohnort() {
		return wohnort;
	}

	public void setWohnort(String wohnort) {
		this.wohnort = wohnort;
	}

	public String getTelefonPrivat() {
		return telefonPrivat;
	}

	public void setTelefonPrivat(String telefonPrivat) {
		this.telefonPrivat = telefonPrivat;
	}

	public String getTelefonGesch() {
		return telefonGesch;
	}

	public void setTelefonGesch(String telefonGesch) {
		this.telefonGesch = telefonGesch;
	}

	public String getTelefonMobil() {
		return telefonMobil;
	}

	public void setTelefonMobil(String telefonMobil) {
		this.telefonMobil = telefonMobil;
	}

	public String getEmailPrivat() {
		return emailPrivat;
	}

	public void setEmailPrivat(String emailPrivat) {
		this.emailPrivat = emailPrivat;
	}

	public String getEmailGesch() {
		return emailGesch;
	}

	public void setEmailGesch(String emailGesch) {
		this.emailGesch = emailGesch;
	}

	public String getBemerkung() {
		return bemerkung;
	}

	public void setBemerkung(String bemerkung) {
		this.bemerkung = bemerkung;
	}

	public LocalDateTime getErstelltAm() {
		return erstelltAm;
	}

	public void setErstelltAm(LocalDateTime erstelltAm) {
		this.erstelltAm = erstelltAm;
	}

	public LocalDateTime getAktualisiertAm() {
		return aktualisiertAm;
	}

	public void setAktualisiertAm(LocalDateTime aktualisiertAm) {
		this.aktualisiertAm = aktualisiertAm;
	}
}
