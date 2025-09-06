package com.yamyam.messenger.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yamyam.messenger.client.network.dto.MessageDto;
import com.yamyam.messenger.server.database.*;
import com.yamyam.messenger.server.services.EmailService;
import com.yamyam.messenger.shared.model.*;
import com.google.gson.Gson;
import com.yamyam.messenger.shared.model.chat.*;
import com.yamyam.messenger.shared.model.message.MessageEntity;
import com.yamyam.messenger.shared.model.user.ContactRelation;
import com.yamyam.messenger.shared.model.user.UserProfile;
import com.yamyam.messenger.shared.model.user.Users;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static com.yamyam.messenger.server.database.Database.getUserIdByEmail;
import static com.yamyam.messenger.server.database.Database.updateUserProfile;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson;

    private final DataInputStream binaryIn;
    private final DataOutputStream binaryOut;

    private final List allClients;

    public ClientHandler(Socket socket , List allClients) throws IOException {
        this.socket = socket;
        this.gson = new Gson();

        this.binaryIn = new DataInputStream(socket.getInputStream());
        this.binaryOut = new DataOutputStream(socket.getOutputStream());

        this.allClients = allClients ;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // first read length os message
                int length = binaryIn.readInt();

                // if length exist continue
                if (length > 0) {
                    // make a byte array with length of message
                    byte[] jsonBytes = new byte[length];

                    // read bytes until it is full
                    binaryIn.readFully(jsonBytes, 0, length);

                    // convert byte array into json string
                    String jsonRequest = new String(jsonBytes, StandardCharsets.UTF_8);

                    // implementing a request on a message
                    Message request = gson.fromJson(jsonRequest, Message.class);
                    if (request == null) {
                        continue;
                    }

                    switch (request.getType()) { // do tasks base on request type
                        case 1:
                            Users user = handleLogin(request.getSender());
                            Message loginUser = null;
                            if (user != null) {
                                loginUser = new Message(1, "Server", user.toString() );
                            }
                            sendJsonMessage(loginUser);
                            break;
                        case 2:
                            String email = request.getSender();
                            int verificationCode = generateAndSendVerificationCode(email);

                            String responseContent = (verificationCode != -1) ? String.valueOf(verificationCode) : "EMAIL_FAILED";
                            Message codeResponse = new Message(2, "Server", responseContent);
                            sendJsonMessage(codeResponse);
                            break;
                        case 3: {
                            long userId;
                            List<PrivateChat> privateChats;

                            try {
                                userId = getUserIdByEmail(request.getSender());
                                privateChats = DataManager.getInstance().getPrivateChatsForUser(userId);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(3, "Server", "")); // ارسال رشته خالی در صورت خطا
                                break;
                            }

                            // لیست آبجکت‌ها را به یک رشته چندخطی تبدیل می‌کنیم
                            String content = privateChats.stream()
                                    .map(PrivateChat::toString) // متد toString هر آبجکت را صدا می‌زنیم
                                    .collect(Collectors.joining("\n")); // با خط جدید به هم می‌چسبانیم

                            Message response = new Message(3, "Server", content);
                            sendJsonMessage(response);
                            break;
                        }
                        case 4:
                            UserProfile userProfile = new UserProfile() ;
                            userProfile = UserProfile.fromString(request.getContent()) ;
                            long id = getUserIdByEmail(request.getSender());
                            updateUserProfile (id , userProfile);

                            break;
                        case 7:
                            String userPrompt = request.getContent();
                            String aiResponse = getAIResponse(userPrompt);

                            Message responseMessage = new Message(7, "AI", aiResponse);
                            sendJsonMessage(responseMessage);
                            break;
                        case 8: {
                            long userId;
                            List<Chat> groupAndChannelChats;

                            try {
                                userId = getUserIdByEmail(request.getSender());
                                groupAndChannelChats = DataManager.getInstance().getGroupAndChannelChatsForUser(userId);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(8, "Server", null));
                                break;
                            }

                            String json = gson.toJson(groupAndChannelChats);
                            Message response = new Message(8, "Server", json);
                            sendJsonMessage(response);
                            break;
                        }
                        case 10: {
                            long userId = getUserIdByEmail(request.getSender());
                            List<ContactRelation> contactRelations = DataManager.getInstance().getContacts(userId);
                            String json = gson.toJson(contactRelations);
                            sendJsonMessage(new Message(10, "Server", json));
                            break;
                        }
                        case 11: {
                            List<Users> allUsers;
                            try {
                                allUsers = DataManager.getInstance().getAllUsers();
                            } catch (SQLException e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(11, "Server", null));
                                break;
                            }

                            String json = gson.toJson(allUsers);
                            sendJsonMessage(new Message(11, "Server", json));
                            break;
                        }
                        case 12: {
                            String query = request.getContent();
                            long userId = getUserIdByEmail(request.getSender());

                            List<SearchResult> results;
                            try {
                                Search searchEngine = new Search(DataManager.getInstance());
                                results = searchEngine.search(query, userId);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(12, "Server", null));
                                break;
                            }
                            String content = results.stream()
                                    .map(SearchResult::toString)
                                    .collect(Collectors.joining("\n")); // ✅ درست
                            sendJsonMessage(new Message(12, "server", content));
                            break;
                        }
                        case 13 :{
                            Long userId = Long.parseLong(request.getContent());
                            Users user2;
                            try {
                                user2 = DataManager.getInstance().getUser(userId);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(13, "Server", null));
                                break;
                            }

                            String json = gson.toJson(user2);
                            sendJsonMessage(new Message(13, "Server", json));
                            break;

                        }
                        case 14: {
                            String[] ids = request.getContent().split(",");
                            long senderId = Long.parseLong(ids[0]);
                            long receiverId = Long.parseLong(ids[1]);

                            PrivateChat chat;
                            try {
                                chat = DataManager.getInstance().getOrCreatePrivateChat(senderId, receiverId);
                            } catch (Exception e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(14, "Server", null));
                                break;
                            }

                            String json = chat.toString();
                            sendJsonMessage(new Message(14, "Server", json));
                            break;
                        }
                        case 15: {
                            long chatId = Long.parseLong(request.getContent());

                            Channel channel;
                            try {
                                channel = DataManager.getInstance().getChannelById(chatId);
                            } catch (Exception e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(15, "Server", null));
                                break;
                            }

                            String json = gson.toJson(channel);
                            sendJsonMessage(new Message(15, "Server", json));
                            break;
                        }
                        case 16: {
                            JsonObject payload = JsonParser.parseString(request.getContent()).getAsJsonObject();
                            long chatId = payload.get("chatId").getAsLong();
                            long userId = payload.get("userId").getAsLong();

                            Channel channel;
                            ChannelSubscribers subscriber;

                            try {
                                channel = DataManager.getInstance().getChannelById(chatId);
                                subscriber = DataManager.getInstance().getOrSubscribeUser(channel, userId);
                            } catch (Exception e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(16, "Server", null));
                                break;
                            }

                            String json = gson.toJson(subscriber);
                            sendJsonMessage(new Message(16, "Server", json));
                            break;
                        }
                        case 17: {
                            JsonObject payload = JsonParser.parseString(request.getContent()).getAsJsonObject();
                            String name = payload.get("name").getAsString();
                            String description = payload.get("description").getAsString();
                            long creatorId = payload.get("creatorId").getAsLong();
                            boolean isPrivate = payload.get("isPrivate").getAsBoolean();

                            GroupChat groupChat;
                            try {
                                groupChat = DataManager.getInstance().getOrCreateGroupChat(name, description, creatorId, isPrivate);
                            } catch (Exception e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(17, "Server", null));
                                break;
                            }

                            String json = gson.toJson(groupChat);
                            sendJsonMessage(new Message(17, "Server", json));
                            break;
                        }

                        case 18: {
                            String content = request.getContent();
                            String[] parts = content.split(",");

                            long chatId = Long.parseLong(parts[0]);
                            long memberId = Long.parseLong(parts[1]);
                            long invitedById = Long.parseLong(parts[2]);

                            try {
                                UserHandler userHandler = new UserHandler();
                               // GroupChat groupChat = DataManager.getInstance().getOrCreateGroupChat(chatId);
                                Users member = Database.loadUser(memberId);
                                Users invitedBy = Database.loadUser(memberId);

                             //   GroupMembers groupMember = DataManager.getInstance().getOrJoinGroupMember(groupChat, member, invitedBy);

                                // ارسال به کلاینت با toString
                              //  sendJsonMessage(new Message(18, "Server", groupMember.toString()));

                            } catch (Exception e) {
                                e.printStackTrace();
                                sendJsonMessage(new Message(18, "Server", null));
                            }

                            break;
                        }

                        case 19: {
                            long chatId = Long.parseLong(request.getContent());

                            List<MessageEntity> messages;
                            try {
                                messages = DataManager.getInstance().getMessages(chatId);
                            } catch (Exception e) {
                                e.printStackTrace();
                                // در صورت خطا، یک رشته خالی ارسال می‌کنیم
                                sendJsonMessage(new Message(19, "Server", ""));
                                break;
                            }

                            // ۱. لیست پیام‌ها را با استریم به یک رشته چندخطی تبدیل می‌کنیم
                            String content = messages.stream()
                                    .map(MessageEntity::toString) // متد toString() هر پیام را فراخوانی می‌کند
                                    .collect(Collectors.joining("\n")); // نتایج را با کاراکتر خط جدید به هم می‌چسباند

                            // ۲. پاسخ را با محتوای جدید و نوع صحیح (19) ارسال می‌کنیم
                            sendJsonMessage(new Message(19, "Server", content));
                            break;
                        }
                        case 20 :
                            System.out.println("✅ Received message with Type 20: Handling new chat message.");
                            try {
                                // Get the message content (which is a JSON string from MessageDto)
                                String payload = request.getContent();

                                // Convert JSON string to MessageDto object
//                                MessageDto messageDto = MessageDto.fromString(payload) ;
                                com.yamyam.messenger.client.network.dto.MessageDto messageDto =
                                        com.yamyam.messenger.client.network.dto.MessageDto.fromString(payload);

                                // Extracting the necessary information from the DTO
                                long chatId = messageDto.getChatId();
                                long senderId = messageDto.getSenderId();
                                String text = messageDto.getText();

                                // Calling the addMessage method on the DataManager to save the message to the database
                                DataManager.getInstance().addMessage(chatId, senderId, text);
                            } catch (com.google.gson.JsonSyntaxException e) {
                                System.err.println("❌ Error parsing MessageDto JSON: " + e.getMessage());
                            } catch (Exception e) {
                                System.err.println("❌ An unexpected error occurred in case 20: " + e.getMessage());
                                e.printStackTrace();
                            }
                            break;
                        default:
                            System.err.println("Unknown request type: " + request.getType());
                            break;
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("Client " + socket.getInetAddress() + " disconnected.");
        } catch (IOException e) {
            System.err.println("Error with client " + socket.getInetAddress() + ": " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("finally error");
        }
    }

    private Users handleLogin(String email) throws IOException {
        try {

            // Create an instance of UserHandler and pass the connection to it
            UserHandler userHandler = new UserHandler();

            // Call the checkOrCreateUser method with the user's email
            Users user = userHandler.checkOrCreateUser(email);

            // result
            if (user != null) {
                System.out.println("SUCCESS: User checked or created successfully for email: " + user.getEmail());
            } else {
                System.err.println("FAILURE: Could not check or create user for email: " + email);
            }
            return user ;
        } catch (SQLException e) {
            System.err.println("Database error during checkOrCreateUser: " + e.getMessage());
            e.printStackTrace();
            return null ;
        }
    }

    private void sendJsonMessage(Message message) throws IOException {
        String json = gson.toJson(message);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        binaryOut.writeInt(jsonBytes.length); // send length
        binaryOut.write(jsonBytes);           // send data
        binaryOut.flush();
    }

    private int generateAndSendVerificationCode(String email) {
        // create random code
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);

        // sending email using EmailService
        boolean emailSent = EmailService.sendVerificationCode(email, code);

        if (emailSent) {
            // Return the code to the client only if the submission is successful
            return code;
        } else {
            // If an error occurs while sending the email, return an invalid value (e.g. -1)
            return -1;
        }
    }
    private String getAIResponse(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String apiKey = "sk-7G3aFals6gYxmTYHXIBV7EpYhc9JK5jyO8XTgsEaeLeSITWD";

        MediaType mediaType = MediaType.parse("application/json");
        String jsonBody = "{\n" +
                "  \"model\": \"deepseek-chat\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"user\", \"content\": \"" + prompt + "\"}\n" +
                "  ]\n" +
                "}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://api.gapgpt.app/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                int start = responseBody.indexOf("\"content\":\"") + 10;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            } else {
                return "error";
            }
        }
    }


}