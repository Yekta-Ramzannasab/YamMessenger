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
        // TODO: Implement DB query to get userId by email
        return 0;
    }

    public static Users checkOrCreateUser(String email) throws SQLException {
        // TODO: Check if user exists, if not create, then return the Users object
        return null;
    }

    // ----- Messages -----
    public static List<MessageEntity> loadMessages(long chatId) throws SQLException {
        // TODO: Load all messages of a chat
        return null;
    }

    public static void insertMessage(long chatId, long senderId, String message) throws SQLException {
        // TODO: Insert message into DB
    }

    // ----- Chats -----
    public static PrivateChat loadPrivateChat(long chatId) throws SQLException {
        // TODO: Load private chat by chatId
        return null;
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
