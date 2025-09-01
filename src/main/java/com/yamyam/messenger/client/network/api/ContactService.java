package com.yamyam.messenger.client.network.api;

import com.yamyam.messenger.shared.model.Contact;

import java.util.List;

public interface ContactService {
    List<Contact> getContacts(long meUserId);
}
