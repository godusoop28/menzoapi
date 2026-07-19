package com.menzo.menzo.dto.community;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String category,
        String title,
        String body,
        Instant createdAt,
        boolean read,
        UUID relatedPostId,
        UUID relatedRoomId,
        UUID relatedUserId,
        UUID relatedEventId) {
}
