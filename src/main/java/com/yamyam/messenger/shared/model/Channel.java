package com.yamyam.messenger.shared.model;

import java.sql.Timestamp;

public class Channel extends Chat{

    private String channelName;
    private long owner;
    private String description;
    private boolean isPrivate;

    public Channel(long id,String channelName,long owner,boolean isPrivate,String description){
        super(id,new Timestamp(System.currentTimeMillis()),ChatType.CHANNEL);
        this.channelName = channelName;
        this.description = description;
        this.isPrivate = isPrivate;
        this.owner = owner;
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
}
