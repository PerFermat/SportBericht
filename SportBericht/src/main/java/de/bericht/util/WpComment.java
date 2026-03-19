package de.bericht.util;

public class WpComment {
	private int id;
	private int post;
	private String date;
	private String authorName;
	private String content;
	private String status;
	private long postId;

	@Override
	public String toString() {
		return "Kommentar von " + authorName + " (Status: " + status + "): " + content;
	}

	public int getId() {
		return id;
	}

	public int getPost() {
		return post;
	}

	public String getAuthorName() {
		return authorName;
	}

	public String getContent() {
		return content;
	}

	public String getStatus() {
		return status;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setPost(int post) {
		this.post = post;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getPostId() {
		return postId;
	}

	public void setPostId(long postId) {
		this.postId = postId;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
}
