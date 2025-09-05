package com.yamyam.messenger.client.network;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.client.network.dto.ContactType;
import com.yamyam.messenger.client.network.dto.MessageDto;
//import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.server.database.DataManager;
import com.yamyam.messenger.server.database.SearchResult;
import com.yamyam.messenger.shared.model.*;
import com.google.gson.Gson;
import com.yamyam.messenger.shared.model.chat.*;
import com.yamyam.messenger.shared.model.message.MessageEntity;
import com.yamyam.messenger.shared.model.user.ContactRelation;
import com.yamyam.messenger.shared.model.user.UserProfile;
import com.yamyam.messenger.shared.model.user.Users;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
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
            System.out.println(loginMessage.getType());


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
    public List<ContactRelation> fetchContacts(String email) throws IOException {
        Message request = new Message(10, email, "GET_CONTACTS");
        sendJsonMessage(request);
        Message response = receiveJsonMessage();

        if (response != null && response.getContent() != null) {
            Type listType = new TypeToken<List<ContactRelation>>() {}.getType();
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
    public List<Users> fetchSearchResults(String query, String email) throws IOException {
        Message request = new Message(12, email, query);
        sendJsonMessage(request);

        Message response = receiveJsonMessage();

        if (response == null) {
            System.err.println("❌ No response received from server");
            return List.of();
        }

        String raw = response.getContent();
        System.out.println("📥 Raw response content:\n" + raw);
        System.out.println("🔍 From user: " + request.getSender());

        if (raw == null || raw.isBlank()) {
            System.out.println("ℹ️ Response content is empty");
            return List.of();
        }

        String[] lines = raw.split("\n");
        System.out.println("📦 Received " + lines.length + " raw line(s)");
        Arrays.stream(lines).forEach(line -> System.out.println(" - " + line));

        List<Users> results = Arrays.stream(lines)
                .map(SearchResult::fromString)
                .filter(Objects::nonNull)
                .toList();

        System.out.println("✅ Converted to " + results.size() + " SearchResult(s)");
        return results;
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
    public static List<Chat> fetchAllChatsForUser(long userId) {
        try {
            return DataManager.getInstance().getAllChatsForUser(userId);
        } catch (SQLException e) {
            System.err.println("[NetworkService] Failed to fetch chats for user " + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
    public static Contact fetchContactDetailsById(long userId) {
        try {
            Users user = DataManager.getInstance().getUser(userId);
            if (user == null) return null;

            UserProfile profile = user.getUserProfile();
            return new Contact(
                    user.getId(),
                    profile.getProfileName(),
                    profile.getProfileImageUrl(),
                    user.isOnline(),
                    ContactType.DIRECT,
                    null
            );
        } catch (SQLException e) {
            System.err.println("[NetworkService] Failed to fetch contact for user " + userId + ": " + e.getMessage());
            return null;
        }
    }

    public void fillUserFirstProfile (String email,String fullName,String username,String bio) throws IOException {
        UserProfile userProfile = new UserProfile();

        userProfile.setBio(bio);
        userProfile.setProfileName(fullName);
        userProfile.setUsername(username);

        Message message = new Message(4,email,userProfile.toString());
        sendJsonMessage(message);
    }
    public List<MessageEntity> fetchMessages(long chatId) throws IOException {
        Message request = new Message(19, "system", String.valueOf(chatId));
        sendJsonMessage(request);

        Message response = receiveJsonMessage();
        if (response != null && response.getContent() != null) {
            return new Gson().fromJson(response.getContent(), new TypeToken<List<MessageEntity>>() {}.getType());
        }

        return Collections.emptyList();
    }

//    public void sendChatMessage(long chatId, String text) throws IOException {
//        // یک پیام برای ارسال به سرور می‌سازیم
//        // فرض می‌کنیم نوع پیام ۱ برای پیام‌های چت است
//        // در محتوا، می‌توانیم ID چت را هم قرار دهیم تا سرور بداند پیام برای کجاست
//        // یا می‌توانیم از فیلد sender برای ارسال ID چت استفاده کنیم.
//        // فعلاً یک راه ساده را انتخاب می‌کنیم:
//        String payload = chatId + ":" + text;
//        Message chatMessage = new Message(1, "me", payload); // "me" باید با ID کاربر لاگین کرده جایگزین شود
//
//        // از متد کمکی که از قبل داشتیم برای ارسال استفاده می‌کنیم
//        sendJsonMessage(chatMessage);
//    }

    public void sendChatMessage(long chatId, String text) throws IOException {
        // یک کد نوع (type) جدید برای ارسال پیام در نظر می‌گیریم.
        // چون 19 برای fetchMessages است، از 20 استفاده می‌کنیم.
        final int MESSAGE_TYPE_SEND_CHAT = 20;

        // شناسه‌ی کاربری که لاگین کرده را از AppSession می‌گیریم
        long senderId = AppSession.requireUserId();

        // ۱. ساخت DTO با اطلاعات لازم
        MessageDto messageDto = new MessageDto(chatId, senderId, text);

        // ۲. تبدیل DTO به یک رشته JSON (این رشته محتوای پیام ما خواهد بود)
        String payload = new Gson().toJson(messageDto);

        // ۳. ساخت آبجکت Message اصلی طبق پروتکل شما
        // sender را می‌توانیم شناسه کاربر قرار دهیم تا سرور لاگ بهتری داشته باشد.
        Message messageToServer = new Message(MESSAGE_TYPE_SEND_CHAT, String.valueOf(senderId), payload);

        // ۴. ارسال پیام نهایی با استفاده از متد کمکی موجود
        System.out.println("🚀 Sending chat message to server. Type: 20, Payload: " + payload);
        sendJsonMessage(messageToServer);
    }
}