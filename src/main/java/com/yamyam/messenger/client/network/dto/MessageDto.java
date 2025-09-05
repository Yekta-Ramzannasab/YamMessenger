package com.yamyam.messenger.client.network.dto;

public class MessageDto {

    private final long chatId;
    private final long senderId;
    private final String text;

    public MessageDto(long chatId, long senderId, String text) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
    }

    public long getChatId() {
        return chatId;
    }

    public long getSenderId() {
        return senderId;
    }

    public String getText() {
        return text;
    }
}
