package com.menzo.menzo.dto.chat;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @Size(max = 2000) String body, String imageUri, UUID replyToMessageId, UUID stickerId) {

    /** Exactamente uno de texto/imagen/sticker — igual que WhatsApp/Telegram, un sticker no
     * se combina con texto ni imagen en el mismo mensaje. */
    @AssertTrue(message = "El mensaje necesita texto, una imagen o un sticker")
    public boolean isBodyOrImagePresent() {
        boolean hasText = body != null && !body.isBlank();
        boolean hasImage = imageUri != null && !imageUri.isBlank();
        boolean hasSticker = stickerId != null;
        if (hasSticker) {
            return !hasText && !hasImage;
        }
        return hasText || hasImage;
    }
}
