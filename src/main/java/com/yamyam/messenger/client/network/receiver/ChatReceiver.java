package com.yamyam.messenger.client.network.receiver;

import com.yamyam.messenger.client.gui.controller.chat.ChatController;
import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.shared.model.message.MessageEntity;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import com.yamyam.messenger.client.network.NetworkService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;

/**
 * A runnable task that continuously listens for incoming objects (specifically MessageEntity)
 * from the server's ObjectInputStream. When a message is received, it updates the
 * UIs message list on the JavaFX Application Thread.
 */
public class ChatReceiver implements Runnable {

    private final NetworkService networkService;
    private final ObservableList<ChatController.Msg> messages;
    private volatile boolean running = true; // 'volatile' ensures visibility across threads

    public ChatReceiver(NetworkService networkService, ObservableList<ChatController.Msg> messages) {
        this.networkService = networkService;
        this.messages = messages;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = networkService.getObjectInputStream();
            System.out.println("✅ ChatReceiver thread started. Waiting for messages...");

            while (running) {
                // This line blocks until an object is sent from the server
                final Object receivedObject = in.readObject();

                if (receivedObject instanceof MessageEntity messageEntity) {
                    System.out.println("📬 New message received from server: " + messageEntity.getText());

                    // UI updates MUST happen on the JavaFX Application Thread.
                    // Platform.runLater schedules the code to run on that thread.
                    Platform.runLater(() -> {
                        // Determine if the message is from the current user or someone else
                        boolean isMe = messageEntity.getSender().getId() == AppSession.requireUserId();

                        // We set isMe to false because this listener only receives messages from others.
                        // Messages sent by "me" are added to the UI optimistically in ChatController.
                        // If the server were to broadcast our own messages back, we would need logic to avoid duplicates.
                        // For now, we assume the server doesn't send our own messages back to us.

                        ChatController.Msg newMsg = new ChatController.Msg(
                                isMe, // This will likely be false
                                messageEntity.getText(),
                                messageEntity.getSentAt().toLocalDateTime()
                        );
                        messages.add(newMsg);
                    });
                }
            }
        } catch (SocketException e) {
            System.out.println("🔌 Socket closed. ChatReceiver is stopping. " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                System.err.println("❌ Error in ChatReceiver: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            System.out.println("🛑 ChatReceiver thread finished.");
            running = false;
        }
    }

    /**
     * Signals the listening loop to terminate gracefully.
     */
    public void stop() {
        this.running = false;
    }
}