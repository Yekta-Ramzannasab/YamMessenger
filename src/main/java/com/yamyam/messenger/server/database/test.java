package com.yamyam.messenger.server.database;

import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.shared.model.chat.Channel;
import com.yamyam.messenger.shared.model.chat.Chat;// تست مستقیم دیتابیس
import com.yamyam.messenger.shared.model.chat.GroupChat;
import com.yamyam.messenger.shared.model.chat.PrivateChat;

import java.sql.SQLException;
import java.util.List;
public class test {
    public static void main(String[] a) {
        long meUserId = 12;
        try {
            List<Chat> chats = DataManager.getInstance().getAllChatsForUser(meUserId);
            System.out.println("✅ Database returned " + chats.size() + " chats:");
            for (Chat chat : chats) {
                System.out.println(" - " + chat.getName() + " | " + chat.getType() + " | ID: " + chat.getChatId());

                if (chat instanceof GroupChat group) {
                    System.out.println("   👥 Group: " + group.getGroupName() + " | Members: " + group.getMemberCount());
                } else if (chat instanceof Channel channel) {
                    System.out.println("   📢 Channel: " + channel.getChannelName());
                } else if (chat instanceof PrivateChat privateChat) {
                    System.out.println("   👤 Private: " + privateChat.getUser1() + " - " + privateChat.getUser2());
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
}