package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.Channel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChannelHandler {

    // Constructor is no longer needed because we will fetch connection from Pool directly
    public ChannelHandler() {}

    /**
     * Check if a channel exists for given ownerId and name.
     * If not, create it. Uses HikariCP connection pool.
     *
     * @param ownerId Owner user ID
     * @param name Channel name
     * @param isPrivate Channel privacy flag
     * @return Channel object from database
     */
    public Channel checkOrCreateChannel(long ownerId, String name, boolean isPrivate) {
        // Try-with-resources: connection auto-closed and returned to pool
        try (Connection con = Database.getConnection()) {

            // 1. Check if channel exists
            String sql = "SELECT * FROM channel WHERE owner_id = ? AND channel_name = ?";
            try (PreparedStatement stm = con.prepareStatement(sql)) {
                stm.setLong(1, ownerId);
                stm.setString(2, name);
                ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    return new Channel(
                            rs.getLong("chat_id"),
                            rs.getString("channel_name"),
                            rs.getLong("owner_id"),
                            rs.getBoolean("is_private"),
                            rs.getString("description")
                    );
                }
            }

            // 2. If not exists, insert new channel
            String insertSQL = "INSERT INTO channel(channel_name, description, owner_id, is_private) " +
                    "VALUES (?, ?, ?, ?) RETURNING *";
            try (PreparedStatement st = con.prepareStatement(insertSQL)) {
                st.setString(1, name);
                st.setString(2, ""); // default description
                st.setLong(3, ownerId);
                st.setBoolean(4, isPrivate);
                ResultSet r = st.executeQuery();
                if (r.next()) {
                    return new Channel(
                            r.getLong("chat_id"),
                            r.getString("channel_name"),
                            r.getLong("owner_id"),
                            r.getBoolean("is_private"),
                            r.getString("description")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error in checkOrCreateChannel: " + e.getMessage(), e);
        }

        return null;
    }
}
