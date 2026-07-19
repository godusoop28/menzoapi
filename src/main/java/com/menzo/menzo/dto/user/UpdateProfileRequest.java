package com.menzo.menzo.dto.user;

import java.util.List;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 60) String displayName,
        String avatarUri,
        String avatarGradient,
        String aura,
        @Size(max = 280) String bio,
        @Size(max = 140) String statusText,
        List<String> interests) {
}
