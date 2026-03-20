package de.bericht.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
@RequestScoped
public class ErrorBean {

	private final Throwable exception;

	public ErrorBean() {
		this.exception = resolveException();
	}

	private Throwable resolveException() {
		FacesContext fc = FacesContext.getCurrentInstance();
		if (fc == null || fc.getExternalContext() == null) {
			return null;
		}

		Object ex = fc.getExternalContext().getRequestMap().get("org.omnifaces.exceptionhandler.exception");
		if (ex instanceof Throwable throwable) {
			return throwable;
		}

		Object request = fc.getExternalContext().getRequest();
		if (request instanceof HttpServletRequest servletRequest) {
			Object jakartaEx = servletRequest.getAttribute("jakarta.servlet.error.exception");
			if (jakartaEx instanceof Throwable throwable) {
				return throwable;
			}

			Object javaxEx = servletRequest.getAttribute("javax.servlet.error.exception");
			if (javaxEx instanceof Throwable throwable) {
				return throwable;
			}
		}

		return null;
	}

	public boolean isExceptionAvailable() {
		return exception != null;
	}

	public String getMessage() {
		return exception != null ? exception.getMessage() : "";
	}

	public String getType() {
		return exception != null ? exception.getClass().getName() : "";
	}

	public String getStacktrace() {
		if (exception == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw));
		return sw.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
	}

}