package com.yamyam.messenger.shared;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserProfile {
    private long profileId;
    private long userId;
    private String profileImageUrl;
    private String bio;
    private LocalDateTime createdAt;
    private boolean isActive;
    private Timestamp updatedAt;
    private String username;
    private String passwordHashed;

    public UserProfile() {}

    public UserProfile(long profileId,
                       String profileImageUrl,
                       String bio,
                       boolean isActive,
                       String username,
                       String passwordHashed,
                       Timestamp updatedAt) {
        this.profileId = profileId;
        this.userId = userId;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.isActive = isActive;
        this.username = username;
        this.passwordHashed = passwordHashed;
        this.updatedAt = updatedAt;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getBio() {
        return bio;
    }
    public void setBio(String bio) {
        this.bio = bio;
    }
    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean active) {
        isActive = active;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPasswordHashed() {
        return passwordHashed;
    }
    public void setPasswordHashed(String passwordHashed) {
        this.passwordHashed = passwordHashed;
    }
}
