package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.api.ContactService;
import com.yamyam.messenger.client.network.dto.Contact;

import java.util.List;

public class NetworkContactServiceAdapter implements ContactService {
    private final NetworkService net;
    public NetworkContactServiceAdapter(NetworkService net) { this.net = net; }

    @Override
    public List<Contact> getContacts(long meUserId) {
        // TODO: when back ready
        // temporary for now to go through UI
        return List.of(
                new Contact(2L, "Caroline Gray", null, true),
                new Contact(3L, "Presley Martin", null, false),
                new Contact(4L, "Matthew Brown", null, true)
        );
    }
}
