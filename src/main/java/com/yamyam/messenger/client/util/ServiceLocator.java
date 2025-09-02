package com.yamyam.messenger.client.util;

import com.yamyam.messenger.client.network.api.ChatService;
import com.yamyam.messenger.client.network.api.ContactService;
import com.yamyam.messenger.client.network.api.UserService;

public final class ServiceLocator {
    private static ContactService contacts;
    private static ChatService chat;
    private static UserService users;

    private ServiceLocator() {}

    public static void set(ContactService c) { contacts = c; }
    public static void set(ChatService c)    { chat = c; }
    public static void set(UserService u)    { users = u; }

    public static ContactService contacts() {
        if (contacts == null) throw new IllegalStateException("ContactService not set");
        return contacts;
    }
    public static ChatService chat() {
        if (chat == null) throw new IllegalStateException("ChatService not set");
        return chat;
    }
    public static UserService users() {
        if (users == null) throw new IllegalStateException("UserService not set");
        return users;
    }


    public static void clear() { contacts = null; chat = null; }
}
