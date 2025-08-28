package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.PrivateChat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrivateChatHandler {

    // Constructor no longer needs a connection, each method fetches it from Database pool
    public PrivateChatHandler() {}

    /**
     * Check if a private chat exists between two users.
     * If not, create a new private chat.
     * Uses HikariCP connection pool and try-with-resources.
     *
     * @param sender User ID of sender
     * @param receiver User ID of receiver
     * @return PrivateChat object representing existing or newly created chat
     */
    public PrivateChat checkOrCreateChat(long sender, long receiver) {
        // Ensure consistent ordering for user1 and user2
        long user1 = Math.min(sender, receiver);
        long user2 = Math.max(sender, receiver);

        try (Connection con = Database.getConnection()) {

            // 1. Check if chat exists
            String sqlSelect = "SELECT * FROM private_chat WHERE user1_id = ? AND user2_id = ?";
            try (PreparedStatement stm = con.prepareStatement(sqlSelect)) {
                stm.setLong(1, user1);
                stm.setLong(2, user2);
                ResultSet rs = stm.executeQuery();

                if (rs.next()) {
                    return new PrivateChat(
                            rs.getLong("chat_id"),
                            rs.getLong("user1_id"),
                            rs.getLong("user2_id")
                    );
                }
            }

            // 2. If chat does not exist, insert new chat
            String sqlInsert = "INSERT INTO private_chat(user1_id, user2_id) VALUES (?, ?) RETURNING *";
            try (PreparedStatement st = con.prepareStatement(sqlInsert)) {
                st.setLong(1, user1);
                st.setLong(2, user2);
                ResultSet rs = st.executeQuery();

                if (rs.next()) {
                    return new PrivateChat(
                            rs.getLong("chat_id"),
                            rs.getLong("user1_id"),
                            rs.getLong("user2_id")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error in checkOrCreateChat", e);
        }

        return null;
    }
}
