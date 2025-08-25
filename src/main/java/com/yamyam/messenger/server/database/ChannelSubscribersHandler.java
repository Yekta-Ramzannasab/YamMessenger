package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.Channel;
import com.yamyam.messenger.shared.model.ChannelSubscribers;
import com.yamyam.messenger.shared.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChannelSubscribersHandler {

    // Constructor no longer needs a connection; each method fetches it from Database pool
    public ChannelSubscribersHandler() {}

    /**
     * Check if a user is already subscribed to a channel.
     * If not, subscribe the user with default role 'member'.
     * Uses HikariCP connection pool and try-with-resources.
     *
     * @param channel The channel object
     * @param subscribeId The user ID to subscribe
     * @return ChannelSubscribers object representing existing or newly subscribed user
     */
    public ChannelSubscribers checkOrSubscribeUser(Channel channel, long subscribeId) {
        try (Connection con = Database.getConnection()) {

            // 1. Check if user already subscribed
            String sqlSelect = "SELECT * FROM channel_subscribers WHERE user_id = ? AND chat_id = ?";
            try (PreparedStatement stmt = con.prepareStatement(sqlSelect)) {
                stmt.setLong(1, subscribeId);
                stmt.setLong(2, channel.getChatId());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Role role = Role.valueOf(rs.getString("role").toUpperCase());
                    return new ChannelSubscribers(
                            channel,
                            role,
                            rs.getLong("user_id"),
                            rs.getBoolean("is_approved")
                    );
                }
            }

            // 2. If not subscribed, insert new subscription with default role 'member'
            String sqlInsert = "INSERT INTO channel_subscribers(chat_id, user_id, role, joined_at, is_approved) " +
                    "VALUES (?, ?, ?, now(), true) RETURNING *";

            try (PreparedStatement stmt = con.prepareStatement(sqlInsert)) {
                stmt.setLong(1, channel.getChatId());
                stmt.setLong(2, subscribeId);
                stmt.setString(3, "member");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new ChannelSubscribers(
                                channel,
                                Role.valueOf(rs.getString("role").toUpperCase()),
                                rs.getLong("user_id"),
                                rs.getBoolean("is_approved")
                        );
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error in checkOrSubscribeUser", e);
        }

        return null;
    }
}
