package com.yamyam.messenger.client.network.service;

import com.yamyam.messenger.shared.model.*;
import com.yamyam.messenger.shared.model.chat.Channel;
import com.yamyam.messenger.shared.model.chat.ChannelSubscribers;
import com.yamyam.messenger.shared.model.user.Users;

import java.util.List;

public interface UserService {
    List<Users> getAllUsers();
    Users getUserById(long userId);
    ChannelSubscribers subscribeToChannel(Channel channel, long userId);
    ChannelSubscribers getSubscription(Channel channel, long userId);
    GroupMembers joinGroupChat(GroupChat groupChat, Users member, Users invitedBy);
}