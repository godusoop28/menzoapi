package com.menzo.menzo.dto.moderation;

import java.time.Instant;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record ModerationActionResponse(
        UUID id,
        UserSummary actor,
        String actionType,
        String targetType,
        UUID targetId,
        String reason,
        Instant createdAt) {
}
