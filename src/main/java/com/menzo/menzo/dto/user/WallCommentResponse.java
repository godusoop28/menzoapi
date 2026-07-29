package com.menzo.menzo.dto.user;

import java.time.Instant;
import java.util.UUID;

public record WallCommentResponse(
        UUID id,
        UUID wallMessageId,
        UUID parentCommentId,
        UserSummary author,
        String body,
        String imageUri,
        Instant createdAt,
        long likeCount,
        boolean likedByMe) {
}
