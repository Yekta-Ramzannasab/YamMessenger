package com.yamyam.messenger.client.network.api;


import java.util.List;

public interface ChatService {
    void openChat(long meUserId, long targetUserId);
    //List<Message> getHistory(long meUserId, long targetUserId, int limit);
    //void send(long meUserId, long targetUserId, String text);
}
