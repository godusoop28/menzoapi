package com.menzo.menzo.dto.user;

import java.util.UUID;

import jakarta.validation.constraints.Size;

public record WallCommentRequest(@Size(max = 1000) String body, String imageUri, UUID parentCommentId) {
}
