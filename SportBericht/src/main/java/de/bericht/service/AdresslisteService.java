package de.bericht.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.bericht.util.AdressEintrag;

public class AdresslisteService {
	private static final ConcurrentMap<String, List<AdressEintrag>> ADRESS_CACHE = new ConcurrentHashMap<>();
	private final DatabaseService databaseService = new DatabaseService();

	public AdresslisteService() {
	}

	public List<AdressEintrag> findAll(String vereinnr) {
		return copyListe(ADRESS_CACHE.computeIfAbsent(vereinnr, this::findAllFromDatabase));
	}

	public List<AdressEintrag> refreshCache(String vereinnr) {
		List<AdressEintrag> datenbankEintraege = findAllFromDatabase(vereinnr);
		ADRESS_CACHE.put(vereinnr, copyListe(datenbankEintraege));
		return copyListe(datenbankEintraege);
	}

	private List<AdressEintrag> findAllFromDatabase(String vereinnr) {
		return databaseService.ladeAdressEintraege(vereinnr);
	}

	public void create(String vereinnr, AdressEintrag eintrag) {
		databaseService.erstelleAdressEintrag(vereinnr, eintrag);
		if (eintrag.getId() != null) {
			ADRESS_CACHE.computeIfPresent(vereinnr, (key, cacheEintraege) -> {
				List<AdressEintrag> neueListe = copyListe(cacheEintraege);
				neueListe.add(eintrag.copy());
				neueListe.sort((links, rechts) -> vergleicheEintraege(links, rechts));
				return neueListe;
			});
		}
	}

	public void update(String vereinnr, AdressEintrag eintrag) {
		boolean aktualisiert = databaseService.aktualisiereAdressEintrag(vereinnr, eintrag);
		if (aktualisiert) {
			AdressEintrag aktualisierterEintrag = eintrag.copy();
			aktualisierterEintrag.setVereinnr(vereinnr);
			aktualisierterEintrag.setAktualisiertAm(LocalDateTime.now());
			ADRESS_CACHE.computeIfPresent(vereinnr, (key, cacheEintraege) -> {
				List<AdressEintrag> neueListe = copyListe(cacheEintraege);
				for (int i = 0; i < neueListe.size(); i++) {
					AdressEintrag cacheEintrag = neueListe.get(i);
					if (eintrag.getId() != null && eintrag.getId().equals(cacheEintrag.getId())) {
						aktualisierterEintrag.setErstelltAm(cacheEintrag.getErstelltAm());
						neueListe.set(i, aktualisierterEintrag.copy());
						break;
					}
				}
				neueListe.sort((links, rechts) -> vergleicheEintraege(links, rechts));
				return neueListe;
			});
		}
	}

	public void delete(String vereinnr, Integer id) {
		boolean geloescht = databaseService.loescheAdressEintrag(vereinnr, id);
		if (geloescht) {
			ADRESS_CACHE.computeIfPresent(vereinnr, (key, cacheEintraege) -> {
				List<AdressEintrag> neueListe = copyListe(cacheEintraege);
				neueListe.removeIf(eintrag -> id != null && id.equals(eintrag.getId()));
				return neueListe;
			});
		}
	}

	private static List<AdressEintrag> copyListe(List<AdressEintrag> eintraege) {
		List<AdressEintrag> result = new ArrayList<>();
		if (eintraege == null) {
			return result;
		}
		for (AdressEintrag eintrag : eintraege) {
			result.add(eintrag.copy());
		}
		return result;
	}

	private static int vergleicheEintraege(AdressEintrag links, AdressEintrag rechts) {
		int nachnameVergleich = vergleichWert(links.getName()).compareTo(vergleichWert(rechts.getName()));
		if (nachnameVergleich != 0) {
			return nachnameVergleich;
		}
		return vergleichWert(links.getVorname()).compareTo(vergleichWert(rechts.getVorname()));
	}

	private static String vergleichWert(String wert) {
		return wert == null ? "" : wert.toLowerCase();
	}

}
