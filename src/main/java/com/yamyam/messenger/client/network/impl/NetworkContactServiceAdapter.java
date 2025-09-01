package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.api.ContactService;
import com.yamyam.messenger.server.database.Database;
import com.yamyam.messenger.shared.model.Contact;
import com.yamyam.messenger.shared.model.Users;

import java.util.List;

public class NetworkContactServiceAdapter implements ContactService {
    private final NetworkService net;
    public NetworkContactServiceAdapter(NetworkService net) { this.net = net; }

    @Override
    public List<Contact> getContacts(long meUserId) {
        try {
            Users me = Database.loadUser(meUserId);

            return net.fetchContacts(me.getEmail());

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
