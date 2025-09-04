package com.yamyam.messenger.server.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yamyam.messenger.shared.model.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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

        return "UNKNOWN|" + rank + "|null";
    }

    private String safe(Object val) {
        if (val == null) return "null";
        String s = val.toString();
        if (s.contains(",") || s.contains("|")) {
            return "\"" + s.replace("\"", "'") + "\""; // جلوگیری از شکستن رشته
        }
        return s;
    }

    public static Users fromString(String str) {
        if (str == null || str.isEmpty()) return null;

        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        if (fields.size() < 11) return null;

        try {
            Users u = new Users();
            u.setId(Long.parseLong(fields.get(0)));
            u.setCreateAt(Timestamp.valueOf(parseDate(fields.get(1))));
            u.setLastSeen(Timestamp.valueOf(parseDate(fields.get(2))));
            u.setVerified(Boolean.parseBoolean(fields.get(3)));
            u.setOnline(Boolean.parseBoolean(fields.get(4)));
            u.setDeleted(Boolean.parseBoolean(fields.get(5)));
            u.setEmail(unquote(fields.get(6)));
            u.setSearchRank(Double.parseDouble(fields.get(7)));

            UserProfile p = new UserProfile();
            p.setUsername(unquote(fields.get(8)));
            p.setProfileName(unquote(fields.get(9)));
            p.setBio(unquote(fields.get(10)));
            u.setUserProfile(p);

            return u;
        } catch (Exception e) {
            System.err.println("❌ Failed to parse Users: " + e.getMessage());
            return null;
        }
    }

    private static LocalDateTime parseDate(String s) {
        if (s == null || s.equals("null")) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String unquote(String s) {
        if (s == null || s.equals("null")) return null;
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("'", "\"");
        }
        return s;
    }
}
