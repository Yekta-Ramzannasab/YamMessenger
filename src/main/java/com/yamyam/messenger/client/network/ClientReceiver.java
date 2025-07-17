package com.yamyam.messenger.client.network;

import com.yamyam.messenger.shared.Message;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Scanner;

public class ClientReceiver implements Runnable {
    private final Gson gson;

    private final DataInputStream binaryIn;
    private static DataOutputStream binaryOut;

    private static String username;
    Scanner scanner;

    public ClientReceiver(Socket socket ,String username,Scanner scanner) throws IOException {
        ClientReceiver.username = username ;
        this.scanner = scanner;

        this.gson = new Gson();

        this.binaryIn = new DataInputStream(socket.getInputStream());
        binaryOut = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            // The loop continues as long as the thread has NOT been interrupted.
            while ( !Thread.currentThread().isInterrupted() ) {

                int length = binaryIn.readInt(); // read length
                byte[] jsonBytes = new byte[length];
                binaryIn.readFully(jsonBytes, 0, length); // read message
                String serverMessageJson = new String(jsonBytes, StandardCharsets.UTF_8);

                // processing json string
                try {
                    Message request = gson.fromJson(serverMessageJson, Message.class);
                    if (request == null) {
                        System.err.println("Invalid request format received.");
                        continue;
                    }

                    switch (request.getType()) {
                        case 1: // chat message
                            if (!request.getSender().equals(username)) {
                                System.out.println("\n" + request.getSender() + ": " + request.getContent());
                            }
                            break;
                        case 3:
                            if (request.getContent().charAt(0) == 'l') {
                                // send request for download
                                String choice = parseAndChoose(request.getContent(), scanner);
                                if (choice != null) {
                                    Message fileIndex = new Message(3, username, choice);
                                    sendJsonMessage(fileIndex);
                                }
                            } else {
                                receiveFile(request.getContent());
                            }
                            break;
                        default:
                            break;
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("Invalid JSON received from server: " + e.getMessage());
                }
            }
        } catch (java.io.EOFException e) {
            System.out.println("\n[Server has closed the connection.]");
        } catch (SocketException e) {
            System.out.println("\n[Connection to the server has been lost.]");
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.out.println("\n[An error occurred: " + e.getMessage() + "]");
            }
        } finally {
            System.out.println("\n[Chat receiver thread is shutting down.]");
        }
    }

    private static String parseAndChoose (String list, Scanner scanner){
        // making list
        list = list.substring(2);
        String[] fileNames = list.split(",");

        if (fileNames.length == 0) {
            System.out.println("No files to download.");
            return null;
        }

        // Show available files
        System.out.println("Select a file to download:");
        for (int i = 0; i < fileNames.length; i++) {
            System.out.println((i + 1) + ". " + fileNames[i]);
        }

        System.out.print("Enter file number: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return null;
        }

        if (choice < 0 || choice >= fileNames.length) {
            System.out.println("Invalid choice.");
            return null;
        }

        return ( "" + choice ) ;
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

    private void receiveFile(String payload)
    {
        try {
            // split file name from base64 string
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                System.err.println("Invalid Base64 file format.");
                return;
            }

            String fileName = parts[0];
            String encodedFileString = parts[1];

            System.out.println("Receiving Base64 file: " + fileName);

            // decoding base64 string into byte array
            byte[] fileBytes = Base64.getDecoder().decode(encodedFileString);

            // saving file
            File serverUploadsDir = new File("src/main/resources/Client/" + username );
            if (!serverUploadsDir.exists()) {
                serverUploadsDir.mkdirs();
            }
            File fileToSave = new File(serverUploadsDir, fileName);
            Files.write(fileToSave.toPath(), fileBytes);

            System.out.println("✅ File '" + fileName + "' decoded and saved.");

        } catch (IllegalArgumentException | IOException e) {
            System.err.println("❌ Invalid Base64 string received: " + e.getMessage());
        }
    }
}