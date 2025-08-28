package com.yamyam.messenger.client.util;

import com.yamyam.messenger.client.network.api.ChatService;
import com.yamyam.messenger.client.network.api.ContactService;

public final class ServiceLocator {
    private static ContactService contacts;
    private static ChatService chat;

    private ServiceLocator() {}

    public static void set(ContactService c) { contacts = c; }
    public static void set(ChatService c)    { chat = c; }

    public static ContactService contacts() {
        if (contacts == null) throw new IllegalStateException("ContactService not set");
        return contacts;
    }
    public static ChatService chat() {
        if (chat == null) throw new IllegalStateException("ChatService not set");
        return chat;
    }

    public static void clear() { contacts = null; chat = null; }
}
