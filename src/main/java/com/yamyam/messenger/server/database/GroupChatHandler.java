package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.chat.GroupChat;

import java.sql.SQLException;

public class GroupChatHandler {

    private static final GroupChatHandler instance = new GroupChatHandler();

    public static GroupChatHandler getInstance() {
        return instance;
    }

    private GroupChatHandler() {}

    public GroupChat createGroupChat(String name, String description, long creatorId, boolean isPrivate) throws SQLException {
        return Database.insertGroupChat(name, description, creatorId, isPrivate);
    }

    public GroupChat loadGroupChat(long chatId) throws SQLException {
        return Database.loadGroupChat(chatId);
    }
}