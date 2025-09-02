package com.yamyam.messenger.client.network.api;

import com.yamyam.messenger.shared.model.*;

import java.util.List;

public interface UserService {
    List<Users> getAllUsers();
    Users getUserById(long userId);
    ChannelSubscribers subscribeToChannel(Channel channel, long userId);
    ChannelSubscribers getSubscription(Channel channel, long userId);
    GroupMembers joinGroupChat(GroupChat groupChat, Users member, Users invitedBy);
}