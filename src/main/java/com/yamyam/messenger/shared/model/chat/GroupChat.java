package com.yamyam.messenger.shared.model.chat;

import com.yamyam.messenger.shared.model.ChatType;

import java.sql.Timestamp;

public class GroupChat extends Chat {

    private String groupName;
    private String description;
    private long creatorId;
    private boolean isPrivate;
    private String groupAvatarUrl;
    private int memberCount;


    public GroupChat(long id, String name, String description, long creatorId, boolean isPrivate, String avatarUrl) {
        super(id, new Timestamp(System.currentTimeMillis()), ChatType.GROUP_CHAT);
        this.groupName = name;
        this.description = description;
        this.creatorId = creatorId;
        this.isPrivate = isPrivate;
        this.groupAvatarUrl = avatarUrl;
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

    public String getGroupAvatarUrl() {
        return groupAvatarUrl;
    }

    public int getMemberCount() {
        return memberCount;
    }
}
