package com.yamyam.messenger.server;

import com.yamyam.messenger.server.database.*;
import com.yamyam.messenger.server.services.EmailService;
import com.yamyam.messenger.shared.model.*;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static com.yamyam.messenger.server.database.Database.getUserIdByEmail;
import static com.yamyam.messenger.server.database.Database.loadUserChats;

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
                                loginUser = new Message(5, "Server", user.toString() );
                            }
                            sendJsonMessage(loginUser);
                            break;
                        case 2:
                            String email = request.getSender();
                            int verificationCode = generateAndSendVerificationCode(email);

                            String responseContent = (verificationCode != -1) ? String.valueOf(verificationCode) : "EMAIL_FAILED";
                            Message codeResponse = new Message(5, "Server", responseContent);
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
                                sendJsonMessage(new Message(3, "Server", null));
                                break;
                            }

                            String json = gson.toJson(privateChats);
                            Message response = new Message(3, "Server", json);
                            sendJsonMessage(response);
                            break;
                        }

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
                            List<Contact> contacts = DataManager.getInstance().getContacts(userId);
                            String json = gson.toJson(contacts);
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

                            String json = gson.toJson(results);
                            sendJsonMessage(new Message(12, "Server", json));
                            break;
                        }

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
            // Establish a connection to the database.
            Connection dbConnection = Database.getConnection();

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

            // Close the database connection
            dbConnection.close();
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