package com.yamyam.messenger.shared.model;

import java.time.LocalDateTime;

public class ContactRelation {
    private final long ownerId;
    private final long contactId;
    private final LocalDateTime addedAt;

    public ContactRelation(long ownerId, long contactId, LocalDateTime addedAt) {
        this.ownerId = ownerId;
        this.contactId = contactId;
        this.addedAt = addedAt;
    }

    public long getContactId() {
        return contactId;
    }
    public LocalDateTime getAddedAt() {
        return addedAt;
    }
    public long getOwnerId() {
        return ownerId;
    }
}