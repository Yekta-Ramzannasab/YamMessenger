package com.yamyam.messenger.shared.model.message;

import com.yamyam.messenger.shared.model.chat.Chat;
import com.yamyam.messenger.shared.model.user.Users;

import java.sql.Timestamp;

public class MessageEntity {

    private long id;
    private Users sender;
    private Chat chat;
    private String text;
    private MessageType type;
    private long reply;
    private long forward;
    private boolean isEdited;
    private boolean isDeleted;
    private MessageStatus status;
    private Timestamp sentAt;
    private double searchRank;

    public MessageEntity(MessageStatus status,
                         boolean isDeleted,
                         boolean isEdited,
                         long forward,
                         long reply,
                         MessageType type,
                         String text,
                         Chat chat,
                         Users sender,
                         long id) {
        this.status = status;
        this.isDeleted = isDeleted;
        this.isEdited = isEdited;
        this.forward = forward;
        this.reply = reply;
        this.type = type;
        this.text = text;
        this.chat = chat;
        this.sender = sender;
        this.id = id;
        this.sentAt = new Timestamp(System.currentTimeMillis());
    }
    public MessageEntity(){}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public long getForward() {
        return forward;
    }

    public void setForward(long forward) {
        this.forward = forward;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Users getSender() {
        return sender;
    }

    public void setSender(Users sender) {
        this.sender = sender;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public long getReply() {
        return reply;
    }

    public void setReply(long reply) {
        this.reply = reply;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }
    public double getSearchRank() {
        return searchRank;
    }

    public void setSearchRank(double searchRank) {
        this.searchRank = searchRank;
    }

    @Override
    public String toString() {
        // A more robust delimiter that is unlikely to be in the message text.
        final String DELIMITER = "|:|";

        // Use a StringBuilder for better performance.
        StringBuilder sb = new StringBuilder();

        sb.append(this.getId()).append(DELIMITER);
        // Handle potential null sender/chat objects gracefully.
        sb.append(this.getSender() != null ? this.getSender().getId() : "null").append(DELIMITER);
        sb.append(this.getChat() != null ? this.getChat().getChatId() : "null").append(DELIMITER);
        // Handle null text.
        sb.append(this.getText() != null ? this.getText() : "").append(DELIMITER);
        sb.append(this.getType()).append(DELIMITER);
        sb.append(this.getReply()).append(DELIMITER);
        sb.append(this.getForward()).append(DELIMITER);
        sb.append(this.isEdited()).append(DELIMITER);
        sb.append(this.isDeleted()).append(DELIMITER);
        sb.append(this.getStatus()).append(DELIMITER);
        // Convert timestamp to a simple long (milliseconds) to avoid formatting issues.
        sb.append(this.getSentAt() != null ? this.getSentAt().getTime() : "0").append(DELIMITER);
        sb.append(this.getSearchRank());

        return sb.toString();
    }

    public static MessageEntity fromString(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        // Use the same robust delimiter. We need to escape the pipe character for regex.
        final String DELIMITER_REGEX = "\\|:\\|";
        // The -1 limit ensures trailing empty fields are not discarded.
        String[] parts = data.split(DELIMITER_REGEX, -1);

        if (parts.length != 12) {
            System.err.println("Invalid MessageEntity string format. Expected 12 parts, got " + parts.length);
            return null;
        }

        try {
            MessageEntity entity = new MessageEntity();
            entity.setId(Long.parseLong(parts[0]));

            // Recreate placeholder objects for sender and chat.
            if (!"null".equals(parts[1])) {
                Users sender = new Users();
                sender.setId(Long.parseLong(parts[1]));
                entity.setSender(sender);
            }

            if (!"null".equals(parts[2])) {
                Chat chat = new Chat();
                chat.setChatId(Long.parseLong(parts[2]));
                entity.setChat(chat);
            }

            entity.setText(parts[3]);
            entity.setType(MessageType.valueOf(parts[4]));
            entity.setReply(Long.parseLong(parts[5]));
            entity.setForward(Long.parseLong(parts[6]));
            entity.setEdited(Boolean.parseBoolean(parts[7]));
            entity.setDeleted(Boolean.parseBoolean(parts[8]));
            entity.setStatus(MessageStatus.valueOf(parts[9]));

            // Parse the long back to a Timestamp.
            long sentAtMillis = Long.parseLong(parts[10]);
            if (sentAtMillis > 0) {
                entity.setSentAt(new Timestamp(sentAtMillis));
            }

            entity.setSearchRank(Double.parseDouble(parts[11]));

            return entity;

        } catch (Exception e) {
            System.err.println("Failed to parse MessageEntity from string: " + data + " | Error: " + e.getMessage());
            return null; // Return null instead of crashing.
        }
    }
}