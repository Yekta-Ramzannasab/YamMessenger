package com.yamyam.messenger.client.network.dto;

public record SearchItem(
        String title,
        String subtitle,
        String avatarUrl,
        SearchKind kind,
        Object rawEntity
) {}