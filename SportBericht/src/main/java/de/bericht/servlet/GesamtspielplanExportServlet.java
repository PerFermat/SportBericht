package de.bericht.servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import de.bericht.controller.GesamtspielplanBean;
import de.bericht.util.BerichtHelper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Liefert den Gesamtspielplan als PDF oder Excel über einen einfachen GET-Aufruf –
 * bewusst OHNE JSF-View/Session, damit der Download auch in einem Cross-Site-iframe
 * (Gesamtspielplan eingebettet auf der Vereinshomepage) auf dem Handy funktioniert.
 * Der JSF-Postback-Weg scheiterte dort, weil der Session-Cookie im iframe nicht
 * mitgesendet wird (ViewExpiredException).
 *
 * Parameter: v = Vereinsnummer/Ort, halbserie = Runde (optional), format = pdf|excel.
 * Es werden nur öffentlich einsehbare Spielplandaten erzeugt (wie die Seite selbst).
 */
@WebServlet("/gesamtspielplan-export")
public class GesamtspielplanExportServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String vParam = req.getParameter("v");
		// Ort (z. B. "hattenhofen") auf die Vereinsnummer abbilden; ist es bereits
		// eine Nummer (oder kein Ort-Treffer), direkt verwenden.
		String vereinnr = BerichtHelper.bestimmenVereinnr(vParam);
		if (vereinnr == null || vereinnr.isBlank()) {
			vereinnr = (vParam != null && !vParam.isBlank()) ? vParam : req.getParameter("vereinnr");
		}
		if (vereinnr == null || vereinnr.isBlank()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'v' (Vereinsnummer) fehlt.");
			return;
		}

		String halbserie = req.getParameter("halbserie");
		boolean excel = "excel".equalsIgnoreCase(req.getParameter("format"));

		GesamtspielplanBean bean = new GesamtspielplanBean();
		bean.initFuerExport(vereinnr, halbserie);

		byte[] daten = excel ? bean.erzeugeExcelBytes() : bean.erzeugePdfBytes();
		if (daten == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Export konnte nicht erzeugt werden.");
			return;
		}

		String datum = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String dateiname = "gesamtspielplan-" + datum + (excel ? ".xlsx" : ".pdf");

		resp.setContentType(excel ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
				: "application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"" + dateiname + "\"");
		resp.setContentLength(daten.length);
		resp.getOutputStream().write(daten);
		resp.getOutputStream().flush();
	}
}
