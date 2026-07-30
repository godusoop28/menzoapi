package com.menzo.menzo.dto.music;

import java.time.Instant;
import java.util.UUID;

public record MusicEvent(
        String eventId,
        MusicEventType type,
        UUID roomId,
        UUID liveSessionId,
        UUID musicSessionId,
        long version,
        Instant occurredAt,
        Object payload) {

    public static MusicEvent of(MusicEventType type, UUID roomId, UUID liveSessionId, UUID musicSessionId, long version, Object payload) {
        return new MusicEvent(UUID.randomUUID().toString(), type, roomId, liveSessionId, musicSessionId, version, Instant.now(), payload);
    }
}
