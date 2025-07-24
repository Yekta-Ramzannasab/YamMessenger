package com.yamyam.messenger.shared;

import java.sql.Timestamp;

public class Users {
    private int id;
    private String username;
    private String profileName;
    private Timestamp createAt;
    private Timestamp lastSeen;
    private boolean isVerified;
    private boolean isOnline;
    private boolean isDeleted;
    private String email;
    private String password;
    private Timestamp updated_at;


    public Users(int id,
                 String username,
                 String profileName,
                 Timestamp createAt,
                 Timestamp lastSeen,
                 boolean isVerified,
                 boolean isOnline,
                 boolean isDeleted,
                 String email,
                 String password,
                 Timestamp updated_at
                 ){
        this.id = id;
        this.username = username;
        this.profileName = profileName;
        this.createAt = createAt;
        this.lastSeen = lastSeen;
        this.isVerified = isVerified;
        this.isOnline  = isOnline;
        this.isDeleted = isDeleted;
        this.email = email;
        this.password = password;
        this.updated_at = updated_at;

    }


    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getProfileName() {
        return profileName;
    }
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
    public Timestamp getCreateAt() {
        return createAt;
    }
    public void setCreateAt(Timestamp createAt) {
        this.createAt = createAt;
    }
    public Timestamp getLastSeen() {
        return lastSeen;
    }
    public void setLastSeen(Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }
    public boolean isVerified() {
        return isVerified;
    }
    public void setVerified(boolean verified) {
        isVerified = verified;
    }
    public boolean isOnline() {
        return isOnline;
    }
    public void setOnline(boolean online) {
        isOnline = online;
    }
    public boolean isDeleted() {
        return isDeleted;
    }
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Timestamp getUpdated_at() {
        return updated_at;
    }
    public void setUpdated_at(Timestamp updated_at) {
        this.updated_at = updated_at;
    }
}
