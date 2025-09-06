package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.chat.Chat;
import com.yamyam.messenger.shared.model.chat.ChatType;
import com.yamyam.messenger.shared.model.chat.GroupChat;
import com.yamyam.messenger.shared.model.chat.Channel;
import com.yamyam.messenger.shared.model.user.UserProfile;
import com.yamyam.messenger.shared.model.user.Users;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SearchResult {
    private Object entity; // user , chat
    private double rank;

    public SearchResult(Users user, double rank) {
        this.entity = user;
        this.rank = rank;
    }

    public SearchResult(Chat chat, double rank) {
        this.entity = chat;
        this.rank = rank;
    }

    public Object getEntity() {
        return entity;
    }

    public double getRank() {
        return rank;
    }

    @Override
    public String toString() {
        if (entity instanceof Users u) {
            UserProfile p = u.getUserProfile();
            return "USER|" +
                    rank + "|" +
                    u.getId() + "," +
                    safe(u.getCreateAt()) + "," +
                    safe(u.getLastSeen()) + "," +
                    u.isVerified() + "," +
                    u.isOnline() + "," +
                    u.isDeleted() + "," +
                    safe(u.getEmail()) + "," +
                    safe(p != null ? p.getUsername() : null) + "," +
                    safe(p != null ? p.getProfileName() : null) + "," +
                    safe(p != null ? p.getBio() : null);
        }

        if (entity instanceof Chat c) {
            String name = extractChatName(c);
            return "CHAT|" +
                    rank + "|" +
                    c.getChatId() + "," +
                    safe(c.getType()) + "," +
                    safe(name) + "," +
                    safe(c.getCreatedAt());
        }

        return "UNKNOWN|" + rank + "|null";
    }

    private String extractChatName(Chat c) {
        return switch (c.getType()) {
            case CHANNEL -> (c instanceof Channel ch) ? ch.getChannelName() : "کانال بی‌نام";
            case GROUP_CHAT -> (c instanceof GroupChat g) ? g.getGroupName() : "گروه بی‌نام";
            case PRIVATE_CHAT -> "چت خصوصی"; // یا اسم طرف مقابل از context
        };
    }

    private String safe(Object val) {
        if (val == null) return "null";
        String s = val.toString();
        if (s.contains(",") || s.contains("|")) {
            return "\"" + s.replace("\"", "'") + "\"";
        }
        return s;
    }

    public static SearchResult fromString(String str) {
        if (str == null || str.isEmpty()) return null;

        String[] parts = str.split("\\|", 3);
        if (parts.length < 3) return null;

        String type = parts[0];
        double rank = Double.parseDouble(parts[1]);
        String data = parts[2];

        try {
            if (type.equals("USER")) {
                Users u = new Users();
                u.setId(Long.parseLong(data)); // فرض می‌کنیم data فقط id است
                u.setSearchRank(rank);
                return new SearchResult(u, rank);
            }

            if (type.equals("CHAT")) {
                Chat c = new Chat();
                c.setChatId(Long.parseLong(data)); // فرض می‌کنیم data فقط chatId است
                c.setSearchRank(rank);
                return new SearchResult(c, rank);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to parse SearchResult: " + e.getMessage());
        }

        return null;
    }
}