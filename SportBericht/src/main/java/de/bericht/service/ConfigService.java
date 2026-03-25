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

}
