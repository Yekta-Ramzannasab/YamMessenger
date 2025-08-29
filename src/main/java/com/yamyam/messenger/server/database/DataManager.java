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
    private static final Map<String, List<UserProfile>> searchCache = new ConcurrentHashMap<>();


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

    // ---------------- Observer Pattern ----------------
    public void addListener(DataChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(String eventType, Object data) {
        for (DataChangeListener l : listeners) {
            l.onDataChanged(eventType, data);
        }
    }
    public static List<UserProfile> searchUsers(String query) throws SQLException {
        // first search in cash
        if (searchCache.containsKey(query.toLowerCase())) {
            System.out.println("⚡ Result from cache for: " + query);
            return searchCache.get(query.toLowerCase());
        }


        List<UserProfile> results = new ArrayList<>();

        String sql = """
            SELECT up.profile_id,
                   up.user_id,
                   up.profile_image_url,
                   up.bio,
                   up.username,
                   up.password_hashed,
                   up.updated_at,
                   up.profile_name,
                   ts_rank_cd(u.search_vector, plainto_tsquery('english', ?)) AS rank
            FROM user_profiles up
            JOIN users u ON u.id = up.user_id
            WHERE u.search_vector @@ plainto_tsquery('english', ?)
            ORDER BY rank DESC
            LIMIT 20;
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, query);
            stmt.setString(2, query);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserProfile profile = new UserProfile(
                            rs.getLong("profile_id"),
                            rs.getString("profile_image_url"),
                            rs.getString("bio"),
                            rs.getString("username"),
                            rs.getString("password_hashed"),
                            rs.getTimestamp("updated_at"),
                            rs.getString("profile_name")
                    );
                    profile.setUserId(rs.getLong("user_id"));
                    results.add(profile);
                }
            }
        }

        searchCache.put(query.toLowerCase(), results);

        return results;
    }
}
