package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.api.UserService;
import com.yamyam.messenger.server.database.DataManager;
import com.yamyam.messenger.shared.model.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class UsersServiceAdapter implements UserService {
    private final NetworkService net;

    public UsersServiceAdapter(NetworkService net) {
        this.net = net;
    }

    @Override
    public List<Users> getAllUsers() {
        try {
            return net.fetchAllUsers();
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }
    @Override
    public Users getUserById(long userId) {
        try {
            return net.fetchUserById(userId);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public ChannelSubscribers subscribeToChannel(Channel channel, long userId) {
        try {
            return NetworkService.getInstance().subscribeToChannel(channel, userId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to subscribe user to channel", e);
        }
    }
    @Override
    public ChannelSubscribers getSubscription(Channel channel, long userId) {
        try {
            return DataManager.getInstance().getOrSubscribeUser(channel, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load subscription", e);
        }
    }
    public GroupMembers joinGroupChat(GroupChat groupChat, Users member, Users invitedBy) {
        try {
            return NetworkService.getInstance().joinGroupChat(groupChat, member, invitedBy);
        } catch (IOException e) {
            throw new RuntimeException("Failed to join group chat", e);
        }
    }

}