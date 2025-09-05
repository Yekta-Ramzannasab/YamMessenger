package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.chat.Channel;

import java.sql.SQLException;

public class ChannelHandler {

    private static final ChannelHandler instance = new ChannelHandler();

    public static ChannelHandler getInstance() {
        return instance;
    }

    public Channel createChannel(String name, long ownerId, boolean isPrivate, String description) throws SQLException {
        return Database.insertChannel(name, ownerId, isPrivate, description);
    }

    public Channel loadChannel(long chatId) throws SQLException {
        return Database.loadChannel(chatId);
    }
}