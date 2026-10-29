package chandanv.local.chandanv.models.dto;

public class ChatMessage {

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        NOTIFICATION,
        CLEAR_CHAT
    }

    private MessageType type = MessageType.CHAT;
    private int idBan;
    private String sender;
    private String role; // "CUSTOMER" | "STAFF" | "SYSTEM"
    private String content; // văn bản thuần – client phải hiển thị bằng textContent
    private long timestamp;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(MessageType type, int idBan, String sender, String role, String content) {
        this.type = type;
        this.idBan = idBan;
        this.sender = sender;
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public int getIdBan() { return idBan; }
    public void setIdBan(int idBan) { this.idBan = idBan; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    /** Mã ngẫu nhiên của điện thoại gửi (nhiều máy cùng bàn) – để mỗi máy biết tin nào là của mình. */
    private String clientId;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
