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

    @Override
    public String toString (){
        return ( profileId + "," + userId + "," + profileImageUrl + "," + bio + "," +
                createdAt + "," + updatedAt + "," + username + "," + passwordHashed + "," + profileName );
    }

    public static UserProfile fromString(String profileDataString) {
        if (profileDataString == null || profileDataString.isEmpty()) {
            return null;
        }

        // Split string into parts based on commas
        String[] parts = profileDataString.split(",", 7);
        if (parts.length < 7) return null;// if number of parts less than 8 return null

        try {
            // Convert each part to the corresponding data type
            long profileId = Long.parseLong(parts[0]);
            String profileImageUrl = parts[1].equals("null") ? null : parts[1];
            String bio = parts[2].equals("null") ? null : parts[2];
            String username = parts[3].equals("null") ? null : parts[3];
            String password = parts[4].equals("null") ? null : parts[4];
            Timestamp updatedAt = parts[5].equals("null") ? null : Timestamp.valueOf(parts[5]);
            String profileName = parts[6].equals("null") ? null : parts[6];

            // Create and return a new object with the extracted values.
            return new UserProfile(profileId, profileImageUrl, bio, username, password, updatedAt, profileName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}