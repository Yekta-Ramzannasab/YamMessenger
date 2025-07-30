package com.yamyam.messenger.shared;

import com.yamyam.messenger.server.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Test {
    public static void main(String[] ar) {
        try {
            Database.getConnection();
            System.out.println("connected");
        } catch (SQLException e) {
            System.out.println("Not connect!");
        }
        try {
            String sq = "INSERT INTO users(username,email) VALUES(?,?)";
            Connection con = Database.getConnection();
            PreparedStatement st = con.prepareStatement(sq);
            st.setString(1,"mobin2025");
            st.setString(2,"mobin@email");
            st.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();

        }
    }
}
