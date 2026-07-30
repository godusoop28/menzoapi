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
}
