package com.yamyam.messenger.shared.model;

import java.sql.Timestamp;
import java.time.LocalTime;

public abstract class Chat {
    protected long chatId;
    protected Timestamp createdAt;
    protected ChatType type;

    public Chat(long chatId, Timestamp createdAt, ChatType type){
        this.chatId = chatId;
        this.createdAt = createdAt;
        this.type = type;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public ChatType getType() {
        return type;
    }

    public void setType(ChatType type) {
        this.type = type;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
