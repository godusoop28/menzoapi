package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.LiveParticipant;
import com.menzo.menzo.domain.chat.LiveParticipantRole;
import com.menzo.menzo.domain.chat.LiveParticipantStatus;

public interface LiveParticipantRepository extends JpaRepository<LiveParticipant, UUID> {

    Optional<LiveParticipant> findByLiveSessionIdAndUserId(UUID liveSessionId, UUID userId);

    List<LiveParticipant> findByLiveSessionIdAndStatus(UUID liveSessionId, LiveParticipantStatus status);

    List<LiveParticipant> findByLiveSessionIdAndStatusAndRoleOrderByRequestedToSpeakAtAsc(
            UUID liveSessionId, LiveParticipantStatus status, LiveParticipantRole role);

    long countByLiveSessionIdAndStatusAndRoleIn(
            UUID liveSessionId, LiveParticipantStatus status, List<LiveParticipantRole> roles);

    long countByLiveSessionIdAndStatus(UUID liveSessionId, LiveParticipantStatus status);

    List<LiveParticipant> findByRoomIdAndStatus(UUID roomId, LiveParticipantStatus status);

    /** Para el uno-a-la-vez de screen share (ver LiveService.setScreenSharing) — a lo sumo una
     * fila "true" por sesión en la práctica, pero es una lista (no Optional) porque nada en el
     * schema lo garantiza a nivel de constraint; el servicio la trata como "el/los que había
     * compartiendo antes de este cambio", nunca asume tamaño 0 o 1. */
    List<LiveParticipant> findByLiveSessionIdAndScreenSharingTrue(UUID liveSessionId);
}
