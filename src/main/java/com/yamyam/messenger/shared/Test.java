package com.yamyam.messenger.shared;

import com.yamyam.messenger.server.database.Database;
import com.yamyam.messenger.server.database.UserHandler;

import java.sql.SQLException;

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
