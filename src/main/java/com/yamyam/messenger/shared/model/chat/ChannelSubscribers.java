package com.yamyam.messenger.shared.model.chat;

import java.sql.Timestamp;

public class ChannelSubscribers {
    private Channel channel;
    private Role role;
    private Timestamp joinedAt;
    private long subscribeId;
    private boolean approve;

    public ChannelSubscribers(Channel channel, Role role, long subscribeId, boolean approve) {
        this.channel = channel;
        this.role = role;
        this.subscribeId = subscribeId;
        this.approve = approve;
        this.joinedAt = new Timestamp(System.currentTimeMillis());
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }

    public long getSubscribe() {
        return subscribeId;
    }

    public void setSubscribe(long subscribeId) {
        this.subscribeId = subscribeId;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
