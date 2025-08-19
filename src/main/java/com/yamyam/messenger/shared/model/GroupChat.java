package com.yamyam.messenger.shared.model;

import java.sql.Timestamp;
import java.util.Currency;

public class GroupChat extends Chat {

    private String groupName;
    private String description;
    private long creatorId;
    private boolean isPrivate;

    public GroupChat(long id,String group,String description,long creatorId,boolean isPrivate){
        super(id,new Timestamp(System.currentTimeMillis()),ChatType.GROUP_CHAT);
        this.creatorId = creatorId;
        this.isPrivate = isPrivate;
        this.groupName = group;
        this.description = description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(long creatorId) {
        this.creatorId = creatorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
