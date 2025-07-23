package com.yamyam.messenger.shared;

import com.yamyam.messenger.server.Database;

import java.sql.SQLException;
import java.sql.Timestamp;

public class Test {
    public static void main(String[] ar){
        try{
            Database.getConnection();
        }
        catch (SQLException e){
            System.out.println("Not connect!");
        }
        UserHandler handler = new UserHandler();

        Users user = new Users(
                0,
                "mobin2025",
                "Mobin T",
                "hi i'm mobin",
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()),
                true,
                true,
                false,
                "mobin@example.com",
                "123456"
        );

        boolean success = handler.registerUser(user);

        if (success) {
            System.out.println("yessss");
        } else {
            System.out.println("nooo");
        }


    }
}
