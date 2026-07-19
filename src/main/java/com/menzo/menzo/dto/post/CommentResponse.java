package com.menzo.menzo.dto.post;

import java.time.Instant;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record CommentResponse(
        UUID id,
        UUID postId,
        UserSummary author,
        String body,
        Instant createdAt) {
}
