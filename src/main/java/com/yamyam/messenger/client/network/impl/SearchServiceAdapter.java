package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.service.SearchService;
import com.yamyam.messenger.server.database.Database;
import com.yamyam.messenger.server.database.SearchResult;
import com.yamyam.messenger.shared.model.chat.Chat;
import com.yamyam.messenger.shared.model.user.Users;

import java.util.List;
import java.util.stream.Collectors;

public class SearchServiceAdapter implements SearchService {
    private final NetworkService net;

    public SearchServiceAdapter(NetworkService net) {
        this.net = net;
    }

    @Override
    public List<SearchResult> search(String query, long meUserId) {
        try {
            List<Chat> searchResults = Database.searchChats(query, meUserId);
            return searchResults.stream()
                    .map(chat -> new SearchResult(chat, chat.getSearchRank()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ خطا در سرچ: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}