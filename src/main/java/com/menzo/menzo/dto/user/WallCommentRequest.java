package com.menzo.menzo.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WallCommentRequest(@NotBlank @Size(max = 1000) String body) {
}
