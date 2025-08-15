package com.yamyam.messenger.shared.model;

import java.sql.Timestamp;

public class ChannelSubscribes {
    private Channel channel;
    private Role role;
    private Timestamp joinedAt;
    private Users subscribe;
    private boolean approve;

    public ChannelSubscribes(Channel channel,Role role,Users subscribe,boolean approve) {
        this.channel = channel;
        this.role = role;
        this.subscribe = subscribe;
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

    public Users getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Users subscribe) {
        this.subscribe = subscribe;
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
