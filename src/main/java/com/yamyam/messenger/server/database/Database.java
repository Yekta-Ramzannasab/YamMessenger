package com.yamyam.messenger.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {

    // Static HikariDataSource, acts like a singleton for the whole app
    private static final HikariDataSource dataSource;

    static {
        // Read environment variables for database credentials
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        // Check if any required environment variable is missing
        if (url == null || user == null || password == null) {
            throw new RuntimeException("Database environment variables (DB_URL, DB_USER, DB_PASSWORD) are not set!");
        }

        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);                     // Database URL
        config.setUsername(user);                   // Database username
        config.setPassword(password);               // Database password

        // Pool settings
        config.setMaximumPoolSize(10);             // Maximum number of connections in the pool
        config.setMinimumIdle(2);                  // Minimum number of idle connections kept in the pool
        config.setIdleTimeout(30000);              // 30 seconds before an idle connection is released
        config.setMaxLifetime(1800000);            // 30 minutes maximum lifetime for a connection
        config.setConnectionTimeout(10000);        // Wait up to 10 seconds for a connection

        // Initialize the connection pool
        dataSource = new HikariDataSource(config);
    }

    // Private constructor to prevent instantiation (singleton-like)
    private Database() {}

    /**
     * Get a connection from the pool.
     * Remember: closing the connection returns it to the pool, it does not really close it.
     *
     * @return Connection from the pool
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
