package com.yamyam.messenger.shared;


import com.yamyam.messenger.server.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserHandler {
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
        try(Connection con = Database.getConnection();
            PreparedStatement stmt = con.prepareStatement(sql)){
                stmt.setInt(1,user.getId());
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

            }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
