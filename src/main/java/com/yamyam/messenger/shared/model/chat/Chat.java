package com.yamyam.messenger.shared.model.chat;

import java.sql.Timestamp;

public  class Chat {
    protected long chatId;
    protected Timestamp createdAt;
    protected ChatType type;
    private double searchRank;

    public Chat(long chatId, Timestamp createdAt, ChatType type){
        this.chatId = chatId;
        this.createdAt = createdAt;
        this.type = type;
    }
    public Chat() {
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
    public double getSearchRank() {
        return searchRank;
    }
    public void setSearchRank(double searchRank) {
        this.searchRank = searchRank;
    }
    @Override
    public String toString() {
        return chatId + "," + createdAt + "," + type + "," + searchRank  ;
    }
    public static Chat fromString(String data) {
        // ۱. ورودی null یا خالی را بررسی می‌کنیم
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        // ۲. رشته را بر اساس کاما (,) به چهار بخش تقسیم می‌کنیم
        String[] parts = data.split(",");
        if (parts.length != 4) {
            System.err.println("Invalid format for Chat string (expected 4 parts): " + data);
            return null;
        }

        try {
            // ۳. هر بخش را به نوع داده مربوطه تبدیل می‌کنیم
            long chatId = Long.parseLong(parts[0].trim());
            Timestamp createdAt = Timestamp.valueOf(parts[1].trim());
            ChatType type = ChatType.valueOf(parts[2].trim());
            double searchRank = Double.parseDouble(parts[3].trim());

            // ۴. آبجکت جدید را می‌سازیم
            // این بخش فرض می‌کند شما یک کانستراکتور مناسب دارید
            Chat chat = new Chat(chatId, createdAt, type);
            // و یک setter برای searchRank
            chat.setSearchRank(searchRank);

            return chat;

        } catch (IllegalArgumentException e) {
            // این خطا انواع مختلفی از خطاهای پارس (عدد، تاریخ، enum) را مدیریت می‌کند
            System.err.println("Failed to parse values from Chat string: " + data + " | Error: " + e.getMessage());
            return null;
        }
    }

}
