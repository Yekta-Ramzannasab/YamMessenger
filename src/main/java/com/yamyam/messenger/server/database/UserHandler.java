package com.yamyam.messenger.server.database;


import com.yamyam.messenger.shared.model.UserProfile;
import com.yamyam.messenger.shared.model.Users;

import java.sql.*;

import static java.time.LocalTime.now;

public class UserHandler {

    private Connection connection;

    public UserHandler(Connection connection) {
        this.connection = connection;
    }

    public Users checkOrCreateUser(String email) throws SQLException {
        String sqlCheck = "SELECT u.*, p.* FROM users u LEFT JOIN user_profiles p ON u.user_id = p.user_id WHERE u.email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sqlCheck)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();


            if (rs.next()) {
                long userId = rs.getLong("user_id");
                String sqlUpdateUser = "UPDATE users SET is_verified = true WHERE user_id = ?";
                try (PreparedStatement stm = connection.prepareStatement(sqlUpdateUser)) {

                    stm.setLong(1, userId);
                    stm.executeUpdate();
                }
                UserProfile profile = new UserProfile(
                        rs.getLong("profile_id"),
                        rs.getString("profile_image_url"),
                        rs.getString("bio"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getTimestamp("updated_at"),
                        rs.getString("profile_name")
                );

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


        String sqlInsert = "INSERT INTO users (created_at," +
                "last_seen," +
                "is_verified," +
                " is_online," +
                " is_deleted," +
                "email) " +
                "VALUES ( now(), null, false,false,false,?) RETURNING user_id ,created_at";

        try (PreparedStatement stmt = connection.prepareStatement(sqlInsert)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String sql = "INSERT INTO user_profiles(user_id," +
                        "profile_image_url," +
                        "bio," +
                        "updated_at," +
                        "username," +
                        "password," +
                        "profile_name)" +
                        "VALUES (?,null,null,now(),null,null,'username')";
                long newUserId = rs.getLong("user_id");
                try (PreparedStatement profileStmt = connection.prepareStatement(sql)) {
                    profileStmt.setLong(1, newUserId);
                    profileStmt.executeUpdate();
                }

                UserProfile profile = new UserProfile(
                        newUserId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "username"


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

        return null;
    }
}
    /*


    public Users completeUserProfile(long userId, String profileName, String username, String bio, String profileImageUrl) throws SQLException {

        String sqlUpdateUser = "UPDATE users SET profile_name = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sqlUpdateUser)) {
            stmt.setString(1, profileName);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }

        String sqlInsertProfile = "INSERT INTO user_profiles (user_id, username, bio, profile_image_url, is_active, updated_at) " +
                "VALUES (?, ?, ?, ?, true, NOW())";
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

     */

