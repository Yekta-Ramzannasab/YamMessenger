package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.api.ChatService;
import com.yamyam.messenger.shared.model.Channel;
import com.yamyam.messenger.shared.model.Chat;
import com.yamyam.messenger.shared.model.PrivateChat;

import java.io.IOException;
import java.util.List;

public class NetworkChatServiceAdapter implements ChatService {
    private final NetworkService net;
    public NetworkChatServiceAdapter(NetworkService net) { this.net = net; }

    @Override
    public void openChat(long meUserId, long targetUserId) {
        // TODO: call when its ready
        System.out.println("[openChat] me=" + meUserId + " -> target=" + targetUserId);
        // net.openChat(meUserId, targetUserId);
    }

    @Override
    public List<PrivateChat> getChatsForUser(String email) {
        try {
            return net.fetchPrivateChatsForUser(email);
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }
    @Override
    public List<Chat> getGroupAndChannelChatsForUser(String email) {
        try {
            return net.fetchGroupAndChannelChatsForUser(email);
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }
    @Override
    public PrivateChat getOrCreatePrivateChat(long meUserId, long otherUserId) {
        try {
            return net.fetchOrCreatePrivateChat(meUserId, otherUserId);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Channel getChannelByChatId(long chatId) {
        try {
            return net.fetchChannelById(chatId);
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }


}
