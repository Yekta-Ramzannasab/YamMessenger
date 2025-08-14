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
            UserHandler uh = new UserHandler(Database.getConnection());
            System.out.println(uh.checkOrCreateUser("amir@email").isVerified());
        }
        catch (SQLException e) {
            e.printStackTrace();

        }
    }
}
