package com.yamyam.messenger.client.network;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.yamyam.messenger.server.database.SearchResult;
import com.yamyam.messenger.shared.model.*;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.lang.reflect.Type;

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
            Message response = receiveJsonMessage();

            if(response != null){
                String chatsJson = response.getContent();
                Gson gson = new Gson();

                // Use TypeToken to tell Gson to expect a list of type Chat
                Type chatListType = new TypeToken<ArrayList<Chat>>() {}.getType();

                // Convert json string to a real list
                return gson.fromJson(chatsJson, chatListType);
            }else
                return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    public String sendToAI(String prompt) {
        try {
            Message aiRequest = new Message(7, "Client", prompt);
            sendJsonMessage(aiRequest);

            Message aiResponse = receiveJsonMessage();
            return (aiResponse != null) ? aiResponse.getContent() : "no response";
        } catch (IOException e) {
            e.printStackTrace();
            return "error server";
        }
    }
    public List<PrivateChat> fetchPrivateChatsForUser(String email) throws IOException {
        Message request = new Message(3, email, "GET_PRIVATE_CHATS");
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            Type listType = new TypeToken<List<PrivateChat>>() {}.getType();
            return new Gson().fromJson(response.getContent(), listType);
        }

        return List.of();
    }
    public List<Chat> fetchGroupAndChannelChatsForUser(String email) throws IOException {
        Message request = new Message(8, email, "GET_GROUP_CHANNEL_CHATS");
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            Type listType = new TypeToken<List<Chat>>() {}.getType();
            return new Gson().fromJson(response.getContent(), listType);
        }

        return List.of();
    }
    public List<Contact> fetchContacts(String email) throws IOException {
        Message request = new Message(10, email, "GET_CONTACTS");
        sendJsonMessage(request);
        Message response = receiveJsonMessage();

        if (response != null && response.getContent() != null) {
            Type listType = new TypeToken<List<Contact>>() {}.getType();
            return new Gson().fromJson(response.getContent(), listType);
        }

        return List.of();
    }
    public List<Users> fetchAllUsers() throws IOException {
        Message request = new Message(11, "system", "GET_ALL_USERS");
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            Type listType = new TypeToken<List<Users>>() {}.getType();
            return new Gson().fromJson(response.getContent(), listType);
        }

        return List.of();
    }
    public List<SearchResult> fetchSearchResults(String query, String email) throws IOException {
        Message request = new Message(12, email, query);
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            Type listType = new TypeToken<List<SearchResult>>() {}.getType();
            return new Gson().fromJson(response.getContent(), listType);
        }

        return List.of();
    }
    public Users fetchUserById(long userId) throws IOException {
        Message request = new Message(13,"system",String.valueOf(userId));
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if(response != null && response.getContent() != null){
            return new Gson().fromJson(response.getContent(),Users.class);
        }
        return null;
    }
    public PrivateChat fetchOrCreatePrivateChat(long senderId, long receiverId) throws IOException {
        String content = senderId + "," + receiverId;
        Message request = new Message(14, "system", content);
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            return new Gson().fromJson(response.getContent(), PrivateChat.class);
        }

        return null;
    }
    public Channel fetchChannelById(long chatId) throws IOException {
        Message request = new Message(15, "system", String.valueOf(chatId));
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            return new Gson().fromJson(response.getContent(), Channel.class);
        }

        return null;
    }
    public ChannelSubscribers subscribeToChannel(Channel channel, long userId) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("chatId", channel.getChatId());
        payload.addProperty("userId", userId);

        Message request = new Message(16, "system", payload.toString());
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            return new Gson().fromJson(response.getContent(), ChannelSubscribers.class);
        }

        return null;
    }
    public GroupChat getOrCreateGroupChat(String name, String description, long creatorId, boolean isPrivate) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("description", description);
        payload.addProperty("creatorId", creatorId);
        payload.addProperty("isPrivate", isPrivate);

        Message request = new Message(17, "system", payload.toString());
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            return new Gson().fromJson(response.getContent(), GroupChat.class);
        }

        return null;
    }
    public GroupMembers joinGroupChat(GroupChat groupChat, Users member, Users invitedBy) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("chatId", groupChat.getChatId());
        payload.addProperty("memberId", member.getId());
        payload.addProperty("invitedById", invitedBy.getId());

        Message request = new Message(18, "system", payload.toString());
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            return new Gson().fromJson(response.getContent(), GroupMembers.class);
        }

        return null;
    }
}