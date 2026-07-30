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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Menzi DJ — estado canónico de la música de un LIVE (uno por chat_live_session, ver
 * uq_music_sessions_live_session en la migración). No transmite audio: YouTube IFrame Player
 * reproduce en cada dispositivo, esta fila es la única fuente de verdad de qué video, en qué
 * posición y en qué estado, para que todos calculen el mismo tiempo de reproducción sin confiar
 * en el reloj local de nadie (ver MusicService.computePosition).
 *
 * `version` es un @Version real de JPA (no solo un contador manual): si dos coanfitriones
 * mandan un control casi al mismo tiempo, Hibernate rechaza el segundo UPDATE con un
 * OptimisticLockingFailureException (mapeado a 409 en GlobalExceptionHandler) en vez de dejar
 * que uno pise al otro en silencio. El mismo valor se expone en el snapshot para que el cliente
 * mande `expectedVersion` y el servicio pueda devolver un 409 legible antes de tocar nada.
 */
@Entity
@Table(name = "live_music_sessions")
@Getter
@Setter
@NoArgsConstructor
public class LiveMusicSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "live_session_id", nullable = false, unique = true)
    private UUID liveSessionId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MusicSessionStatus status = MusicSessionStatus.IDLE;

    @Column(name = "current_queue_item_id")
    private UUID currentQueueItemId;

    @Column(name = "current_video_id", length = 20)
    private String currentVideoId;

    @Column(name = "current_title", length = 300)
    private String currentTitle;

    @Column(name = "current_channel_title", length = 200)
    private String currentChannelTitle;

    @Column(name = "current_thumbnail_url", columnDefinition = "text")
    private String currentThumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "position_seconds", nullable = false)
    private int positionSeconds = 0;

    @Column(name = "playback_started_at")
    private Instant playbackStartedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "started_by_user_id")
    private UUID startedByUserId;

    @Column(name = "controlled_by_user_id")
    private UUID controlledByUserId;

    @Column(name = "allow_requests", nullable = false)
    private boolean allowRequests = true;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
