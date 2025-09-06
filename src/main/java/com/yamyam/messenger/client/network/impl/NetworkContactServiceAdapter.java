package com.yamyam.messenger.client.network.impl;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.dto.ContactType;
import com.yamyam.messenger.client.network.service.ContactService;
import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.shared.model.chat.Channel;
import com.yamyam.messenger.shared.model.chat.Chat;
import com.yamyam.messenger.shared.model.chat.GroupChat;
import com.yamyam.messenger.shared.model.chat.PrivateChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkContactServiceAdapter implements ContactService {
    private final NetworkService net;

    public NetworkContactServiceAdapter(NetworkService net) {
        this.net = net;
    }

    @Override
    public List<Contact> getContacts(long meUserId) {
        List<Chat> chats = NetworkService.getInstance().fetchAllChatsForUser(meUserId);
        if (chats == null || chats.isEmpty()) return Collections.emptyList();

        List<Contact> contacts = new ArrayList<>();

        for (Chat chat : chats) {
            if (chat instanceof PrivateChat pc) {
                long otherId = (pc.getUser1() == meUserId) ? pc.getUser2() : pc.getUser1();
                Contact contact = NetworkService.fetchContactDetailsById(otherId);

                if (contact != null) {
                    contacts.add(new Contact(
                            contact.id(),
                            contact.title(),
                            contact.avatarUrl(),
                            contact.online(),
                            ContactType.DIRECT,
                            null
                    ));
                }
            }

            else if (chat instanceof GroupChat gc) {
                contacts.add(new Contact(
                        gc.getChatId(),
                        gc.getGroupName(),
                        gc.getGroupAvatarUrl(),
                        false,
                        ContactType.GROUP,
                        gc.getMemberCount()
                ));
            }

            else if (chat instanceof Channel ch) {
                contacts.add(new Contact(
                        ch.getChatId(),
                        ch.getChannelName(),
                        ch.getChannelAvatarUrl(),
                        false,
                        ContactType.CHANNEL,
                        ch.getSubscriberCount()
                ));
            }
        }

        return contacts;
    }
}