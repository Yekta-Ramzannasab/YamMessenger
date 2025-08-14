package com.yamyam.messenger.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    // Information about environmental changes is read
    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        // We check whether the variables are defined in the environment or not
        if (URL == null || USER == null || PASSWORD == null) {
            System.err.println("FATAL ERROR: Database environment variables (DB_URL, DB_USER, DB_PASSWORD) are not set!");
            System.err.println("Please configure them in your IntelliJ Run Configuration.");
            throw new SQLException("Missing database credentials in environment variables.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}