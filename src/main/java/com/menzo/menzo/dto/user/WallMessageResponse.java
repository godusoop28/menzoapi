package com.menzo.menzo.dto.user;

import java.time.Instant;
import java.util.UUID;

public record WallMessageResponse(
        UUID id,
        UUID profileId,
        UserSummary author,
        String body,
        Instant createdAt) {
}
