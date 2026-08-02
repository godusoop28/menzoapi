package com.menzo.menzo.dto.chat;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(@Size(max = 2000) String body, String imageUri, UUID replyToMessageId) {

    @AssertTrue(message = "El mensaje necesita texto o una imagen")
    public boolean isBodyOrImagePresent() {
        return (body != null && !body.isBlank()) || (imageUri != null && !imageUri.isBlank());
    }
}
