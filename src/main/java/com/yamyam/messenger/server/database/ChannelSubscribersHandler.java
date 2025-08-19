package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.Channel;
import com.yamyam.messenger.shared.model.ChannelSubscribers;
import com.yamyam.messenger.shared.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChannelSubscribersHandler {

    private Connection con;

    public ChannelSubscribersHandler(Connection con){this.con = con;}

    public ChannelSubscribers checkOrSubscribeUser(Channel channel , long subscribeId){
        String sql = "SELECT * FROM channel_subscribers WHERE user_id = ? AND chat_id";
        try (PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setLong(1,subscribeId);
            stm.setLong(2,channel.getChatId());
            ResultSet rs = stm.executeQuery();
            if(rs.next()) {
                Role role = Role.valueOf(rs.getString("role").toUpperCase());

                return new ChannelSubscribers(
                        channel,
                        role,
                        rs.getLong("user_id"),
                        rs.getBoolean("is_approved")
                );
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String query = "INSERT INTO channel_subscribers(chat_id,user_id,role,joined_at,is_approved)"+
                "VALUES (?,?,?,now(),true) RETURNING *";
        try (PreparedStatement stt = con.prepareStatement(query)) {
            stt.setLong(1,channel.getChatId());
            stt.setLong(2,subscribeId);
            stt.setString(3,"member");

            try (ResultSet rs = stt.executeQuery()) {
                if(rs.next()) {
                    return new ChannelSubscribers(
                            channel,
                            Role.valueOf(rs.getString("role").toUpperCase()),
                            rs.getLong("user_id"),
                            rs.getBoolean("is_approved")
                    );
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;

    }
}
