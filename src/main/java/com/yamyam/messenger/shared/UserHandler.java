package com.yamyam.messenger.shared;


import com.yamyam.messenger.server.Database;

import java.security.NoSuchAlgorithmException;
import java.sql.*;

import static com.yamyam.messenger.client.network.NetworkService.hashPassword;

import java.sql.*;

public class UserHandler {

    private Connection connection;

    public UserHandler(Connection connection) {
        this.connection = connection;
    }

    public Users checkOrCreateUser(String email) throws SQLException {
        String sqlCheck = "SELECT u.*, p.* FROM users u LEFT JOIN user_profiles p ON u.id = p.user_id WHERE u.email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sqlCheck)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                UserProfile profile = new UserProfile(
                        rs.getLong("profile_id"),
                        rs.getString("profile_image_url"),
                        rs.getString("bio"),
                        rs.getBoolean("is_active"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getTimestamp("updated_at")
                );

                Users user = new Users(
                        rs.getLong("id"),
                        rs.getString("profile_name"),
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


        String sqlInsert = "INSERT INTO users (email, is_verified, is_online, is_deleted, created_at) VALUES (?, false, false, false, NOW()) RETURNING id";

        try (PreparedStatement stmt = connection.prepareStatement(sqlInsert)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long newUserId = rs.getLong("id");
                Users newUser = new Users(
                        newUserId,
                        null,
                        new Timestamp(System.currentTimeMillis()),
                        null,
                        false,
                        false,
                        false,
                        email,
                        null
                );
                return newUser;
            }
        }

        return null;
    }


    public Users completeUserProfile(long userId, String profileName, String username, String bio, String profileImageUrl) throws SQLException {

        String sqlUpdateUser = "UPDATE users SET profile_name = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sqlUpdateUser)) {
            stmt.setString(1, profileName);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }

        String sqlInsertProfile = "INSERT INTO user_profiles (user_id, username, bio, profile_image_url, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, true, NOW(), NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sqlInsertProfile)) {
            stmt.setLong(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, bio);
            stmt.setString(4, profileImageUrl);
            stmt.executeUpdate();
        }

        return getUserById(userId);
    }


    public Users getUserById(long userId) throws SQLException {
        String sql = "SELECT u.*, p.* FROM users u LEFT JOIN user_profiles p ON u.id = p.user_id WHERE u.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                UserProfile profile = new UserProfile(
                        rs.getLong("profile_id"),
                        rs.getString("profile_image_url"),
                        rs.getString("bio"),
                        rs.getBoolean("is_active"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getTimestamp("updated_at")
                );

                Users user = new Users(
                        rs.getLong("id"),
                        rs.getString("profile_name"),
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
        return null;
    }
}

