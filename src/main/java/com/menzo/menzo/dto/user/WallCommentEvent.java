package com.menzo.menzo.dto.user;

import java.util.UUID;

/**
 * Publicado en /topic/wall/{wallMessageId}/comments. "created" trae el comentario completo;
 * "deleted" solo el id, para que el cliente lo saque de su lista sin tener que re-fetch.
 */
public record WallCommentEvent(String type, WallCommentResponse comment, UUID deletedCommentId) {

    public static WallCommentEvent created(WallCommentResponse comment) {
        return new WallCommentEvent("created", comment, null);
    }

    public static WallCommentEvent deleted(UUID commentId) {
        return new WallCommentEvent("deleted", null, commentId);
    }
}
