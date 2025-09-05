package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.chat.Channel;
import com.yamyam.messenger.shared.model.chat.ChannelSubscribers;

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
