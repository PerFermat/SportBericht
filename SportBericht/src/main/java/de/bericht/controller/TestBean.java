package de.bericht.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class TestBean {

	public String test() {
		throw new RuntimeException("RuntimeException Test");
	}
}
