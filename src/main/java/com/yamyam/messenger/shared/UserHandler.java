package com.yamyam.messenger.shared;


import com.yamyam.messenger.server.Database;

import java.security.NoSuchAlgorithmException;
import java.sql.*;

import static com.yamyam.messenger.client.network.NetworkService.hashPassword;

public class UserHandler {
    public boolean registerUser(String username, String email) {
        String sql = "INSERT INTO users (username,email)" +
                "VALUES(?,?)";
        try {
            Connection con = Database.getConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.executeUpdate();
            return true;

        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Users CompleteRegisterUser(String profileName, String password, String username) {
        // Username saved with UI
        String sql = "UPDATE users SET profile_name = ? , password = ? , is_verified = true , updated_at = ? WHERE username = ? AND is_verified = false ";
        try {
            Connection con = Database.getConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, profileName);
            stmt.setString(2, hashPassword(password));
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setString(4, username);
            int updated = stmt.executeUpdate();
            if (updated == 1){
                String sq = "SELECT * FROM users WHERE username = ?";
                PreparedStatement selectStmt = con.prepareStatement(sq);
                selectStmt.setString(1, username);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    // New USER
                    return new Users(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("profile_name"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("last_seen"),
                            rs.getBoolean("is_verified"),
                            rs.getBoolean("is_online"),
                            rs.getBoolean("is_deleted"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getTimestamp("updated_at"));
                }

            }

        }
        catch (NoSuchAlgorithmException | SQLException e) {
            e.printStackTrace();

        }
        return null;
    }

    public boolean login(String username, String password) {
        String sql = "SELECT username,password FROM users WHERE username = ?\n" +
                "AND password = ? AND is_verified = true";
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

    /*
    public boolean registerUser(Users user) {

        String sql = "INSERT INTO users (user_id ,username," +
                " profile_name, " +
                "bio," +
                "created_at, " +
                "last_seen, " +
                "is_verified, " +
                "is_online, " +
                "is_deleted, " +
                "email, " +
                "password)" +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getProfileName());
            stmt.setString(4, user.getBio());
            stmt.setTimestamp(5, user.getCreateAt());
            stmt.setTimestamp(6, user.getLastSeen());
            stmt.setBoolean(7, user.isVerified());
            stmt.setBoolean(8, user.isOnline());
            stmt.setBoolean(9, user.isDeleted());
            stmt.setString(10, user.getEmail());
            stmt.setString(11, user.getPassword());


            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
    */

