package com.menzo.menzo.dto.user;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OnboardingRequest(
        @NotBlank @Size(min = 2, max = 60) String displayName,
        @NotBlank @Size(min = 2, max = 30) @Pattern(regexp = "^[a-z0-9_.]+$", message = "Solo minúsculas, números, puntos y guiones bajos")
        String username,
        @NotBlank String aura,
        String avatarUri,
        @NotBlank String avatarGradient,
        @NotNull List<@NotBlank String> interests) {
}
