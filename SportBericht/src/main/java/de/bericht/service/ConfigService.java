package de.bericht.service;

import java.util.List;
import java.util.Map;

import de.bericht.util.ConfigBedeutung;
import de.bericht.util.ConfigEintrag;
import de.bericht.util.ConfigKategorie;



public class ConfigService {

	DatabaseService db = new DatabaseService();

	public List<String> ladeAlleVereine() {
		return db.ladeAlleVereine();
	}

	public List<ConfigEintrag> ladeConfigEintraege(String vereinnr) {
		return db.ladeConfigEintraege(vereinnr);
	}

	public void speichereConfigEintraege(String vereinnr, List<ConfigEintrag> eintraege) {
		db.speichereConfigEintraege(vereinnr, eintraege);
	}
	

	public Map<String, ConfigBedeutung> ladeConfigBedeutungen() {
		return db.ladeConfigBedeutungen();
	}

	public List<ConfigKategorie> ladeConfigKategorien() {
		return db.ladeConfigKategorien();
	}
	public void upsertConfigBedeutung(String configEintrag, String bedeutung, String inhaltformat, String wertebereich) {
		db.upsertConfigBedeutung(configEintrag, bedeutung, inhaltformat, wertebereich);
	}

	public void replaceConfigKategorien(String configEintrag, List<String> kategorien) {
		db.replaceConfigKategorien(configEintrag, kategorien);
	}

	public void insertOrUpdateConfigEintrag(String vereinnr, String eintrag, String wert) {
		db.insertOrUpdateConfigEintrag(vereinnr, eintrag, wert);
	}

}
