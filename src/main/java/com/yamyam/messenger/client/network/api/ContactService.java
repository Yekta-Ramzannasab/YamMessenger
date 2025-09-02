package com.yamyam.messenger.client.network.api;

import com.yamyam.messenger.shared.model.ContactRelation;

import java.util.List;

public interface ContactService {
    List<ContactRelation> getContacts(long meUserId);
}
