package com.menzo.menzo.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WallMessageRequest(@NotBlank @Size(max = 500) String body) {
}
