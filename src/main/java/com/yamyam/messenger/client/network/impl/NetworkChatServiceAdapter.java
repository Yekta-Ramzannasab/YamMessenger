package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.api.ChatService;

public class NetworkChatServiceAdapter implements ChatService {
    private final NetworkService net;
    public NetworkChatServiceAdapter(NetworkService net) { this.net = net; }

    @Override
    public void openChat(long meUserId, long targetUserId) {
        // TODO: call when its ready
        System.out.println("[openChat] me=" + meUserId + " -> target=" + targetUserId);
        // net.openChat(meUserId, targetUserId);
    }
}
