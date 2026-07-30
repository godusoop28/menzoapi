package com.menzo.menzo.dto.music;

import java.util.List;
import java.util.UUID;

/** Snapshot completo de Menzi DJ para una sala — `positionSeconds` ya viene calculado (posición
 * canónica real en este instante, no el valor crudo guardado), así el cliente solo necesita
 * cargar el video y saltar a esta posición, sin hacer la cuenta de elapsed/playbackStartedAt
 * por su cuenta. `version` es el mismo valor a mandar como `expectedVersion` en el próximo
 * control que haga este cliente. */
public record MusicSessionResponse(
        UUID musicSessionId,
        UUID roomId,
        UUID liveSessionId,
        String status,
        UUID currentQueueItemId,
        String currentVideoId,
        String currentTitle,
        String currentChannelTitle,
        String currentThumbnailUrl,
        Integer durationSeconds,
        int positionSeconds,
        boolean allowRequests,
        long version,
        List<QueueItemResponse> queue,
        List<QueueItemResponse> pendingRequests,
        List<QueueItemResponse> history) {
}
