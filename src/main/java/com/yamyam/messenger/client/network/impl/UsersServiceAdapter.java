package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.api.UserService;
import com.yamyam.messenger.shared.model.Users;

import java.io.IOException;
import java.util.List;

public class AllUsersServiceAdapter implements UserService {
    private final NetworkService net;

    public AllUsersServiceAdapter(NetworkService net) {
        this.net = net;
    }

    @Override
    public List<Users> getAllUsers() {
        try {
            return net.fetchAllUsers();
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }
}