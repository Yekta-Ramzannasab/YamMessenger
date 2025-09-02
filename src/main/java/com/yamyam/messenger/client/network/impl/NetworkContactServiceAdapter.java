package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.service.ContactService;
import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.shared.model.ContactRelation;

import java.util.ArrayList;
import java.util.List;

public class NetworkContactServiceAdapter implements ContactService {
    private final NetworkService net;
    public NetworkContactServiceAdapter(NetworkService net) { this.net = net; }

    @Override
    public List<Contact> getContacts(long meUserId) {
        // Getting raw data from the server
        List<ContactRelation> relations = NetworkService.fetchMyContactRelations(meUserId);

        // Translating raw data into displayable data (DTO)
        List<Contact> contactDtos = new ArrayList<>();
        for (ContactRelation relation : relations) {
            // For each relationship, we need to get the complete profile information of the contact
            Contact contactDetails = NetworkService.fetchContactDetailsById(relation.getContactId());

            if (contactDetails != null) {
                contactDtos.add(contactDetails);
            }
        }
        return contactDtos;
    }
}
