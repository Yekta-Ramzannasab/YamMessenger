package com.yamyam.messenger.client.network.api;

import java.util.List;
import com.yamyam.messenger.client.network.dto.Contact;

public interface ContactService {
    List<Contact> getContacts(long meUserId);
}
