package de.bericht.util.enums;

public enum KiSystem {

	CHATGPT("chatgpt"), DEEPSEEK("deepseek"), CLAUDE("claude"), GEMINI("gemini");

	private final String prefix;

	KiSystem(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

	public String formatModel(String model) {
		return prefix + ":" + model;
	}

	public static KiSystem fromModel(String model) {
		if (model == null) {
			return CHATGPT;
		}

		String lower = model.toLowerCase();

		for (KiSystem system : values()) {
			if (lower.startsWith(system.prefix + ":")) {
				return system;
			}
		}

		return CHATGPT;
	}

	public static String extractPureModel(String model) {
		if (model == null) {
			return "";
		}

		int idx = model.indexOf(":");
		if (idx > 0) {
			return model.substring(idx + 1);
		}

		return model;
	}
}