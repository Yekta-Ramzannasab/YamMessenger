package com.yamyam.messenger.client.network;

import com.yamyam.messenger.shared.Message;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public class NetworkService {
    // Singleton section
    private static NetworkService instance;
    private static Socket socket;

    private NetworkService() {
        try {
            // اتصال را برقرار کرده و در متغیر استاتیک ذخیره می‌کنیم
            socket = new Socket("localhost", 5001);
            binaryIn = new DataInputStream(socket.getInputStream());
            binaryOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }

    private static DataInputStream binaryIn;
    private static DataOutputStream binaryOut;

    private static String username;
    private static final int PORT = 5001;

    private static Thread receiverThread = null;

    public static void main(String[] args) throws Exception {

        try (Socket socket = new Socket("localhost", PORT)) {
            binaryIn = new DataInputStream(socket.getInputStream());
            binaryOut = new DataOutputStream(socket.getOutputStream());

            Scanner scanner = new Scanner(System.in);

            // --- LOGIN PHASE ---
            System.out.println("===== Welcome to CS Music Room =====");


            boolean loggedIn = false;


            // receive and check the server's login response
            while (!loggedIn) {
                // login with asking name and pass from the user
                System.out.print("Username: ");
                username = scanner.nextLine();
                System.out.print("Password: ");
                String password = scanner.nextLine();
                password = hashPassword(password);

                sendLoginRequest(password);

                int length = binaryIn.readInt(); // reading length of response
                if (length > 0) {
                    byte[] jsonBytes = new byte[length];
                    binaryIn.readFully(jsonBytes, 0, length); // reading response
                    String jsonResponse = new String(jsonBytes, StandardCharsets.UTF_8);

                    // response processing
                    Gson gson = new Gson();
                    Message response = gson.fromJson(jsonResponse, Message.class);

                    if (response.getContent().equals("true")) {
                        loggedIn = true;
                        System.out.println("✅ " + "Login was successful");
                    } else {
                        System.out.println("❌ " + "Login failed");
                    }
                } else {
                    System.out.println("Received an empty response from server.");
                }
            }


            // --- ACTION MENU LOOP ---
            while (true) {
                printMenu();
                System.out.print("Enter choice: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1" -> enterChat(scanner, socket);
                    case "2" -> uploadFile(scanner);
                    case "3" -> requestDownload(scanner);
                    case "0" -> {
                        System.out.println("Exiting...");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Enter chat box");
        System.out.println("2. Upload a file");
        System.out.println("3. Download a file");
        System.out.println("0. Exit");
    }

    public static String hashPassword(String password) throws NoSuchAlgorithmException {
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

    private static void sendJsonMessage(Message message) throws IOException {
        // convert message into a json string then convert it to a byte array
        String json = new Gson().toJson(message);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // first send length of message
        binaryOut.writeInt(jsonBytes.length);

        // then send array
        binaryOut.write(jsonBytes);
        binaryOut.flush();
    }

    private static void sendLoginRequest(String password) throws IOException {
        Message loginRequest = new Message(0, username, password);
        sendJsonMessage(loginRequest);
    }

    private static void enterChat(Scanner scanner, Socket socket) throws IOException {
        System.out.print("You have entered the chat ");

        // Create and start ClientReceiver thread to continuously get new messages from server
        ClientReceiver clientReceiver = new ClientReceiver(socket, username, scanner);
        receiverThread = new Thread(clientReceiver);
        receiverThread.start();

        String message_string = "";
        while (!message_string.equalsIgnoreCase("/exit")) {
            message_string = scanner.nextLine();

            if (!message_string.equalsIgnoreCase("/exit")) {
                sendChatMessage(message_string);
            }
        }

        receiverThread.interrupt();
    }

    private static void sendChatMessage(String message_to_send) throws IOException {
        Message chatMessage = new Message(1, username, message_to_send);
        sendJsonMessage(chatMessage);
    }

    private static void uploadFile(Scanner scanner) throws IOException {

        // all files listed in files array
        File userDirectory = new File("src/main/resources/Client/" + username);
        File[] files = userDirectory.listFiles();
        File selectedFile;
        if (files == null || files.length == 0) {
            System.out.println("No files to upload.");
            return;
        }

        // Show available files
        System.out.println("Select a file to upload:");
        for (int i = 0; i < files.length; i++) {
            System.out.println((i + 1) + ". " + files[i].getName());
        }

        System.out.print("Enter file number: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        if (choice < 0 || choice >= files.length) {
            System.out.println("Invalid choice.");
            return;
        }
        selectedFile = files[choice];

        try { // encode and send file
            System.out.println("Encoding " + selectedFile.getName() + "...");
            byte[] fileBytes = Files.readAllBytes(selectedFile.toPath()); // convert file to byte array

            String encodedFileString = Base64.getEncoder().encodeToString(fileBytes);
            String payload = selectedFile.getName() + ":" + encodedFileString; // convert byte array into string

            Message fileMessage = new Message(2, username, payload);
            sendJsonMessage(fileMessage); // sending payload string

            System.out.println("✅ File sent successfully as a JSON message.");
        } catch (IOException e) {
            System.err.println("❌ Error during file encoding/sending: " + e.getMessage());
        }

        System.out.println("✅ File upload complete.");
    }

    private static void requestDownload(Scanner scanner) throws IOException {
        // if receiver thread running for chat box interrupt it
        if (receiverThread != null && receiverThread.isAlive()) {
            receiverThread.interrupt();
            try {
                receiverThread.join(100); // a little while waiting for ending thread
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // send request for file list
        System.out.println("Requesting file list from server...");
        sendJsonMessage(new Message(3, username, "l"));

        // now main thread is waiting for response
        System.out.println("Waiting for file list...");
        int length = binaryIn.readInt();
        byte[] jsonBytes = new byte[length];
        binaryIn.readFully(jsonBytes, 0, length);
        String jsonResponse = new String(jsonBytes, StandardCharsets.UTF_8);

        Message serverResponse = new Gson().fromJson(jsonResponse, Message.class);
        String content = serverResponse.getContent();

        // process list and get the users choice
        if (serverResponse.getType() == 3 && content.startsWith("l,")) {
            String fileListString = content.substring(2);
            if (fileListString.isEmpty()) {
                System.out.println("No files available on the server.");
                return;
            }
            String[] fileNames = fileListString.split(",");

            System.out.println("\n--- 📂 Available Files for Download ---");
            for (int i = 0; i < fileNames.length; i++) {
                System.out.println((i + 1) + ". " + fileNames[i]);
            }
            System.out.println("--------------------------------------");
            System.out.print("\nEnter the number of the file you want to download: ");
            int choice = Integer.parseInt(scanner.nextLine()) - 1;

            if (choice >= 0 && choice < fileNames.length) {
                // send request for download specified file
                String indexToDownload = String.valueOf(choice);
                System.out.println("Requesting to download file at index: " + indexToDownload);
                sendJsonMessage(new Message(3, username, indexToDownload));

                // waiting for file
                // server send a message contains base64 file
                int fileLength = binaryIn.readInt();
                byte[] fileJsonBytes = new byte[fileLength];
                binaryIn.readFully(fileJsonBytes, 0, fileLength);
                String fileJsonResponse = new String(fileJsonBytes, StandardCharsets.UTF_8);
                Message fileMessage = new Gson().fromJson(fileJsonResponse, Message.class);

                // saving file
                receiveFileOnMainThread(fileMessage.getContent());

            } else {
                System.out.println("Invalid selection.");
            }
        } else {
            System.err.println("Error: Received an unexpected response from the server.");
        }
        System.out.println("\nReturning to main menu. Select 'Enter chat box' to see new messages.");
    }

    private static void receiveFileOnMainThread(String payload) {
        try {
            String[] parts = payload.split(":", 2);
            String fileName = parts[0];
            String encodedFileString = parts[1];

            System.out.println("Downloading and saving: " + fileName);
            byte[] fileBytes = Base64.getDecoder().decode(encodedFileString);

            File downloadsDir = new File("src/main/resources/Client/" + username);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File fileToSave = new File(downloadsDir, fileName);
            Files.write(fileToSave.toPath(), fileBytes);
            System.out.println("✅ File '" + fileName + "' saved successfully in your Downloads folder.");
        } catch (Exception e) {
            System.err.println("❌ Error saving file: " + e.getMessage());
        }
    }

    public static String clientHandleLogin ( Socket socket , String name , String pass , String email , boolean signUp ) throws Exception {
        DataInputStream binaryIn = new DataInputStream(socket.getInputStream());
        pass = hashPassword(pass);

        // create a message and send to server fo login request
        Message loginMessage = new Message(0 , email , signUp + "," + name + "," + pass ) ;
        sendJsonMessage(loginMessage);

        int length = binaryIn.readInt(); // reading length of response
        if (length > 0) {
            byte[] jsonBytes = new byte[length];
            binaryIn.readFully(jsonBytes, 0, length); // reading response
            String jsonResponse = new String(jsonBytes, StandardCharsets.UTF_8);

            // response processing
            Gson gson = new Gson();
            Message response = gson.fromJson(jsonResponse, Message.class);

            // parse content
            String[] parts = response.getContent().split(":", 2);
            String status = parts[0];
            String responseContent = parts[1];

            if ( status.equals("true") )
                return "S" + responseContent ;
            else
                return "F" + responseContent ;
        } else
            return "F" + "received an empty response from server";
    }

    public Integer requestVerificationCode(String email) {
        try {
            // Message type 5 to request a verification code
            Message request = new Message(5, email, "SEND_CODE");
            sendJsonMessage(request);

            // Waiting for a response from the server containing the code
            int length = binaryIn.readInt();
            if (length > 0) {
                byte[] jsonBytes = new byte[length];
                binaryIn.readFully(jsonBytes, 0, length);
                String jsonResponse = new String(jsonBytes, StandardCharsets.UTF_8);
                Message response = new Gson().fromJson(jsonResponse, Message.class);

                // Convert the message content, which is the confirmation code, to an Integer and return it
                try {
                    return Integer.parseInt(response.getContent());
                } catch (NumberFormatException e) {
                    // If the server does not return a number (e.g. an error message)
                    System.err.println("Server did not return a valid code: " + response.getContent());
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If any communication problem occurs, return null
        return null;
    }
}