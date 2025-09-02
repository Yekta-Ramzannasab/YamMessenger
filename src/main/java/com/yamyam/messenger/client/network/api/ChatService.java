package com.yamyam.messenger.client.network.api;


import com.yamyam.messenger.shared.model.Channel;
import com.yamyam.messenger.shared.model.Chat;
import com.yamyam.messenger.shared.model.GroupChat;
import com.yamyam.messenger.shared.model.PrivateChat;

import java.util.List;

public interface ChatService {
    void openChat(long meUserId, long targetUserId);
    List<PrivateChat> getChatsForUser(String email);
    List<Chat> getGroupAndChannelChatsForUser(String email);
    PrivateChat getOrCreatePrivateChat(long meUserId, long otherUserId);
    Channel getChannelByChatId(long chatId);
    GroupChat getOrCreateGroupChat(String name, String description, long creatorId, boolean isPrivate);

    //List<Message> getHistory(long meUserId, long targetUserId, int limit);
    //void send(long meUserId, long targetUserId, String text);
}
