package com.yamyam.messenger.shared;

import java.sql.Timestamp;

public class Users {
    private long id;

    private Timestamp createAt;
    private Timestamp lastSeen;
    private boolean isVerified;
    private boolean isOnline;
    private boolean isDeleted;
    private String email;
    private UserProfile userProfile;


    public Users(long id ,
                 Timestamp createAt ,
                 Timestamp lastSeen ,
                 boolean isVerified ,
                 boolean isOnline ,
                 boolean isDeleted ,
                 String email,
                 UserProfile userProfile) {
        this.id = id;
        this.createAt = createAt;
        this.lastSeen = lastSeen;
        this.isVerified = isVerified;
        this.isOnline = isOnline;
        this.isDeleted = isDeleted;
        this.email = email;
        this.userProfile = userProfile;
    }


    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}

