package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class DataManager {

    private static DataManager instance;

    private final Map<Long, Users> userCache = new ConcurrentHashMap<>();
    private final Map<Long, List<MessageEntity>> messageCache = new ConcurrentHashMap<>();
    private final Map<Long, Chat> chatCache = new ConcurrentHashMap<>();
    private final List<DataChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<String, List<Users>> userSearchCache = new ConcurrentHashMap<>();
    private final Map<String, List<Chat>> chatSearchCache = new ConcurrentHashMap<>();
    private final Map<String, List<MessageEntity>> messageSearchCache = new ConcurrentHashMap<>();


    // Private constructor to enforce singleton
    private DataManager() {}

    // Thread-safe instance getter
    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // ---------------- Users ----------------
    public Users getUser(long userId) throws SQLException {
        return userCache.computeIfAbsent(userId, id -> {
            try {
                return Database.loadUser(id);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Users getUser(String email) throws SQLException {
        long userId = Database.getUserIdByEmail(email);
        return userCache.computeIfAbsent(userId, id -> {
            try {
                return Database.loadUser(id);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<Users> getAllUsers() throws SQLException {
        List<Users> users = Database.loadAllUsers();
        for (Users u : users) {
            userCache.putIfAbsent(u.getId(), u);
        }
        return users;
    }

    // ---------------- Messages ----------------
    public List<MessageEntity> getMessages(long chatId) throws SQLException {
        return messageCache.computeIfAbsent(chatId, id -> {
            try {
                return Database.loadMessages(chatId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addMessage(long chatId, long senderId, String text) {
        executor.submit(() -> {
            try {
                // TODO: Use the appropriate handler to insert message into DB
                // Example: MessageHandler.insertMessage(chatId, senderId, text);

                MessageEntity message = new MessageEntity(
                        MessageType.SENT,
                        false,
                        false,
                        0,
                        0,
                        MessageType.TEXT,
                        text,
                        chatCache.get(chatId),
                        getUser(senderId),
                        System.currentTimeMillis() // temporary id
                );

                messageCache.computeIfAbsent(chatId, k -> new ArrayList<>()).add(message);
                notifyListeners("message_added", message);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // ---------------- Chats ----------------
    public Chat getChat(long chatId) throws SQLException {
        return chatCache.computeIfAbsent(chatId, id -> {
            try {
                Chat chat = Database.loadPrivateChat(id);
                if (chat != null) return chat;
                chat = Database.loadGroupChat(id);
                if (chat != null) return chat;
                return Database.loadChannel(id);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public List<PrivateChat> getPrivateChatsForUser(long userId) throws SQLException {
        List<PrivateChat> allChats = Database.getPrivateChatsByUserId(userId);
        List<PrivateChat> privateChats = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (PrivateChat chat : allChats) {
            long chatId = chat.getChatId();
            if (seen.add(chatId)) {
                chatCache.putIfAbsent(chatId, chat);
                privateChats.add(chat);
            }
        }

        System.out.println("[DataManager] Loaded " + privateChats.size() + " private chats for user " + userId);
        return privateChats;
    }
    public List<Chat> getGroupAndChannelChatsForUser(long userId) throws SQLException {
        List<Chat> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        List<Chat> rawChats = Database.getUserGroupsAndChannels(userId);

        for (Chat chat : rawChats) {
            long chatId = chat.getChatId();

            if (seen.add(chatId)) {
                Chat cached = chatCache.get(chatId);

                if (cached != null) {
                    result.add(cached);
                } else {
                    chatCache.put(chatId, chat);
                    result.add(chat);
                }
            }
        }

        System.out.println("[DataManager] Loaded " + result.size() + " group/channel chats for user " + userId);
        return result;
    }


    // ---------------- Observer Pattern ----------------
    public void addListener(DataChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(String eventType, Object data) {
        for (DataChangeListener l : listeners) {
            l.onDataChanged(eventType, data);
        }
    }
    // --------- Users ----------
    public List<Users> searchUsers(String query) throws SQLException {

        if (userSearchCache.containsKey(query)) {
            return userSearchCache.get(query);
        }

        List<Users> users = Database.searchUsers(query);
        userSearchCache.put(query, users);
        return users;
    }

    // --------- Chats ----------
    public List<Chat> searchChats(String query, long userId) throws SQLException {
        if (chatSearchCache.containsKey(query)) {
            return chatSearchCache.get(query);
        }

        List<Chat> chats = Database.searchChats(query, userId);
        chatSearchCache.put(query, chats);
        return chats;
    }

    // --------- Messages ----------
    public List<MessageEntity> searchMessages(String query, long userId) throws SQLException {
        if (messageSearchCache.containsKey(query)) {
            return messageSearchCache.get(query);
        }

        List<MessageEntity> messages = Database.searchMessages(query, userId);
        messageSearchCache.put(query, messages);
        return messages;
    }



}
