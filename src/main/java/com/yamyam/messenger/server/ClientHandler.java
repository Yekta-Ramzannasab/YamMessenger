package com.yamyam.messenger.server;

import com.yamyam.messenger.shared.Message;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson;

    private final DataInputStream binaryIn;
    private final DataOutputStream binaryOut;

    private final List<ClientHandler> allClients;

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
                        case 0:
                            handleLogin(request.getSender(), request.getContent());
                            break;
                        case 1:
                            broadcast(request.getSender(), request.getContent());
                            System.out.println(request.getSender() + " : " + request.getContent());
                            break;
                        case 2:
                            receiveFile(request.getContent()) ;
                            break;
                        case 3:
                            if (request.getContent().equals("l"))
                                sendFileList();
                            else
                                sendFile(request.getContent());
                            break;
                        case 5:

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
        } finally {
            System.out.println("finally error");
        }
    }

    private void broadcast(String username, String msg) throws IOException {
        Message message = new Message(1, username, msg);

        // we use synchronized block for security in multithreaded apps
        synchronized (allClients) {
            for (ClientHandler aClient : allClients) {
                // broadcast message to all clients
                aClient.sendJsonMessage(message);
            }
        }
    }

    private void sendFileList() throws IOException {
        // List all files in the server directory
        File userDirectory = new File("src/main/resources/Server/Files" );
        File[] files = userDirectory.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files to upload.");
            return;
        }

        // Send a message containing file names as a comma-separated string
        StringBuilder list ;
        list = new StringBuilder("l,"+files[0].getName());
        for (int i = 1; i < files.length; i++) {
            list.append(",").append(files[i].getName());
        }
        String finalList = list.toString();
        Message sendList = new Message(3,"server",finalList) ;
        sendJsonMessage(sendList);
    }

    private void sendFile(String fileIndex){
        // finding selected file
        File userDirectory = new File("src/main/resources/Server/Files" );
        File[] files = userDirectory.listFiles();
        if ( files == null ){
            System.out.println("no files in directory");
            return;
        }
        File selectedFile = files[Integer.parseInt(fileIndex)];

        try { // encode and send file
            System.out.println("Encoding " + selectedFile.getName() + "...");
            byte[] fileBytes = Files.readAllBytes(selectedFile.toPath()); // convert file to byte array

            String encodedFileString = Base64.getEncoder().encodeToString(fileBytes);
            String payload = selectedFile.getName() + ":" + encodedFileString;// convert byte array into string


            Message fileMessage = new Message(3, "server", payload);
            sendJsonMessage(fileMessage);// sending payload string

            System.out.println("✅ File sent successfully as a JSON message.");
        } catch (IOException e) {
            System.err.println("❌ Error during file encoding/sending: " + e.getMessage());
        }
        System.out.println("✅ File upload complete.");
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
            File serverUploadsDir = new File("src/main/resources/Server/Files");
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

    private void handleLogin(String email, String otherArguments) throws IOException {
        String[] parts = otherArguments.split(",", 3);
        boolean singUp = Boolean.parseBoolean(parts[0]);
        String name = parts[1];
        String pass = parts[2];

        if ( singUp ){

        } else {

        }



//        boolean isAuthenticated = Server.authenticate(username, password);
//        Message response = isAuthenticated
//                ? new Message(0, "Server", "true")
//                : new Message(0, "Server", "false");
//        sendJsonMessage(response);
    }

    private void sendJsonMessage(Message message) throws IOException {
        String json = gson.toJson(message);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        binaryOut.writeInt(jsonBytes.length); // send length
        binaryOut.write(jsonBytes);           // send data
        binaryOut.flush();
    }


}