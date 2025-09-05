package com.yamyam.messenger.shared.model.chat;

import java.sql.Timestamp;

public class Channel extends Chat {

    private String channelName;
    private long owner;
    private String description;
    private boolean isPrivate;
    private String channelAvatarUrl;
    private int subscriberCount;


    public Channel(long id, String channelName, long owner, boolean isPrivate, String description, String avatarUrl) {
        super(id, new Timestamp(System.currentTimeMillis()), ChatType.CHANNEL);
        this.channelName = channelName;
        this.description = description;
        this.isPrivate = isPrivate;
        this.owner = owner;
        this.channelAvatarUrl = avatarUrl;
    }


    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getOwner() {
        return owner;
    }

    public void setOwner(long owner) {
        this.owner = owner;
    }

    public String getChannelAvatarUrl() {
        return channelAvatarUrl;
    }

    public int getSubscriberCount() {
        return subscriberCount;
    }
}
