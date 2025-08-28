package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    // Static HikariDataSource, acts like a singleton for the whole app
    private static final HikariDataSource dataSource;


    static {
        // Read environment variables for database credentials
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        // Check if any required environment variable is missing
        if (url == null || user == null || password == null) {
            throw new RuntimeException("Database environment variables (DB_URL, DB_USER, DB_PASSWORD) are not set!");
        }

        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);                     // Database URL
        config.setUsername(user);                   // Database username
        config.setPassword(password);               // Database password

        // Pool settings
        config.setMaximumPoolSize(10);             // Maximum number of connections in the pool
        config.setMinimumIdle(2);                  // Minimum number of idle connections kept in the pool
        config.setIdleTimeout(30000);              // 30 seconds before an idle connection is released
        config.setMaxLifetime(1800000);            // 30 minutes maximum lifetime for a connection
        config.setConnectionTimeout(10000);        // Wait up to 10 seconds for a connection

        // Initialize the connection pool
        dataSource = new HikariDataSource(config);
    }

    // Private constructor to prevent instantiation (singleton-like)
    private Database() {}

    /**
     * Get a connection from the pool.
     * Remember: closing the connection returns it to the pool, it does not really close it.
     *
     * @return Connection from the pool
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    public static Users loadUser(long userId) throws SQLException {
        // profile could be null?
        try (Connection connection = Database.getConnection()) {

            String sql = "SELECT u.*, p.* FROM users u LEFT JOIN user_profiles p ON u.user_id = p.user_id WHERE u.user_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    // Build UserProfile object
                    UserProfile profile = new UserProfile(
                            rs.getLong("profile_id"),
                            rs.getString("profile_image_url"),
                            rs.getString("bio"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getTimestamp("updated_at"),
                            rs.getString("profile_name")
                    );

                    // Build Users object
                    Users user = new Users(
                            rs.getLong("user_id"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("last_seen"),
                            rs.getBoolean("is_verified"),
                            rs.getBoolean("is_online"),
                            rs.getBoolean("is_deleted"),
                            rs.getString("email"),
                            profile
                    );

                    return user;
                }
                else{return null;}
            }
        }
    }

    public static List<Users> loadAllUsers() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT u.user_id, u.created_at, u.last_seen, u.is_verified, u.is_online, u.is_deleted, u.email, " +
                    "p.profile_id, p.profile_image_url, p.bio, p.username, p.password, p.updated_at, p.profile_name " +
                    "FROM users u " +
                    "LEFT JOIN user_profiles p ON u.user_id = p.user_id;";

            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery();
            List<Users> usersList = new ArrayList<>();
            while (rs.next()) {
                UserProfile profile = null;
                if (rs.getLong("profile_id") != 0) {
                    profile = new UserProfile(
                            rs.getLong("profile_id"),
                            rs.getString("profile_image_url"),
                            rs.getString("bio"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getTimestamp("updated_at"),
                            rs.getString("profile_name")
                    );
                }

                Users user = new Users(
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen"),
                        rs.getBoolean("is_verified"),
                        rs.getBoolean("is_online"),
                        rs.getBoolean("is_deleted"),
                        rs.getString("email"),
                        profile
                );

                usersList.add(user);
            }
            return usersList;
        }
    }

    public static long getUserIdByEmail(String email) throws SQLException {
        try (Connection connection = Database.getConnection()){
            String sql = "SELECT user_id FROM users WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1,email);
            ResultSet rs = statement.executeQuery();
            if(rs.next()){
                return rs.getLong("user_id");
            }

        }
        return -1;
    }


    // ----- Messages -----
    public static List<MessageEntity> loadMessages(long chatId) throws SQLException {
        List<MessageEntity> messages = new ArrayList<>();

        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT m.message_id, m.chat_id, m.sender_id, m.message_text, m.message_type, " +
                    "m.reply, m.forward, m.is_edited, m.is_deleted, m.status, m.sent_at, " +
                    "u.user_id, u.email, u.is_online, u.is_verified, u.is_deleted, " +
                    "p.profile_id, p.username, p.profile_image_url " +
                    "FROM messages m " +
                    "LEFT JOIN users u ON m.sender_id = u.user_id " +
                    "LEFT JOIN user_profiles p ON u.user_id = p.user_id " +
                    "WHERE m.chat_id = ?";

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Build UserProfile
                UserProfile profile = null;
                if (rs.getLong("profile_id") != 0) {
                    profile = new UserProfile(
                            rs.getLong("profile_id"),
                            rs.getString("profile_image_url"),
                            null, // bio
                            rs.getString("username"),
                            null, // password
                            null, // updated_at
                            null  // profile_name
                    );
                }

                // Build Users
                Users sender = new Users(
                        rs.getLong("user_id"),
                        null, // created_at
                        null, // last_seen
                        rs.getBoolean("is_verified"),
                        rs.getBoolean("is_online"),
                        rs.getBoolean("is_deleted"),
                        rs.getString("email"),
                        profile
                );

                Chat chat = new PrivateChat(rs.getLong("chat_id"), 0, 0);

                // Build MessageEntity
                MessageEntity message = new MessageEntity(
                        MessageType.valueOf(rs.getString("status")),
                        rs.getBoolean("is_deleted"),
                        rs.getBoolean("is_edited"),
                        rs.getLong("forward"),
                        rs.getLong("reply"),
                        MessageType.valueOf(rs.getString("message_type")),
                        rs.getString("message_text"),
                        chat,
                        sender,
                        rs.getLong("message_id")
                );

                messages.add(message);
            }
        }

        return messages;
    }



    // ----- Chats -----
    public static PrivateChat loadPrivateChat(long chatId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT chat_id, user1_id, user2_id, created_at " +
                    "FROM private_chats " +
                    "WHERE chat_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PrivateChat chat = new PrivateChat(
                                rs.getLong("chat_id"),
                                rs.getLong("user1_id"),
                                rs.getLong("user2_id")
                        );
                        chat.setCreatedAt(rs.getTimestamp("created_at"));
                        return chat;
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    public static GroupChat loadGroupChat(long chatId) throws SQLException {
        // TODO: Load group chat by chatId
        return null;
    }

    public static Channel loadChannel(long chatId) throws SQLException {
        // TODO: Load channel by chatId
        return null;
    }
}
