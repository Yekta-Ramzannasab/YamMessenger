package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.*;

import java.sql.SQLException;
import java.util.*;

public class Search {

    private final DataManager dataManager;

    public Search(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<SearchResult> search(String query, long userId) throws SQLException {
        List<SearchResult> results = new ArrayList<>();

        // users
        dataManager.searchUsers(query).forEach(u -> {
            results.add(new SearchResult(u, u.getSearchRank()));

        });

        // chats
        dataManager.searchChats(query, userId).forEach(c -> {
            results.add(new SearchResult(c, c.getSearchRank()));
        });

        // messages
        dataManager.searchMessages(query, userId).forEach(m -> {
            results.add(new SearchResult(m, m.getSearchRank()));
        });

        // sort by rank
        results.sort(Comparator.comparing(SearchResult::getRank).reversed());

        return results;
    }
}
