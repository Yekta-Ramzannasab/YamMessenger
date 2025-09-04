package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.service.SearchService;
import com.yamyam.messenger.server.database.Database;
import com.yamyam.messenger.server.database.SearchResult;
import com.yamyam.messenger.shared.model.Users;

import java.util.List;

public class SearchServiceAdapter implements SearchService {
    private final NetworkService net;

    public SearchServiceAdapter(NetworkService net) {
        this.net = net;
    }

    @Override
    public List<Users> search(String query, long meUserId) {
        try {
            Users me = Database.loadUser(meUserId);
            assert me != null;
            return net.fetchSearchResults(query, me.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}