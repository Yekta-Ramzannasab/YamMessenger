package com.yamyam.messenger.client.network;

import com.google.gson.reflect.TypeToken;
import com.yamyam.messenger.shared.model.Chat;
import com.yamyam.messenger.shared.model.Message;
import com.google.gson.Gson;
import com.yamyam.messenger.shared.model.Users;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class NetworkService {
    private final int PORT = 5001;

    // Singleton section
    private static NetworkService instance;
    private Socket socket;

    private NetworkService() {
        try {
            // We establish the connection and store it in a class variable
            this.socket = new Socket("localhost", PORT);
            this.binaryIn = new DataInputStream(this.socket.getInputStream());
            this.binaryOut = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("CRITICAL: Could not connect to the server. " + e.getMessage());
            throw new RuntimeException("Failed to connect to the server", e);
        }
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }

    private DataInputStream binaryIn;
    private DataOutputStream binaryOut;

    public String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashedBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void sendJsonMessage(Message message) throws IOException {
        // convert message into a json string then convert it to a byte array
        String json = new Gson().toJson(message);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // first send length of message
        binaryOut.writeInt(jsonBytes.length);

        // then send array
        binaryOut.write(jsonBytes);
        binaryOut.flush();
    }

    private Message receiveJsonMessage() throws IOException {
        // reading length of response
        int length = binaryIn.readInt();
        if (length > 0) {
            byte[] jsonBytes = new byte[length];
            binaryIn.readFully(jsonBytes, 0, length);// reading response
            String jsonResponse = new String(jsonBytes, StandardCharsets.UTF_8);

            return new Gson().fromJson(jsonResponse, Message.class);
        }
        return null; // If the message length is zero, return null
    }

    public Users clientHandleLogin (String email ) throws Exception {
        // create a message and send to server for login request
        Message loginMessage = new Message( 1 , email , "CHECK_WITH_DATABASE" ) ;
        sendJsonMessage(loginMessage);

        loginMessage = receiveJsonMessage();

        if(loginMessage != null){
            // parse content
            Users user ;
            user = Users.fromString(loginMessage.getContent()) ;

            // send user to ui
            return user ;
        }else
            return null;
    }

    public Integer requestVerificationCode(String email) {
        try {
            // Message type 2 to request a verification code
            Message request = new Message(2, email, "SEND_CODE");

            sendJsonMessage(request);
            request = receiveJsonMessage();

            if(request != null){
                try {
                    return Integer.parseInt(request.getContent());
                } catch (NumberFormatException e) {
                    // If the server does not return a number (e.g. an error message)
                    System.err.println("Server did not return a valid code: " + request.getContent());
                    return null;
                }
            }else
                return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If any communication problem occurs, return null
        return null;
    }

    public List<Chat> fetchMyChatList(String email) {
        try {
            // Request for chat list
            Message message = new Message(3 , email, "GET_CHATS");
            sendJsonMessage(message);

            // Reading response
            message = receiveJsonMessage() ;

            if(message != null){
                Gson gson = new Gson();
                return gson.fromJson(
                        message.getContent(),
                        new TypeToken<List<Chat>>() {}.getType()
                );
            }else
                return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}