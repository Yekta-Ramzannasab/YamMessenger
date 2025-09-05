package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.message.MessageEntity;
import com.yamyam.messenger.shared.model.MessageStatus;
import com.yamyam.messenger.shared.model.MessageType;
import com.yamyam.messenger.shared.model.PrivateChat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MessageDatabaseHandler {


    public MessageDatabaseHandler(){}
    public MessageEntity insertMessage(long chatId, long senderId, String text) throws SQLException {
        try (Connection con = Database.getConnection()) {
            String sql = "INSERT INTO messages(chat_id, sender_id, message_text, message_type, status, is_deleted, is_edited) " +
                    "VALUES (?, ?, ?, ?, 'sent', false, false) RETURNING *";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, senderId);
                stmt.setString(3, text);
                stmt.setString(4,"text");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long forward = rs.getLong("forwarded_from_message_id");
                        if (rs.wasNull()) forward = 0;

                        long reply = rs.getLong("reply_to_message_id");
                        if (rs.wasNull()) reply = 0;

                        return new MessageEntity(
                                MessageStatus.valueOf(rs.getString("status")),
                                rs.getBoolean("is_deleted"),
                                rs.getBoolean("is_edited"),
                                forward,
                                reply,
                                MessageType.valueOf(rs.getString("message_type")),
                                rs.getString("message_text"),
                                new PrivateChat(rs.getLong("chat_id"), 0, 0),
                                DataManager.getInstance().getUser(senderId),
                                rs.getLong("message_id")
                        );
                    }
                }
            }
        }
        return null;
    }
    public boolean deleteMessage(long messageId,long senderId) throws SQLException {
        try (Connection con = Database.getConnection()) {

            String sql = "UPDATE messages SET is_deleted = true , updated_at = NOW()" +
                    "WHERE message_id = ? AND sender_id = ?";
            try (PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setLong(1, messageId);
                statement.setLong(2, senderId);
                int updated = statement.executeUpdate();
                return updated > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public boolean editMessage(long messageId,long senderId,String newText) throws SQLException {
        try (Connection con = Database.getConnection()) {

            String sql = "UPDATE messages " +
                    "SET message_text = ?, is_edited = TRUE, updated_at = now() " +
                    "WHERE message_id = ? AND sender_id = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, newText);
                stmt.setLong(2, messageId);
                stmt.setLong(3, senderId);
                int updated = stmt.executeUpdate();
                return updated > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

