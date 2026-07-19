package com.menzo.menzo.dto.chat;

import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        String slug,
        String name,
        String description,
        String topic,
        String gradient,
        String icon,
        long memberCount,
        long onlineCount,
        boolean favorite,
        boolean joined) {
}
