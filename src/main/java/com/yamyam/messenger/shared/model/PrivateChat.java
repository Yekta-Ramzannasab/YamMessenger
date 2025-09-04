package com.yamyam.messenger.shared.model;

import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.client.network.dto.ContactType;

import java.sql.Timestamp;

public class PrivateChat extends Chat {

    private long user1;
    private long user2;

    public PrivateChat(long id,long user1 , long user2){
        super(id, new Timestamp(System.currentTimeMillis()),ChatType.PRIVATE_CHAT);
        this.user1 = user1;
        this.user2 = user2;
    }

    public long getUser2() {
        return user2;
    }

    public void setUser2(long user2) {
        this.user2 = user2;
    }

    public long getUser1() {
        return user1;
    }

    public void setUser1(long user1) {
        this.user1 = user1;
    }
    public Contact toContact(Users targetUser) {
        return new Contact(
                this.getChatId(),
                targetUser.getUserProfile().getProfileName(),
                targetUser.getUserProfile().getProfileImageUrl(),
                targetUser.isOnline(),
                ContactType.DIRECT,
                null
        );
    }
}
