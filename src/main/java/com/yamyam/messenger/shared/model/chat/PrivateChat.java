package com.yamyam.messenger.shared.model.chat;

import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.client.network.dto.ContactType;
import com.yamyam.messenger.shared.model.user.Users;

import java.sql.Timestamp;

public class PrivateChat extends Chat {

    private long user1;
    private long user2;

    public PrivateChat(long id,long user1 , long user2){
        super(id, new Timestamp(System.currentTimeMillis()), ChatType.PRIVATE_CHAT);
        this.user1 = user1;
        this.user2 = user2;
    }

    public long getUser2() {
        return user2;
    }

    public void setUser2(long user2) {
        this.user2 = user2;
    }

    public long getUser1() {
        return user1;
    }

    public void setUser1(long user1) {
        this.user1 = user1;
    }
    public Contact toContact(Users targetUser) {
        return new Contact(
                this.getChatId(),
                targetUser.getUserProfile().getProfileName(),
                targetUser.getUserProfile().getProfileImageUrl(),
                targetUser.isOnline(),
                ContactType.DIRECT,
                null
        );
    }

    @Override
    public String toString() {
        return chatId + "," + user1 + "," + user2 ;
    }

    public static PrivateChat fromString(String data) {
        // ۱. ورودی null یا خالی را بررسی می‌کنیم
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        // ۲. رشته را بر اساس کاما (,) به سه بخش تقسیم می‌کنیم
        String[] parts = data.split(",");

        // ۳. بررسی می‌کنیم که دقیقاً سه بخش داشته باشیم
        if (parts.length != 3) {
            System.err.println("Invalid format for PrivateChat string (expected 3 parts): " + data);
            return null;
        }

        try {
            // ۴. هر بخش را به عدد (long) تبدیل می‌کنیم
            long chatId = Long.parseLong(parts[0].trim());
            long user1 = Long.parseLong(parts[1].trim());
            long user2 = Long.parseLong(parts[2].trim());

            // ۵. با استفاده از کانستراکتور اصلی، آبجکت را می‌سازیم
            return new PrivateChat(chatId, user1, user2);

        } catch (NumberFormatException e) {
            // اگر بخش‌ها عدد نباشند، خطا می‌دهیم
            System.err.println("Failed to parse long values from string: " + data);
            return null;
        }
    }
}
