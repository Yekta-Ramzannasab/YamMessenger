package com.yamyam.messenger.shared.model;

import java.io.Serializable;

public class Message implements Serializable {

    public int type; // e.g., 0 = login, 1 = chat message, 2 = file upload, 5 = verification code, etc. 3 = getPrivate chats
    public String sender;
    public String content;    // chat text, filename, password, etc.

    public Message(int type , String sender , String content) {
        this.type = type ;
        this.sender = sender ;
        this.content = content ;
    }

    public int getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }
}