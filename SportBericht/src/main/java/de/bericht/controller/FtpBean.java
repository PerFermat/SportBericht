package de.bericht.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

@Named
@ViewScoped
public class FtpBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final int SFTP_CONNECT_TIMEOUT_MS = 15000;
	private Boolean freigabe;

	private enum UploadThema {
		HALLENBELEGUNGN("HALLENBELEGUNGN", "Hallenbelegung neuer Monat", "Hallenbelegung", "Hallenbelegung.pdf",
				"Hallenbelegungalt.pdf"),
		HALLENBELEGUNGE("HALLENBELEGUNGE", "Hallenbelegung ersetzen aktuellen Monat", "Hallenbelegung",
				"Hallenbelegung.pdf", null),
		KINDERFOTOS("KINDERFOTOS", "Einverständniserklärung Kinderfotos", "Foto", null, null);

		private final String key;
		private final String label;
		private final String verzeichnis;
		private final String neuerDateiname;
		private final String alterDateiname;

		UploadThema(String key, String label, String verzeichnis, String neuerDateiname, String alterDateiname) {
			this.key = key;
			this.label = label;
			this.verzeichnis = verzeichnis;
			this.neuerDateiname = neuerDateiname;
			this.alterDateiname = alterDateiname;
		}

		public static UploadThema fromKey(String key) {
			return Arrays.stream(values()).filter(v -> v.key.equals(key)).findFirst().orElse(HALLENBELEGUNGN);
		}
	}

	private String vereinnr;
	private String ausgewaehltesThema = UploadThema.HALLENBELEGUNGN.key;
	private Part uploadedDatei;

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));
		if (vereinnr == null) {
			vereinnr = request.getParameter("vereinnr");
		}

		if (!(request.getParameter("p") == null)
				&& request.getParameter("p").equals(ConfigManager.getUserPasswort(vereinnr))) {
			this.freigabe = true;
		} else {
			addMessage(FacesMessage.SEVERITY_ERROR, "Fasches Passwort eingegeben",
					"Bitte die Seite mit \"?v=<verein>&p=<Passwort für den internen Bereich>\" aufrufen");
			this.freigabe = false;
		}

	}

	public void hochladen() {
		if (!freigabe) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Fasches Passwort eingegeben",
					"Bitte die Seite mit \"?v=<verein>&p=<Passwort für den internen Bereich>\" aufrufen");
			return;
		}
		if (uploadedDatei == null || uploadedDatei.getSize() == 0) {
			addMessage(FacesMessage.SEVERITY_WARN, "Keine Datei ausgewählt", "Bitte zuerst eine Datei auswählen.");
			return;
		}
		if (vereinnr == null || vereinnr.isBlank()) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Fehlende Vereinsnummer",
					"Die Vereinsnummer fehlt. Bitte Seite über den regulären Einstieg öffnen.");
			return;
		}

		String host = ConfigManager.getSftpUrl(vereinnr);
		String user = ConfigManager.getSftpUser(vereinnr);
		String portRaw = ConfigManager.getSftpPort(vereinnr);
		String passwort = ConfigManager.getSftpPasswort(vereinnr);

		if (isBlank(host) || isBlank(user) || isBlank(portRaw) || isBlank(passwort)) {
			addMessage(FacesMessage.SEVERITY_ERROR, "SFTP-Konfiguration unvollständig",
					"Bitte sftp.url, sftp.user, sftp.port und sftp.passwort in der Config prüfen.");
			return;
		}

		int port;
		try {
			port = Integer.parseInt(portRaw.trim());
		} catch (NumberFormatException e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Ungültiger SFTP-Port", "sftp.port ist keine Zahl: " + portRaw);
			return;
		}

		UploadThema thema = UploadThema.fromKey(ausgewaehltesThema);
		String originalDateiname = sanitizeDateiname(uploadedDatei.getSubmittedFileName());
		String zielDateiname = thema.neuerDateiname != null ? thema.neuerDateiname : originalDateiname;
		String zielPfad = buildRemotePath(
				ConfigManager.getConfigValue(vereinnr, "sftp.verzeichnis.pdf") + thema.verzeichnis, zielDateiname);
		String backupPfad = thema.alterDateiname != null
				? buildRemotePath(ConfigManager.getConfigValue(vereinnr, "sftp.verzeichnis.pdf") + thema.verzeichnis,
						thema.alterDateiname)
				: null;

		Session session = null;
		ChannelSftp sftp = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(user, host, port);
			session.setPassword(passwort);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(SFTP_CONNECT_TIMEOUT_MS);

			sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect(SFTP_CONNECT_TIMEOUT_MS);

			boolean bestehendeDateiGesichert = false;
			if (backupPfad != null && remoteExists(sftp, zielPfad)) {
				if (remoteExists(sftp, backupPfad)) {
					sftp.rm(backupPfad);
				}
				sftp.rename(zielPfad, backupPfad);
				bestehendeDateiGesichert = true;
			}

			try (InputStream inputStream = uploadedDatei.getInputStream()) {
				sftp.put(inputStream, zielPfad, ChannelSftp.OVERWRITE);
			} catch (IOException | SftpException e) {
				if (bestehendeDateiGesichert && remoteExists(sftp, backupPfad) && !remoteExists(sftp, zielPfad)) {
					sftp.rename(backupPfad, zielPfad);
				}
				throw e;
			}

			uploadedDatei = null;
			addMessage(FacesMessage.SEVERITY_INFO, "Upload erfolgreich",
					"Datei wurde per SFTP übertragen: " + zielPfad);
		} catch (JSchException | SftpException | IOException e) {
			addMessage(FacesMessage.SEVERITY_ERROR, "Upload fehlgeschlagen", e.getMessage());
		} finally {
			if (sftp != null && sftp.isConnected()) {
				sftp.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	private boolean remoteExists(ChannelSftp sftp, String remotePath) {
		try {
			sftp.lstat(remotePath);
			return true;
		} catch (SftpException e) {
			return false;
		}
	}

	private String buildRemotePath(String verzeichnis, String dateiname) {
		String basis = verzeichnis.endsWith("/") ? verzeichnis.substring(0, verzeichnis.length() - 1) : verzeichnis;
		return basis + "/" + dateiname;
	}

	private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String sanitizeDateiname(String dateiname) {
		if (dateiname == null || dateiname.isBlank()) {
			return "upload.bin";
		}
		String clean = dateiname.replace("\\", "/");
		int idx = clean.lastIndexOf('/');
		String result = idx >= 0 ? clean.substring(idx + 1) : clean;
		if (result.isBlank()) {
			return "upload.bin";
		}
		return result;
	}

	public List<SelectItem> getThemaOptionen() {
		return Arrays.stream(UploadThema.values()).map(thema -> new SelectItem(thema.key, thema.label)).toList();
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public String getAusgewaehltesThema() {
		return ausgewaehltesThema;
	}

	public void setAusgewaehltesThema(String ausgewaehltesThema) {
		this.ausgewaehltesThema = ausgewaehltesThema;
	}

	public Part getUploadedDatei() {
		return uploadedDatei;
	}

	public void setUploadedDatei(Part uploadedDatei) {
		this.uploadedDatei = uploadedDatei;
	}
}
