package com.menzo.menzo.dto.chat;

import java.time.Instant;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record MessageResponse(
        UUID id,
        UUID roomId,
        String authorId,
        UserSummary author,
        String type,
        String body,
        String imageUri,
        Instant createdAt) {
}
