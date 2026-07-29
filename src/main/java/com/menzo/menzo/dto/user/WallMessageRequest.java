package com.menzo.menzo.dto.user;

import jakarta.validation.constraints.Size;

public record WallMessageRequest(@Size(max = 500) String body, String imageUri) {
}
