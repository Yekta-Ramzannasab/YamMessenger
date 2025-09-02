package com.yamyam.messenger.client.network.api;

import com.yamyam.messenger.server.database.SearchResult;
import java.util.List;

public interface SearchService {
    List<SearchResult> search(String query, long meUserId);
}