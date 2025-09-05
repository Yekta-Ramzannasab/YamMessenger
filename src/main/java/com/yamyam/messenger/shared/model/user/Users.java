package com.yamyam.messenger.shared.model.user;

import java.sql.Timestamp;

public class Users {
    private long id;
    private Timestamp createAt;
    private Timestamp lastSeen;
    private boolean isVerified;
    private boolean isOnline;
    private boolean isDeleted;
    private String email;
    private double searchRank;
    private UserProfile userProfile;



    public Users(){}

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

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public double getSearchRank() {
        return searchRank;
    }

    public void setSearchRank(double searchRank) {
        this.searchRank = searchRank;
    }
    public String getName() {
        return userProfile != null ? userProfile.getProfileName() : "Unknown";
    }

    @Override
    public String toString (){
        return ( id + "," + createAt + "," + lastSeen + "," + isVerified + "," +
                isOnline + "," + isDeleted + "," + email + "," + searchRank + "," + userProfile ) ;
    }

    public static Users fromString(String userDataString) {
        if (userDataString == null || userDataString.isEmpty()) {
            return null;
        }

        // Split string into parts based on commas
        String[] parts = userDataString.split(",", 9);

        if (parts.length < 9) {
            return null; // if number of parts less than 8 return null
        }

        try {
            // Convert each part to the corresponding data type
            //    private long id;
//    private Timestamp createAt;
//    private Timestamp lastSeen;
//    private boolean isVerified;
//    private boolean isOnline;
//    private boolean isDeleted;
//    private String email;
//    private UserProfile userProfile;
//    private double searchRank;
            long id = Long.parseLong(parts[0]);
            Timestamp createAt = Timestamp.valueOf(parts[1]);
            Timestamp lastSeen = parts[2].equals("null") ? null : Timestamp.valueOf(parts[2]);
            boolean isVerified = Boolean.parseBoolean(parts[3]);
            boolean isOnline = Boolean.parseBoolean(parts[4]);
            boolean isDeleted = Boolean.parseBoolean(parts[5]);
            String email = parts[6];
            double searchRank = Double.parseDouble(parts[7]);

            // The last part of the string is related to the profile, which we will also reconstruct with fromString itself
            UserProfile userProfile = UserProfile.fromString(parts[8]);

            // Create and return a new object with the extracted values
            return new Users(id, createAt, lastSeen, isVerified, isOnline, isDeleted, email, userProfile);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}