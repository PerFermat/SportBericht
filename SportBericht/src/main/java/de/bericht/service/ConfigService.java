package de.bericht.service;

import java.util.List;

import de.bericht.util.ConfigEintrag;



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
}
