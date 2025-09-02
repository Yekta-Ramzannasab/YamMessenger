package com.yamyam.messenger.client.network.api;

import com.yamyam.messenger.shared.model.Channel;
import com.yamyam.messenger.shared.model.ChannelSubscribers;
import com.yamyam.messenger.shared.model.Users;
import java.util.List;

public interface UserService {
    List<Users> getAllUsers();
    Users getUserById(long userId);
    ChannelSubscribers subscribeToChannel(Channel channel, long userId);
    ChannelSubscribers getSubscription(Channel channel, long userId);
}