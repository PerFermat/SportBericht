package de.bericht.filter;

import java.io.IOException;

import de.bericht.service.DatabaseSchemaInitializer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

@WebFilter("/*")
public class IndexForwardFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		DatabaseSchemaInitializer.initializeIfNeeded();

		if (request instanceof HttpServletRequest httpRequest && httpRequest.getHeader("X-Forwarded-Proto") != null
				&& httpRequest.getContextPath() != null && !httpRequest.getContextPath().isBlank()) {
			HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
				@Override
				public String getContextPath() {
					return "";
				}
			};
			chain.doFilter(wrappedRequest, response);
			return;
		}

		chain.doFilter(request, response);
	}
}