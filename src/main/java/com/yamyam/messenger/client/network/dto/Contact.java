package com.yamyam.messenger.client.network.dto;

public record Contact(
        long id,
        String title,
        String avatarUrl,
        boolean online,
        ContactType type,       // DIRECT, GROUP, CHANNEL
        Integer memberCount     // فقط برای GROUP و CHANNEL
) {}