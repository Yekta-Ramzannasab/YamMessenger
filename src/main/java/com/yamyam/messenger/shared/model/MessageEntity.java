package com.yamyam.messenger.shared.model;

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

}
