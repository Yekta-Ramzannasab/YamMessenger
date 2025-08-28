package com.yamyam.messenger.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MessageDatabaseHandler {


    public MessageDatabaseHandler(){}

    public void insertMessage(long chatId,long senderId,String message) throws SQLException {
        try (Connection con = Database.getConnection()) {


            String sql = "INSERT INTO messages(chat_id," +
                    "sender_id," +
                    "message_text," +
                    "message_type)" +
                    "VALUES(?,?,?,'text') RETURNING *";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, senderId);
                stmt.setString(3, message);
                try (ResultSet rs = stmt.executeQuery()) {
                }


            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }
    }
    public void getMessage(long chatId) throws SQLException {
        try (Connection con = Database.getConnection()) {

            String sql = "SELECT * FROM messages WHERE chat_id = ? AND is_deleted = false ORDER BY sent_at = ?";
            try (PreparedStatement st = con.prepareStatement(sql)) {
                st.setLong(1, chatId);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) ;
                /*
                if (!rs.getBoolean("is_deleted")) {
                }

                 */
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
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

