import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private String content;
    private LocalDateTime timestamp;
    private String sender;
    private MessageType type;
    private boolean encrypted;

    public enum MessageType {
        PUBLIC, PRIVATE, SYSTEM, FILE
    }

    public Message(String content, String sender, MessageType type) {
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.sender = sender;
        this.type = type;
        this.encrypted = false;
    }

    public Message(String content, String sender, MessageType type, boolean encrypted) {
        this(content, sender, type);
        this.encrypted = encrypted;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return timestamp.format(formatter);
    }

    public String getSender() {
        return sender;
    }

    public MessageType getType() {
        return type;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public String toString() {
        String prefix = "[" + getFormattedTimestamp() + "] ";
        switch (type) {
            case PUBLIC:
                return prefix + sender + ": " + content;
            case PRIVATE:
                return prefix + "[PM from " + sender + "] " + content;
            case SYSTEM:
                return prefix + content;
            case FILE:
                return prefix + sender + " sent a file: " + content;
            default:
                return prefix + content;
        }
    }
}
