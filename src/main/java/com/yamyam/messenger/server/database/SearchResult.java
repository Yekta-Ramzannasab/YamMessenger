package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.Chat;
import com.yamyam.messenger.shared.model.MessageEntity;
import com.yamyam.messenger.shared.model.Users;

public class SearchResult {
    private Object entity; // user , chat, message
    private double rank;   // for sorting

    public SearchResult(Users user, double rank) {
        this.entity = user;
        this.rank = rank;
    }

    public SearchResult(Chat chat, double rank) {
        this.entity = chat;
        this.rank = rank;
    }

    public SearchResult(MessageEntity message, double rank) {
        this.entity = message;
        this.rank = rank;
    }

    public Object getEntity() {
        return entity;
    }

    public double getRank() {
        return rank;
    }
}
