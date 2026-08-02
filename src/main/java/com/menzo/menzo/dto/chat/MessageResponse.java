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
        String createdAt,
        ReplyPreview replyTo,
        boolean deleted,
        StickerPreview sticker) {

    /**
     * Copia liviana del mensaje citado, embebida acá para que ningún cliente necesite un segundo
     * fetch solo para pintar la cita. `deleted=true` cuando el mensaje original ya no existe (la
     * FK usa ON DELETE SET NULL, ver V18__message_reply.sql) — en ese caso `authorName`/`bodyPreview`
     * no traen el contenido real, los clientes deben mostrar "Mensaje eliminado" en su lugar.
     */
    public record ReplyPreview(UUID id, String authorName, String bodyPreview, boolean deleted) {
    }

    /** Igual razonamiento que ReplyPreview: evita un segundo fetch al picker solo para pintar la
     * burbuja. UUID suelto en Message.stickerId (ver V25__stickers.sql), así que si el sticker
     * ya no existe esto viene null y el cliente cae a un placeholder simple. */
    public record StickerPreview(UUID id, String imageUrl) {
    }
}
