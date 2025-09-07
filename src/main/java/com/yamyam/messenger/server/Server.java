package com.yamyam.messenger.server;

import com.yamyam.messenger.server.database.Database; // Your existing import

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Map; // Switched from List to Map
import java.util.concurrent.ConcurrentHashMap; // A thread-safe Map implementation

public class Server {

    // --- CHANGE 1: Switched from a List to a Map ---
    // A thread-safe map to store online clients: <UserId, ClientHandler>
    private static final Map<Long, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private static final int PORT = 5001;

    // --- CHANGE 2: Added helper methods to manage the new Map ---

    /**
     * Registers a new client handler when a user logs in.
     * @param userId The ID of the logged-in user.
     * @param handler The client handler instance associated with the user.
     */
    public static void registerClient(long userId, ClientHandler handler) {
        onlineClients.put(userId, handler);
        System.out.println("SERVER: User " + userId + " is now online. Total online: " + onlineClients.size());
    }

    /**
     * Unregisters a client handler when a user disconnects.
     * @param userId The ID of the user who went offline.
     */
    public static void unregisterClient(long userId) {
        if (userId > 0) {
            onlineClients.remove(userId);
            System.out.println("SERVER: User " + userId + " went offline. Total online: " + onlineClients.size());
        }
    }

    /**
     * Retrieves the client handler for a specific user if they are online.
     * @param userId The ID of the target user.
     * @return The ClientHandler instance, or null if the user is offline.
     */
    public static ClientHandler getClientHandler(long userId) {
        return onlineClients.get(userId);
    }


    public static void main(String[] args) {
        // Try to connect database
        try {
            Database.getConnection();
        } catch (SQLException e) {
            System.out.println("Not connect!");
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("new client connected");

                // --- CHANGE 3: We no longer need to pass the list to the handler ---
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                // We don't add the handler to a list here anymore.
                // It will be added to the Map upon successful login.
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}