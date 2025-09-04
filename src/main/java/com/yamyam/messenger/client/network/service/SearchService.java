package com.yamyam.messenger.client.network.service;

import com.yamyam.messenger.server.database.SearchResult;
import com.yamyam.messenger.shared.model.Users;

import java.util.List;

public interface SearchService {
    List<Users> search(String query, long meUserId);
}