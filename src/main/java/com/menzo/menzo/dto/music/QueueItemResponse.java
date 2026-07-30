package com.menzo.menzo.dto.music;

import java.time.Instant;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record QueueItemResponse(
        UUID id,
        String videoId,
        String title,
        String channelTitle,
        String thumbnailUrl,
        Integer durationSeconds,
        UserSummary requestedBy,
        UserSummary approvedBy,
        Integer position,
        String status,
        Instant createdAt) {
}
