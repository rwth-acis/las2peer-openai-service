package i5.las2peer.services.openAIService;

public class ChatMessage {
	public ChatMessage(String role, String content, String name) {
		super();
		this.role = role;
		this.content = content;
		this.name = name;
	}
	String role;
	String content;
	String name;
	
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean hasName() {
		if (this.name != null) {
			return true;
		}
		return false;
	}
}
