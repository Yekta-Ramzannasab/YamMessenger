package com.yamyam.messenger.shared;

import com.yamyam.messenger.server.Database;

import java.sql.SQLException;
import java.sql.Timestamp;

public class Test {
    public static void main(String[] ar){
        try{
            Database.getConnection();
            System.out.println("connected");
        }
        catch (SQLException e){
            System.out.println("Not connect!");
        }
        UserHandler uh = new UserHandler();
        System.out.println(uh.login("mobin2025","123456"));


    }
}
