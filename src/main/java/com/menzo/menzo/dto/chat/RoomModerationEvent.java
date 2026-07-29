package com.menzo.menzo.dto.chat;

import java.util.UUID;

/**
 * Publicado en /topic/rooms/{roomId}/moderation. Además del mensaje de sistema legible que ya se
 * escribe en el historial de la sala, este evento le permite al cliente del usuario afectado
 * (kickeado/baneado) reaccionar (salir de la vista de la sala) sin tener que interpretar texto.
 */
public record RoomModerationEvent(
        String type,
        UUID roomId,
        UUID targetUserId,
        String targetDisplayName,
        UUID actorUserId,
        String actorDisplayName,
        String newRole) {
}
