package com.menzo.menzo.dto.user;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String displayName,
        String username,
        String avatarUri,
        String avatarGradient,
        boolean isOnline) {
}
