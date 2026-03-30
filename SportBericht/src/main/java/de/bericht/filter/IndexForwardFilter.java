package de.bericht.filter;

import java.io.IOException;

import de.bericht.service.DatabaseSchemaInitializer;
import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter("/*")
public class IndexForwardFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		DatabaseSchemaInitializer.initializeIfNeeded();

		String uri = req.getRequestURI();
		String contextPath = req.getContextPath();

		boolean startseitenAufruf = uri.equals(contextPath + "/") || uri.equals(contextPath + "/index.xhtml");

		if (!startseitenAufruf) {
			chain.doFilter(request, response);
			return;
		}

		String name = req.getParameter("v");
		String vereinnr = BerichtHelper.bestimmenVereinnr(name);

		boolean vereinPerParameterAngegeben = name != null && !name.isBlank();

		if (vereinPerParameterAngegeben && vereinnr == null && req.getParameter("vereinnr") == null) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("/fehlenderVerein.xhtml");
			dispatcher.forward(req, resp);
			return;
		}

		if (vereinnr == null) {
			vereinnr = req.getParameter("vereinnr");
		}
		if (vereinnr == null) {
			vereinnr = "13014";
		}

		String url = ConfigManager.getSpielplanURL(vereinnr);

		if (url == null || !url.contains("wtb-tennis")) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("/spielplan.xhtml?v=" + name);
			dispatcher.forward(req, resp);
			return;
		}

		chain.doFilter(request, response);
	}
}