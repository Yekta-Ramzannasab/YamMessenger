package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.PrivateChat;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrivateChatHandler {

    private Connection con;

    public PrivateChatHandler(Connection con){this.con = con;}

    public PrivateChat checkOrCreateChat(long sender,long receiver) { // Or overload this method by getting User object!
        sender = Math.min(sender, receiver);
        receiver = Math.max(sender, receiver);

        String sql = "SELECT * FROM private_chat WHERE " +
                "user1_id = ? AND user2_id = ?";
        try (PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setLong(1, sender);
            stm.setLong(2, receiver);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new PrivateChat(
                        rs.getLong("chat_id"),
                        rs.getLong("user1_id"),
                        rs.getLong("user2_id")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading private chat",e);
        }
        String query = "INSERT INTO private_chat(user1_id,user2_id)" +
                "VALUES(?,?) RETURNING *";
        try (PreparedStatement st = con.prepareStatement(query)) {
            st.setLong(1,sender);
            st.setLong(2,receiver);
            ResultSet rls = st.executeQuery();
            if(rls.next()){
                return new PrivateChat( //another way to return private chat ?oh...
                        rls.getLong("chat_id"),
                        rls.getLong("user1_id"),
                        rls.getLong("user2_id"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error creating private chat",e);
        }
        return null;

    }

}
