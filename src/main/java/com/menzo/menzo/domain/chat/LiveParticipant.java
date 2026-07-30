package com.menzo.menzo.domain.chat;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila por (liveSessionId, userId) — persiste el rol/estado real de cada participante del
 * LIVE (a diferencia del mapa en memoria de VoiceService, que solo sabe "quién está conectado
 * ahora" para el flujo viejo /voice/* usado por la app móvil). roomId está denormalizado para
 * poder consultar "mis participaciones en esta sala" sin tener que joinear con
 * chat_live_sessions primero.
 */
@Entity
@Table(name = "live_participants")
@Getter
@Setter
@NoArgsConstructor
public class LiveParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "live_session_id", nullable = false)
    private UUID liveSessionId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private LiveParticipantRole role = LiveParticipantRole.AUDIENCE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LiveParticipantStatus status = LiveParticipantStatus.ACTIVE;

    @Column(name = "microphone_enabled", nullable = false)
    private boolean microphoneEnabled = false;

    @Column(name = "requested_to_speak_at")
    private Instant requestedToSpeakAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    /** Solo para aplicar el cooldown entre solicitudes rechazadas — no forma parte del contrato
     * público (no se expone en LiveParticipantResponse). */
    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();
}
