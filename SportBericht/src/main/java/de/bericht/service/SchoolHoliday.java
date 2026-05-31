package de.bericht.service;

import java.time.LocalDate;

public class SchoolHoliday {

	public LocalDate start;
	public LocalDate end;
	private String name;

	public SchoolHoliday() {
	}

	public SchoolHoliday(LocalDate start, LocalDate end, String name) {
		this.start = start;
		this.end = end;
		this.name = name;
	}

	public LocalDate getStart() {
		return start;
	}

	public void setStart(LocalDate start) {
		this.start = start;
	}

	public LocalDate getEnd() {
		return end;
	}

	public void setEnd(LocalDate end) {
		this.end = end;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}