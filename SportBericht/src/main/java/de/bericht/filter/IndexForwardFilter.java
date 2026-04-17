package de.bericht.filter;

import java.io.IOException;

import de.bericht.service.DatabaseSchemaInitializer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

@WebFilter("/*")
public class IndexForwardFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		DatabaseSchemaInitializer.initializeIfNeeded();
		chain.doFilter(request, response);
	}
}
