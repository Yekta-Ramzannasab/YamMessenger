package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.chat.PrivateChat;

import java.sql.SQLException;

public class PrivateChatHandler {


    public PrivateChat checkOrCreateChat(long senderId, long receiverId) throws SQLException {
        long user1 = Math.min(senderId, receiverId);
        long user2 = Math.max(senderId, receiverId);

        PrivateChat existing = Database.loadPrivateChat(user1, user2);
        if (existing != null) {
            System.out.println("✅ Existing private chat found: chatId=" + existing.getChatId());
            return existing;
        }
        else{
            System.out.println("not exit");
        }

        long chatId = Database.createChat();
        System.out.println("🆕 Creating new private chat with chatId=" + chatId);

        return Database.createPrivateChat(chatId, user1, user2);
    }
}