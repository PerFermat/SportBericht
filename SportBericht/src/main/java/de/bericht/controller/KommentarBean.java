package de.bericht.controller;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import de.bericht.util.BerichtHelper;
import de.bericht.util.ConfigManager;
import de.bericht.util.WordPressAPIClient;
import de.bericht.util.WpComment;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("kommentarBean")
@ViewScoped
public class KommentarBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String name = "tt-hattenhofen";
	private String username;
	private String vereinnr;
	private String password;
	private String selectedPostUrl;

	private List<WpComment> comments;

	public KommentarBean() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));

		this.vereinnr = BerichtHelper.bestimmenVereinnr(request.getParameter("v"));

		if (vereinnr == null) {
			vereinnr = request.getParameter("vereinnr");
		}
		if (vereinnr == null) {
			vereinnr = "13014";
		}

		this.name = request.getParameter("name");
		this.username = request.getParameter("username");
		try {
			String hashPassword = request.getParameter("password");
			this.password = ConfigManager.decryptPassword(vereinnr, hashPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (this.name != null && this.username != null && this.password != null) {
			loadComments();
			selectedPostUrl = ConfigManager.getWordpressValue(vereinnr, name, "domain");
		}
	}

	public void loadComments() {
		try {
			WordPressAPIClient api = new WordPressAPIClient(vereinnr, username, password, name);
			if (api.getFehler() == 0) {
				comments = api.fetchAllComments();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void trashComment(WpComment comment) {
		try {
			WordPressAPIClient api = new WordPressAPIClient(vereinnr, username, password, name);
			if (api.getFehler() == 0) {
				api.trashComment(comment);
				loadComments(); // neu laden
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void approveComment(WpComment comment) {
		try {
			WordPressAPIClient api = new WordPressAPIClient(vereinnr, username, password, name);
			if (api.getFehler() == 0) {
				api.approveComment(comment);
				loadComments(); // neu laden
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void markAsSpam(WpComment comment) {
		try {
			WordPressAPIClient api = new WordPressAPIClient(vereinnr, username, password, name);
			if (api.getFehler() == 0) {
				api.markCommentAsSpam(comment);
				loadComments(); // neu laden
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getHomepageUrl() {
		selectedPostUrl = ConfigManager.getWordpressValue(vereinnr, name, "domain");
		return selectedPostUrl;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public List<WpComment> getComments() {
		return comments;
	}

	public void setName(String name) {
		this.name = name;
		selectedPostUrl = ConfigManager.getWordpressValue(vereinnr, name, "domain");
	}

	public String getName() {
		return name;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<String> getHomepages() throws IOException, InterruptedException, URISyntaxException {
		String domains = ConfigManager.getConfigValue(vereinnr, "wordpress.domains");
		String[] werteArray = domains.split(",");
		return Arrays.asList(werteArray);

	}

	public String getItemValue() {
		return BerichtHelper.getHomepageStandardZusammen(vereinnr);
	}

	public void showPost(long postId) {
		String domain = ConfigManager.getWordpressValue(vereinnr, name, "domain");
		selectedPostUrl = domain + "/?p=" + postId;
	}

	public String getSelectedPostUrl() {
		return selectedPostUrl != null ? selectedPostUrl : getHomepageUrl();
	}

	public List<WpComment> getApprovedComments() {
		if (comments == null) {
			return List.of();
		}
		return comments.stream().filter(c -> "approved".equalsIgnoreCase(c.getStatus())).toList();
	}

	public List<WpComment> getHoldComments() {
		if (comments == null) {
			return List.of();
		}

		return comments.stream().filter(c -> "hold".equalsIgnoreCase(c.getStatus())).toList();
	}

	public List<WpComment> getSpamComments() {
		if (comments == null) {
			return List.of();
		}
		return comments.stream().filter(c -> "spam".equalsIgnoreCase(c.getStatus())).toList();
	}

	public List<WpComment> getTrashComments() {
		if (comments == null) {
			return List.of();
		}
		return comments.stream().filter(c -> "trash".equalsIgnoreCase(c.getStatus())).toList();
	}

	public int getApprovedCount() {
		return (comments == null) ? 0
				: (int) comments.stream().filter(c -> "approved".equalsIgnoreCase(c.getStatus())).count();
	}

	public int getHoldCount() {
		return (comments == null) ? 0
				: (int) comments.stream().filter(c -> "hold".equalsIgnoreCase(c.getStatus())).count();
	}

	public int getSpamCount() {
		return (comments == null) ? 0
				: (int) comments.stream().filter(c -> "spam".equalsIgnoreCase(c.getStatus())).count();
	}

	public int getTrashCount() {
		return (comments == null) ? 0
				: (int) comments.stream().filter(c -> "trash".equalsIgnoreCase(c.getStatus())).count();
	}

	public String getVereinnr() {
		return vereinnr;
	}

	public void setVereinnr(String vereinnr) {
		this.vereinnr = vereinnr;
	}

	public String getBestimmenIcon() {
		return ConfigManager.getConfigValue(vereinnr, "style.icon");
	}

	public String getVereinHomepage() {
		return ConfigManager.getConfigValue(vereinnr, "homepage.verein");
	}

	public boolean isTennis() {
		return ConfigManager.isTennis(vereinnr);
	}

	public boolean isTischtennis() {
		return ConfigManager.isTischtennis(vereinnr);
	}

	public void zurueck() {

	}
}