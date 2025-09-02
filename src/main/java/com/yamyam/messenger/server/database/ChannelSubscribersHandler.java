package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.Channel;
import com.yamyam.messenger.shared.model.ChannelSubscribers;
import com.yamyam.messenger.shared.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChannelSubscribersHandler {
    private static final ChannelSubscribersHandler instance = new ChannelSubscribersHandler();

    public static ChannelSubscribersHandler getInstance() {
        return instance;
    }
    public ChannelSubscribers checkOrSubscribeUser(Channel channel, long userId) {
        try {
            ChannelSubscribers existing = Database.loadSubscription(channel.getChatId(), userId, channel);
            if (existing != null) return existing;

            return Database.insertSubscription(channel.getChatId(), userId, channel);
        } catch (SQLException e) {
            throw new RuntimeException("Error in checkOrSubscribeUser", e);
        }
    }
}
