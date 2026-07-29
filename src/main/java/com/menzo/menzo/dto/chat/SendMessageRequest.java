package com.menzo.menzo.dto.chat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(@Size(max = 2000) String body, String imageUri) {

    @AssertTrue(message = "El mensaje necesita texto o una imagen")
    public boolean isBodyOrImagePresent() {
        return (body != null && !body.isBlank()) || (imageUri != null && !imageUri.isBlank());
    }
}
