package com.menzo.menzo.dto.chat;

import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

/**
 * createdAt es String, no Instant — a propósito. Depender de que el ObjectMapper serialice
 * Instant truncado a milisegundos resultó no ser confiable acá (ver JacksonConfig), así que este
 * campo llega ya formateado desde ChatService, sin pasar por la resolución de serializers de
 * Jackson para java.time.Instant en absoluto.
 */
public record MessageResponse(
        UUID id,
        UUID roomId,
        String authorId,
        UserSummary author,
        String type,
        String body,
        String imageUri,
        String createdAt) {
}
