package com.menzo.menzo.dto.live;

import java.time.Instant;

import com.menzo.menzo.dto.user.UserSummary;

public record LiveParticipantResponse(
        UserSummary user,
        String role,
        boolean microphoneEnabled,
        boolean screenSharing,
        Instant requestedToSpeakAt,
        Instant joinedAt) {
}
