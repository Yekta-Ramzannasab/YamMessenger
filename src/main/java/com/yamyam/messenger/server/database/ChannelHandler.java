package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.Channel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChannelHandler {

    Connection con;

    public ChannelHandler(Connection con) {
        this.con = con;
    }

    public Channel checkOrCreateChannel(long ownerId, String name,boolean isPrivate) {
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String query = "INSERT INTO channel(channel_name,description,owner_id,is_private)"+
                "VALUES(?,?,?,?) RETURNING *";
        try(PreparedStatement st = con.prepareStatement(query)) {
            st.setString(1,name);
            st.setString(2,"");
            st.setLong(3,ownerId);
            st.setBoolean(4,isPrivate);
            ResultSet r = st.executeQuery();
            if(r.next()){
                return new Channel(
                        r.getLong("chat_id"),
                        r.getString("channel_name"),
                        r.getLong("owner_id"),
                        r.getBoolean("is_private"),
                        r.getString("description")
                );
            }
        }
        catch (SQLException e){
            throw new RuntimeException(e);
        }
        return null;
    }
}

