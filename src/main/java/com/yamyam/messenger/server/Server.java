package com.yamyam.messenger.server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.yamyam.messenger.server.database.Database;

public class Server {
    // List of currently connected clients
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final int PORT = 5001;

    public static void main(String[] args) {
        // Try to connect database
        try{
            Database.getConnection();
        }
        catch (SQLException e){
            System.out.println("Not connect!");
        }

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
}