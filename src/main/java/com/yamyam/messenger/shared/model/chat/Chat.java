package com.yamyam.messenger.shared.model.chat;

import java.sql.Timestamp;

public class Chat {
    protected long chatId;
    protected Timestamp createdAt;
    protected ChatType type;
    private String name; // ✅ فیلد جدید برای نام چت
    private double searchRank;

    // کانستراکتور کامل
    public Chat(long chatId, Timestamp createdAt, ChatType type, String name) {
        this.chatId = chatId;
        this.createdAt = createdAt;
        this.type = type;
        this.name = name;
    }

    // کانستراکتور ساده
    public Chat(long chatId, Timestamp createdAt, ChatType type) {
        this(chatId, createdAt, type, null);
    }

    public Chat() {
    }

    // Getter و Setterها
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSearchRank() {
        return searchRank;
    }

    public void setSearchRank(double searchRank) {
        this.searchRank = searchRank;
    }

    @Override
    public String toString() {
        return chatId + "," + createdAt + "," + type + "," + name + "," + searchRank;
    }

    public static Chat fromString(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        String[] parts = data.split(",", -1); // -1 برای حفظ فیلدهای خالی
        if (parts.length != 5) {
            System.err.println("Invalid format for Chat string (expected 5 parts): " + data);
            return null;
        }

        try {
            long chatId = Long.parseLong(parts[0].trim());
            Timestamp createdAt = Timestamp.valueOf(parts[1].trim());
            ChatType type = ChatType.valueOf(parts[2].trim());
            String name = parts[3].trim();
            double searchRank = Double.parseDouble(parts[4].trim());

            Chat chat = new Chat(chatId, createdAt, type, name);
            chat.setSearchRank(searchRank);
            return chat;

        } catch (Exception e) {
            System.err.println("Failed to parse Chat string: " + data + " | Error: " + e.getMessage());
            return null;
        }
    }
}