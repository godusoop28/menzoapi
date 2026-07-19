package com.menzo.menzo.dto.auth;

import java.time.Instant;

import com.menzo.menzo.dto.user.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        String email,
        boolean onboardingCompleted,
        UserProfileResponse profile) {
}
