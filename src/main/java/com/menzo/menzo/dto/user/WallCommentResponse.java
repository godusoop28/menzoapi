package com.menzo.menzo.dto.user;

import java.time.Instant;
import java.util.UUID;

public record WallCommentResponse(
        UUID id,
        UUID wallMessageId,
        UserSummary author,
        String body,
        Instant createdAt,
        long likeCount,
        boolean likedByMe) {
}
