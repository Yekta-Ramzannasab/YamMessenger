package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.UserProfile;
import com.yamyam.messenger.shared.model.Users;

import java.io.FileDescriptor;
import java.sql.*;

public class UserHandler {

    // Constructor no longer needs a connection, each method fetches it from Database pool
    public UserHandler() {}

    /**
     * Check if a user with the given email exists.
     * If not, create a new user and associated profile.
     * Uses HikariCP connection pool and try-with-resources for safety.
     *
     * @param email User's email
     * @return Users object representing existing or newly created user
     * @throws SQLException if a database error occurs
     */
    public Users checkOrCreateUser(String email) throws SQLException {
        // Get connection from HikariCP pool
        try (Connection connection = Database.getConnection()) {

            // 1. Check if user already exists
            String sqlCheck = "SELECT u.*, p.* FROM users u LEFT JOIN user_profiles p ON u.user_id = p.user_id WHERE u.email = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sqlCheck)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    // If user exists, optionally update verification status
                    long userId = rs.getLong("user_id");
                    String sqlUpdateUser = "UPDATE users SET is_verified = true WHERE user_id = ?";
                    try (PreparedStatement stm = connection.prepareStatement(sqlUpdateUser)) {
                        stm.setLong(1, userId);
                        stm.executeUpdate();
                    }

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
            }

            // 2. If user does not exist, insert new user
            String sqlInsert = "INSERT INTO users (created_at, last_seen, is_verified, is_online, is_deleted, email) " +
                    "VALUES (now(), null, false, false, false, ?) RETURNING user_id, created_at";

            try (PreparedStatement stmt = connection.prepareStatement(sqlInsert)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    long newUserId = rs.getLong("user_id");

                    // Insert default user profile
                    String sqlProfile = "INSERT INTO user_profiles(user_id, profile_image_url, bio, updated_at, username, password, profile_name) " +
                            "VALUES (?, null, null, now(), null, null, null)";
                    try (PreparedStatement profileStmt = connection.prepareStatement(sqlProfile)) {
                        profileStmt.setLong(1, newUserId);
                        profileStmt.executeUpdate();
                    }

                    // Build default profile and user objects
                    UserProfile profile = new UserProfile(
                            newUserId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    );

                    Users newUser = new Users(
                            newUserId,
                            rs.getTimestamp("created_at"),
                            new Timestamp(System.currentTimeMillis()),
                            false,
                            false,
                            false,
                            email,
                            profile
                    );

                    return newUser;
                }
            }

        }

        return null;
    }


}
