package de.bericht.service;

import java.io.IOException;
import java.io.PrintWriter;

import de.bericht.util.ConfigManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/dynamic-style.css")
public class DynamicCssServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/css;charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		int hue = 204; // default
		try (PrintWriter out = resp.getWriter()) {
			// Beispiel: Hue aus Request-Parameter oder Session
			String vereinnr = req.getParameter("vereinnr");
			String hueParam = ConfigManager.getConfigValue(vereinnr, "style.farbe");
			if (hueParam != null) {
				try {
					hue = Integer.parseInt(hueParam);
					if (hue < 0 || hue > 360) {
						hue = 204;
					}
				} catch (NumberFormatException e) {
				}
			}
			out.println(":root {");
			out.println("  --base-hue: " + hue + ";");
			out.println("}");
		}
	}
}
