package com.yamyam.messenger.client.network.receiver; // Your specified package

import com.google.gson.Gson;
import com.yamyam.messenger.client.gui.controller.chat.ChatController;
import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.shared.model.Message;
import com.yamyam.messenger.shared.model.message.MessageEntity;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A runnable task that continuously listens for incoming 'Message' objects
 * from the server using the existing JSON-based protocol.
 */
public class ChatReceiver implements Runnable {

    private final NetworkService networkService;
    private final ObservableList<ChatController.Msg> messages;
    private volatile boolean running = true;

    // A new constant for the message type for pushed chat messages from the server
    private static final int PUSH_CHAT_MESSAGE_TYPE = 21;

    public ChatReceiver(NetworkService networkService, ObservableList<ChatController.Msg> messages) {
        this.networkService = networkService;
        this.messages = messages;
    }

    @Override
    public void run() {
        System.out.println("✅ ChatReceiver thread started. Waiting for messages using JSON protocol...");

        while (running) {
            try {
                // Use the existing method to receive a 'Message' object
                Message serverMessage = networkService.receiveJsonMessage();

                // Check if the message is a new chat message pushed from the server
                if (serverMessage != null && serverMessage.getType() == PUSH_CHAT_MESSAGE_TYPE) {
                    System.out.println("📬 New chat message pushed from server.");

                    // The content of the message is the string representation of MessageEntity
                    String messageEntityString = serverMessage.getContent();
                    MessageEntity messageEntity = MessageEntity.fromString(messageEntityString);

                    if (messageEntity != null) {
                        // UI updates MUST happen on the JavaFX Application Thread
                        Platform.runLater(() -> {
                            // Since this is a pushed message, it's always from someone else.
                            ChatController.Msg newMsg = new ChatController.Msg(
                                    false, // isMe is always false for received messages
                                    messageEntity.getText(),
                                    messageEntity.getSentAt().toLocalDateTime()
                            );
                            messages.add(newMsg);
                        });
                    }
                }
                // We ignore other message types here because they are responses to requests
                // made by the main thread, and receiveJsonMessage has already returned them there.
                // This architecture has a potential risk of race conditions, but we proceed as requested.

            } catch (IOException e) {
                if (running) {
                    System.err.println("❌ Connection error in ChatReceiver: " + e.getMessage());
                }
                // Stop the loop on connection error
                stop();
            }
        }
        System.out.println("🛑 ChatReceiver thread finished.");
    }

    public void stop() {
        this.running = false;
    }
}