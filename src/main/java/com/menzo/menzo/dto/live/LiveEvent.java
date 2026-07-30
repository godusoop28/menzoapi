package com.menzo.menzo.dto.live;

import java.time.Instant;
import java.util.UUID;

public record LiveEvent(
        String eventId,
        LiveEventType type,
        UUID roomId,
        UUID liveSessionId,
        Instant occurredAt,
        Object payload) {

    public static LiveEvent of(LiveEventType type, UUID roomId, UUID liveSessionId, Object payload) {
        return new LiveEvent(UUID.randomUUID().toString(), type, roomId, liveSessionId, Instant.now(), payload);
    }
}
