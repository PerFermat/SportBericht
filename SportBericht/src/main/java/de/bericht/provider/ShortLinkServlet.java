package de.bericht.provider;

import java.io.IOException;

import de.bericht.service.DatabaseService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/l/*")
public class ShortLinkServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		// /abc123
		String pathInfo = req.getPathInfo();

		if (pathInfo == null || pathInfo.length() <= 1) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Kein Code angegeben");
			return;
		}

		String code = pathInfo.substring(1);

		String target = loadTargetFromDatabase(code);

		if (target == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Link nicht gefunden");
			return;
		}

		// 🔒 Sicherheit: keine externen Redirects
		if (target.startsWith("http://") || target.startsWith("https://")) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Externe URLs verboten");
			return;
		}

		// Attribute setzen, um den Ziel-Code serverseitig weiterzugeben
		req.setAttribute("target", target);

		// Serverseitiges Forward auf die Zielseite (URL bleibt gleich)
		RequestDispatcher dispatcher = req.getRequestDispatcher("/" + target);
		try {
			dispatcher.forward(req, resp);
		} catch (ServletException e) {
			throw new IOException("Fehler beim Forwarding", e);
		}
	}

	/**
	 * Prüft, ob die Anfrage von localhost kommt.
	 */
	private boolean isLocalhost(HttpServletRequest req) {
		String host = req.getServerName();
		return host.equals("localhost") || host.equals("127.0.0.1");
	}

	private String loadTargetFromDatabase(String code) {
		// JPA / JDBC / Service
		DatabaseService dbService = new DatabaseService();
		return dbService.loadTargetUrl(code);
	}
}
