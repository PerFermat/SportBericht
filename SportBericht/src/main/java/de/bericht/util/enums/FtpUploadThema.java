package de.bericht.util.enums;

import java.util.Arrays;

public enum FtpUploadThema {
	HALLENBELEGUNGN("HALLENBELEGUNGN", "Hallenbelegung neuer Monat", "Hallenbelegung", "Hallenbelegung.pdf",
			"Hallenbelegungalt.pdf", true),
	HALLENBELEGUNGE("HALLENBELEGUNGE", "Hallenbelegung ersetzen aktuellen Monat", "Hallenbelegung",
			"Hallenbelegung.pdf", null, true),
	KINDERFOTOS("KINDERFOTOS", "Einverständniserklärung Kinderfotos", "Foto", null, null, false);

	public final String key;
	public final String label;
	public final String verzeichnis;
	public final String neuerDateiname;
	public final String alterDateiname;
	private final boolean halleParcer;

	FtpUploadThema(String key, String label, String verzeichnis, String neuerDateiname, String alterDateiname,
			boolean halleParcer) {
		this.key = key;
		this.label = label;
		this.verzeichnis = verzeichnis;
		this.neuerDateiname = neuerDateiname;
		this.alterDateiname = alterDateiname;
		this.halleParcer = halleParcer;
	}

	public static FtpUploadThema fromKey(String key) {
		return Arrays.stream(values()).filter(v -> v.key.equals(key)).findFirst().orElse(HALLENBELEGUNGN);
	}

	public boolean isHalleParcer() {
		return halleParcer;
	}
}