package com.yamyam.messenger.shared.model;

import java.sql.Timestamp;

public class Channel extends Chat{

    private String channelName;
    private Users owner;
    private String description;
    private boolean isPrivate;

    public Channel(long id,String channelName,Users owner,boolean isPrivate,String description){
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

    public Users getOwner() {
        return owner;
    }

    public void setOwner(Users owner) {
        this.owner = owner;
    }
}
