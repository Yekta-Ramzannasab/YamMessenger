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

    @Override
    public String toString() {
        return chatId + "," + senderId + "," + text ;
    }

    public static MessageDto fromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        String[] parts = str.split(",", 3);

        if (parts.length != 3) {
            System.err.println("❌ Invalid MessageDto string format: " + str);
            return null; // strings format is incorrect
        }

        try {
            long chatId = Long.parseLong(parts[0]);
            long senderId = Long.parseLong(parts[1]);
            String text = parts[2];
            return new MessageDto(chatId, senderId, text);
        } catch (NumberFormatException e) {
            System.err.println("❌ Failed to parse Long value in MessageDto string: " + str);
            return null; // error for convert string to object
        }
    }
}
