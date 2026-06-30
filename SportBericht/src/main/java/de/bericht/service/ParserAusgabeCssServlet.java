package de.bericht.service;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/parser-ausgabe.css")
public class ParserAusgabeCssServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		resp.setContentType("text/css;charset=UTF-8");

		try (PrintWriter out = resp.getWriter()) {
			out.print(ParserAusgabeFormatter.css());
		}
	}
}