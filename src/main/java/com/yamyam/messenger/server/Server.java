package com.yamyam.messenger.server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.shared.User;

public class Server {
    // Predefined users for authentication
    private static final User[] users;

    static {
        try {
            users = new User[]{
                    new User("user1", NetworkService.hashPassword("1")),
                    new User("user2", NetworkService.hashPassword("2")),
                    new User("user3", NetworkService.hashPassword("3")),
                    new User("user4", NetworkService.hashPassword("4")),
                    new User("user5", NetworkService.hashPassword("5")),
            };
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // List of currently connected clients
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final int PORT = 5001;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server is listening on   port " + PORT );
            while (true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("new client connected");

                ClientHandler clientHandler = new ClientHandler(clientSocket,clients);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static boolean authenticate(String username, String password) {
        for (User user : users) {
            if (user.username().equals(username) && user.password().equals(password)) {
                return true;
            }
        }
        return false;
    }
}