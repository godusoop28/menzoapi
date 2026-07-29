package com.menzo.menzo.dto.chat;

import java.time.Instant;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record ChatRoomResponse(
        UUID id,
        String slug,
        String name,
        String description,
        String topic,
        String gradient,
        String icon,
        String type,
        String coverUri,
        String backgroundUri,
        UserSummary peer,
        long memberCount,
        long onlineCount,
        boolean favorite,
        boolean joined,
        String role,
        boolean live,
        Instant createdAt,
        LastMessage lastMessage) {

    public record LastMessage(String body, boolean hasImage, String senderId, Instant createdAt) {
    }
}
