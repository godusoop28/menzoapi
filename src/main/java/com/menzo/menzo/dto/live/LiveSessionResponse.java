package com.menzo.menzo.dto.live;

import java.time.Instant;
import java.util.UUID;

/** Estado completo del LIVE de una sala — null (el controller devuelve 204/omite) si no hay
 * sesión ACTIVE. myRole/myMicrophoneEnabled/hasPendingSpeakRequest son relativos al viewer
 * autenticado, para que la cabecera y el panel LIVE no tengan que cruzar con /participants. */
public record LiveSessionResponse(
        UUID id,
        UUID roomId,
        String type,
        String status,
        String title,
        String description,
        String announcement,
        UUID startedByUserId,
        Instant startedAt,
        long participantCount,
        long speakerCount,
        String agoraChannelName,
        String myRole,
        boolean myMicrophoneEnabled,
        boolean hasPendingSpeakRequest) {
}
