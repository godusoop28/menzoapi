package com.menzo.menzo.dto.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String displayName,
        String username,
        String avatarUri,
        String avatarGradient,
        String aura,
        String bio,
        String statusText,
        List<String> interests,
        Instant joinedAt,
        int level,
        int xp,
        int reputation,
        long followers,
        long following,
        long visitors,
        boolean isOnline,
        List<String> badges,
        boolean followedByMe) {
}
