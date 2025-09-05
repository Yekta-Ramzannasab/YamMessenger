package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.service.ChatService;
import com.yamyam.messenger.shared.model.chat.Channel;
import com.yamyam.messenger.shared.model.chat.Chat;
import com.yamyam.messenger.shared.model.chat.GroupChat;
import com.yamyam.messenger.shared.model.chat.PrivateChat;

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
    @Override
    public GroupChat getOrCreateGroupChat(String name, String description, long creatorId, boolean isPrivate) {
        try {
            return NetworkService.getInstance().getOrCreateGroupChat(name, description, creatorId, isPrivate);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get or create group chat", e);
        }
    }

    @Override
    public void sendMessage(long chatId, String text) throws Exception {
        // Check entries
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Message text cannot be empty.");
        }

        // Calling a hypothetical method in NetworkService
        System.out.println("Adapter: Delegating message sending to NetworkService...");
        this.net.sendChatMessage(chatId, text);
    }
}