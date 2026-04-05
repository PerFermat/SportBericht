package de.bericht.service;

import java.io.InputStream;
import java.util.Properties;

import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

public class EmailService {

	private Properties config;
	private String recipients;
	private String ccEmpfaenger;
	private DatabaseService ds = new DatabaseService();

	public EmailService(String vereinnr, String name) {
		bestimmeEmpfaenger(vereinnr, name);
		ladeProperties();

	}

	public EmailService(String vereinnr, String to, String cc) {
		recipients = to;
		ccEmpfaenger = cc;
		ladeProperties();
	}

	public void ladeProperties() {
		config = new Properties();
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
			if (is != null) {
				config.load(is);
			} else {
				System.err.println("mail.properties konnte nicht gefunden werden!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void bestimmeEmpfaenger(String vereinnr, String name) {
		recipients = ConfigManager.getConfigValue(vereinnr, "mail.smtp.recipients");
		ccEmpfaenger = null;

		String cctrennzeichen = "CC:";
		String[] ccteile = name.split(cctrennzeichen);
		String toTrennzeichen = "TO:";
		String[] toteile = name.split(toTrennzeichen);

		if ("TEST".equals(name.toUpperCase())) {
			recipients = "michael.spahr@web.de";
		} else if (ccteile.length > 1) {
			ccEmpfaenger = ccteile[1].trim();
		} else if (toteile.length > 1) {
			recipients = toteile[1].trim();
		}
	}

	/**
	 * Versendet eine E-Mail mit HTML-Content (und optional Anhang). Wenn ein
	 * Mail-Client kein HTML anzeigen kann, wird automatisch eine einfache
	 * Textversion mitgeschickt.
	 */
	public void sendEmail(String vereinnr, String mailSubject, String mailText, byte[] attachment,
			String attachmentName, Boolean speichern) throws MessagingException {

		String htmlMailText = toHtml(mailText);

		// SMTP-Einstellungen laden
		Properties props = new Properties();
		props.put("mail.smtp.host", ConfigManager.getConfigValue(vereinnr, "mail.smtp.host"));
		props.put("mail.smtp.port", ConfigManager.getConfigValue(vereinnr, "mail.smtp.port"));
		props.put("mail.smtp.auth", ConfigManager.getConfigValue(vereinnr, "mail.smtp.auth"));
		props.put("mail.smtp.starttls.enable", ConfigManager.getConfigValue(vereinnr, "mail.smtp.starttls.enable"));

		// Session mit Authentifizierung
		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(ConfigManager.getConfigValue(vereinnr, "mail.username"),
						ConfigManager.getMailPasswort(vereinnr));
			}
		});

		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(ConfigManager.getConfigValue(vereinnr, "mail.username")));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));

		if (ccEmpfaenger != null && !ccEmpfaenger.isBlank()) {
			message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmpfaenger));
		}

		message.setSubject(mailSubject, "UTF-8");

		// multipart/alternative: Text + HTML
		MimeMultipart alternativePart = new MimeMultipart("alternative");

		// Plaintext-Version (HTML entfernt)
		MimeBodyPart textPart = new MimeBodyPart();
		textPart.setText(stripHtmlTags(htmlMailText), "UTF-8");

		// HTML-Version
		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(htmlMailText, "text/html; charset=UTF-8");

		alternativePart.addBodyPart(textPart);
		alternativePart.addBodyPart(htmlPart);

		// Falls Anhang vorhanden ist, kompletten Multipart zusammenbauen
		Multipart finalMultipart = new MimeMultipart();
		MimeBodyPart contentPart = new MimeBodyPart();
		contentPart.setContent(alternativePart);
		finalMultipart.addBodyPart(contentPart);

		if (attachment != null && attachment.length > 0) {
			MimeBodyPart attachmentPart = new MimeBodyPart();
			ByteArrayDataSource dataSource = new ByteArrayDataSource(attachment, "application/octet-stream");
			attachmentPart.setDataHandler(new DataHandler(dataSource));
			attachmentPart.setFileName(attachmentName);
			finalMultipart.addBodyPart(attachmentPart);
		}

		message.setContent(finalMultipart);

		Transport.send(message);
		if (speichern) {
			ds.speichernMail(vereinnr, recipients, ccEmpfaenger, mailSubject, htmlMailText, attachmentName);
		}

	}

	/**
	 * Entfernt alle HTML-Tags und Entities aus einem String. Wird verwendet, um die
	 * reine Textversion der E-Mail zu erstellen.
	 */
	private String stripHtmlTags(String html) {
		if (html == null) {
			return "";
		}
		html = BerichtHelper.mergeParagraphsWithJsoup(html);
		String textOnly = html.replaceAll("<[^>]*>", "");
		textOnly = textOnly.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("&lt;", "<")
				.replaceAll("&gt;", ">").replaceAll("&quot;", "\"").replaceAll("&#39;", "'");
		return textOnly.replaceAll("\\s+", " ").trim();
	}

	private String toHtml(String mailText) {
		if (mailText == null || mailText.isBlank()) {
			return "";
		}
		if (mailText.matches("(?is).*<[^>]+>.*")) {
			return mailText;
		}
		return mailText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
	}

	public String getRecipients() {
		return recipients;
	}

	public void setRecipients(String recipients) {
		this.recipients = recipients;
	}

	public String getCcEmpfaenger() {
		return ccEmpfaenger;
	}

	public void setCcEmpfaenger(String ccEmpfaenger) {
		this.ccEmpfaenger = ccEmpfaenger;
	}
}
