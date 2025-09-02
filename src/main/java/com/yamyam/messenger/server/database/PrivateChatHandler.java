package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.PrivateChat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrivateChatHandler {

    private static final PrivateChatHandler instance = new PrivateChatHandler();

    public static PrivateChatHandler getInstance() {
        return instance;
    }

    public PrivateChat checkOrCreateChat(long senderId, long receiverId) throws SQLException {
        long user1 = Math.min(senderId, receiverId);
        long user2 = Math.max(senderId, receiverId);

        PrivateChat existing = Database.loadPrivateChat(user1, user2);
        if (existing != null) return existing;

        return Database.createPrivateChat(user1, user2);
    }
}