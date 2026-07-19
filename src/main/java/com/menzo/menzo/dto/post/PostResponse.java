package com.menzo.menzo.dto.post;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record PostResponse(
        UUID id,
        UserSummary author,
        String type,
        String title,
        String body,
        String imageUri,
        AbstractVisualResponse abstractVisual,
        String gradient,
        List<String> tags,
        List<PollOptionResponse> pollOptions,
        UUID eventId,
        long likeCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        int commentCount,
        boolean featured,
        Instant createdAt) {
}
