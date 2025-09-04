package com.yamyam.messenger.client.util;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.impl.SearchServiceAdapter;
import com.yamyam.messenger.client.network.service.ChatService;
import com.yamyam.messenger.client.network.service.ContactService;
import com.yamyam.messenger.client.network.service.SearchService;

import java.util.concurrent.atomic.AtomicReference;

public final class ServiceLocator {
    private static final AtomicReference<ContactService> contacts = new AtomicReference<>();
    private static final AtomicReference<ChatService> chat = new AtomicReference<>();
    private static final SearchService searchService = new SearchServiceAdapter(NetworkService.getInstance());


    private ServiceLocator() {}

    public static void set(ContactService c) {
        contacts.set(c);
    }

    public static void set(ChatService c) {
        chat.set(c);
    }

    public static ContactService contacts() {
        ContactService service = contacts.get();
        if (service == null) {
            throw new IllegalStateException("ContactService not set or initialized yet.");
        }
        return service;
    }

    public static ChatService chat() {
        ChatService service = chat.get();
        if (service == null) {
            throw new IllegalStateException("ChatService not set or initialized yet.");
        }
        return service;
    }

    public static void clear() {
        contacts.set(null);
        chat.set(null);
    }
    public static SearchService search() {
        return searchService;
    }

}
