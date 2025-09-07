package com.yamyam.messenger.client.util;

import com.yamyam.messenger.client.gui.controller.chat.ChatController;
import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.shared.model.user.Users;
import javafx.collections.ObservableList;

import java.util.concurrent.atomic.AtomicReference; // این import را اضافه کن

public final class AppSession {

    // Atomic Reference is Thread Safety
    private static final AtomicReference<Users> currentUser = new AtomicReference<>();

    private static Thread activeChatListenerThread;
    private static ChatReceiver activeChatReceiver;

    private AppSession() {}

    public static void setCurrentUser(Users u) {
        // use the atomic set method
        currentUser.set(u);
    }

    public static Users getCurrentUser() {
        // use the atomic get method
        return currentUser.get();
    }

    public static boolean isLoggedIn() {
        return currentUser.get() != null;
    }

    public static void clear() {
        currentUser.set(null);
    }

    public static long requireUserId() {
        Users u = currentUser.get();
        if (u == null) {
            throw new IllegalStateException("No logged-in user in session");
        }
        return u.getId();
    }

    public static void listenActively(long chatId, ObservableList<ChatController.Msg> messages) {
        // Ensure any previous listener is stopped before starting a new one.
        stopListening();

        System.out.println("▶️ Starting to listen actively on chatId: " + chatId);
        try {
            // Get the singleton instance of our network service
            NetworkService networkService = NetworkService.getInstance();

            // Create the runnable task that will listen for messages
            activeChatReceiver = new ChatReceiver(networkService, messages);

            // Create and start the thread
            activeChatListenerThread = new Thread(activeChatReceiver, "ChatListenerThread-" + chatId);
            activeChatListenerThread.setDaemon(true); // Ensures the thread doesn't prevent the app from closing
            activeChatListenerThread.start();

        } catch (Exception e) {
            System.err.println("❌ Failed to start chat listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopListening(long chatId) {
        System.out.println("Received request to stop listening on chatId: " + chatId);
        stopListening(); // Call the main stop method
    }

    private static void stopListening() {
        if (activeChatListenerThread != null && activeChatListenerThread.isAlive()) {
            System.out.println("⏹️ Stopping active chat listener.");

            // Gracefully stop the runnable task and interrupt the thread
            if (activeChatReceiver != null) {
                activeChatReceiver.stop();
            }
            activeChatListenerThread.interrupt();

            // Clear references
            activeChatListenerThread = null;
            activeChatReceiver = null;
        }
    }
}
