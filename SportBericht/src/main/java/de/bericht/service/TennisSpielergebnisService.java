package de.bericht.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.bericht.util.NamensSpeicher;
import de.bericht.util.SpielDetail;
import de.bericht.util.TennisMatchSummary;
import de.bericht.util.WebCache;

public class TennisSpielergebnisService extends AbstractSpielergebnisService {

	private String vereinnr;

	public TennisSpielergebnisService(String vereinnr, String berichtMannschaft, String url, NamensSpeicher ns,
			Boolean verschluesseln) {
		this.vereinnr = vereinnr;

		try {
			Document doc = WebCache.getPage(url);
			TennisMatchSummary summary = parseSummary(berichtMannschaft, doc);

			List<TennisEinzelErgebnis> einzel = new ArrayList<>();
			List<TennisDoppelErgebnis> doppel = new ArrayList<>();
			parseMatches(doc, vereinnr, ns, verschluesseln, einzel, doppel);

			List<SpielDetail> alleSpiele = new ArrayList<>();
			alleSpiele.addAll(einzel);
			alleSpiele.addAll(doppel);

			summary.setSpiele(alleSpiele);
			setSummary(summary);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getSpielErgebnis() {
		if (!(summary instanceof TennisMatchSummary) || summary.getSpiele() == null) {
			return "";
		}

		String verein = summary.getBerichtMannschaft();
		boolean berichtIstHeim = summary.isBerichtMannschaftIstHeim();

		StringBuilder sb = new StringBuilder();
		sb.append("Für ").append(verein).append(" spielten:\n\n");

		List<?> alle = summary.getSpiele();
		List<TennisEinzelErgebnis> einzel = new ArrayList<>();
		List<TennisDoppelErgebnis> doppel = new ArrayList<>();

		for (Object o : alle) {
			if (o instanceof TennisEinzelErgebnis e) {
				einzel.add(e);
			} else if (o instanceof TennisDoppelErgebnis d) {
				doppel.add(d);
			}
		}

		if (!einzel.isEmpty()) {
			sb.append("Einzel:\n");
			for (TennisEinzelErgebnis e : einzel) {
				String spieler = berichtIstHeim ? e.getHeim() : e.getGast();
				if (spieler.toLowerCase().contains("nachgenannt")) {
					continue;
				}
				sb.append(spieler).append("\t ").append(berichtIstHeim ? e.getSatz1() : dreheErgebnis(e.getSatz1()))
						.append(" / ").append(berichtIstHeim ? e.getSatz2() : dreheErgebnis(e.getSatz2()));

				if (e.getSatz3() != null && !e.getSatz3().isBlank()) {
					sb.append(" / ").append(berichtIstHeim ? e.getSatz3() : dreheErgebnis(e.getSatz3()));
				}
				sb.append("\n");
			}
		}

		if (!doppel.isEmpty()) {
			sb.append("Doppel:\n");
			for (TennisDoppelErgebnis d : doppel) {
				String paarung = berichtIstHeim ? d.getHeimPaarungMitVornamen() : d.getGastPaarungMitVornamen();
				if (paarung.toLowerCase().contains("nachgenannt")) {
					continue;
				}

				sb.append(paarung).append("\t ").append(berichtIstHeim ? d.getSatz1() : dreheErgebnis(d.getSatz1()))
						.append(" / ").append(berichtIstHeim ? d.getSatz2() : dreheErgebnis(d.getSatz2()));

				if (d.getSatz3() != null && !d.getSatz3().isBlank()) {
					sb.append(" / ").append(berichtIstHeim ? d.getSatz3() : dreheErgebnis(d.getSatz3()));
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	private TennisMatchSummary parseSummary(String berichtMannschaft, Document doc) {
		String heimVerein = "";
		String gastVerein = "";
		String bezirk = "";
		String saison = "";
		String liga = doc.title();
		String spielBeginn = "";
		String spielEnde = "";
		String gesamtErgebnis = "";
		String gesamtSaetze = "";
		String gesamtGames = "";

		Element tabelleKopf = doc.selectFirst("table.matchResult");
		if (tabelleKopf != null) {
			Elements ths = tabelleKopf.select("th");
			for (Element th : ths) {
				if (th.hasClass("home")) {
					Element h3 = th.selectFirst("h3");
					if (h3 != null) {
						heimVerein = h3.text().trim();
					}
				} else if (th.hasClass("guest")) {
					Element h3 = th.selectFirst("h3");
					if (h3 != null) {
						gastVerein = h3.text().trim();
					}
				}
			}
		}

		Matcher saisonMatcher = Pattern.compile("(\\d{4}/\\d{2})").matcher(doc.title());
		if (saisonMatcher.find()) {
			saison = saisonMatcher.group(1);
		}

		Elements fusstabellen = doc.select("table.table-condensed tfoot");
		if (!fusstabellen.isEmpty()) {
			Element letzteFusszeile = fusstabellen.last();
			Elements tds = letzteFusszeile.select("td.text-center");

			if (tds.size() >= 3) {
				gesamtErgebnis = tds.get(0).text();
				gesamtSaetze = tds.get(1).text();
				gesamtGames = tds.get(2).text();
			}
		}

		return new TennisMatchSummary(berichtMannschaft, heimVerein, gastVerein, bezirk, saison, liga, gesamtErgebnis,
				spielBeginn, spielEnde, gesamtSaetze, gesamtGames);
	}

	private void parseMatches(Document doc, String vereinnr, NamensSpeicher ns, Boolean verschluesseln,
			List<TennisEinzelErgebnis> einzel, List<TennisDoppelErgebnis> doppel) {

		Element tabelle = doc.selectFirst("table.table-condensed");
		if (tabelle == null) {
			return;
		}

		String aktuellerModus = "EINZEL";
		Elements rows = tabelle.select("tr");

		for (Element row : rows) {
			Element th = row.selectFirst("th[colspan=8]");
			if (th != null) {
				String text = th.text().toLowerCase();
				if (text.contains("doppel")) {
					aktuellerModus = "DOPPEL";
				} else if (text.contains("einzel")) {
					aktuellerModus = "EINZEL";
				}
				continue;
			}

			Elements tds = row.select("td");
			if (tds.size() < 8 || row.hasClass("sum")) {
				continue;
			}

			String heimSpieler = normalisiereSpielername(vereinnr, ns, verschluesseln, tds.get(0).text());
			String gastSpieler = normalisiereSpielername(vereinnr, ns, verschluesseln, tds.get(1).text());
			String satz1 = tds.get(2).text();
			String satz2 = tds.get(3).text();
			String satz3 = tds.get(4).text();
			String matches = tds.get(5).text();
			String saetze = tds.get(6).text();
			String games = tds.get(7).text();

			if ("EINZEL".equals(aktuellerModus)) {
				einzel.add(new TennisEinzelErgebnis(heimSpieler, gastSpieler, satz1, satz2, satz3, matches, saetze,
						games));
			} else {
				String[] heim = splitDoppel(heimSpieler);
				String[] gast = splitDoppel(gastSpieler);
				doppel.add(new TennisDoppelErgebnis(heim[0], heim[1], gast[0], gast[1], satz1, satz2, satz3, matches,
						saetze, games));
			}
		}
	}

	private String normalisiereSpielername(String vereinnr, NamensSpeicher ns, boolean verschluesseln, String text) {
		if (!verschluesseln) {
			return text == null ? "" : text.trim();
		}
		return ns.formatName(vereinnr, text == null ? "" : text.trim(), ns);
	}

	private String[] splitDoppel(String text) {
		if (text == null || text.isBlank()) {
			return new String[] { "", "" };
		}

		String[] parts;
		if (text.contains("/")) {
			parts = text.split("\\s*/\\s*");
		} else if (text.contains("&")) {
			parts = text.split("\\s*&\\s*");
		} else {
			parts = new String[] { text, "" };
		}

		if (parts.length == 1) {
			return new String[] { parts[0].trim(), "" };
		}
		return new String[] { parts[0].trim(), parts[1].trim() };
	}

	private String dreheErgebnis(String ergebnis) {
		if (ergebnis == null || !ergebnis.contains(":")) {
			return ergebnis;
		}

		String[] teile = ergebnis.split(":");
		if (teile.length != 2) {
			return ergebnis;
		}

		return teile[1].trim() + ":" + teile[0].trim();
	}
}