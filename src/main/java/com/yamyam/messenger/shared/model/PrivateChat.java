package com.yamyam.messenger.shared.model;

import java.sql.Timestamp;

public class PrivateChat extends Chat {

    private Users user1;
    private Users user2;

    public PrivateChat(long id,Users user1 , Users user2){
        super(id, new Timestamp(System.currentTimeMillis()),ChatType.PRIVATE_CHAT);
        this.user1 = user1;
        this.user2 = user2;
    }

    public Users getUser2() {
        return user2;
    }

    public void setUser2(Users user2) {
        this.user2 = user2;
    }

    public Users getUser1() {
        return user1;
    }

    public void setUser1(Users user1) {
        this.user1 = user1;
    }
}
