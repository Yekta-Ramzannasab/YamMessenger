package com.yamyam.messenger.shared;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserProfile {
    private long profileId;
    private long userId;
    private String profileImageUrl;
    private String bio;
    private LocalDateTime createdAt;
    private Timestamp updatedAt;
    private String username;
    private String passwordHashed;
    private String profileName;

    public UserProfile() {}

    public UserProfile(long profileId,
                       String profileImageUrl,
                       String bio,
                       String username,
                       String passwordHashed,
                       Timestamp updatedAt,
                       String profileName) {
        this.profileId = profileId;
        this.userId = userId;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.username = username;
        this.passwordHashed = passwordHashed;
        this.updatedAt = updatedAt;
        this.profileName = profileName;
    }
    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
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
