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
 * Una fila cubre tanto "cola" como "solicitud" (ver sección 8 del pedido: "LiveMusicRequest solo
 * si no se puede reutilizar QueueItem" — sí se puede): una solicitud de MEMBER nace en PENDING
 * sin `position`; al aprobarla pasa a QUEUED con una posición al final de la cola. Lo que agrega
 * directo un OWNER/CO_HOST nace ya en QUEUED.
 */
@Entity
@Table(name = "live_music_queue_items")
@Getter
@Setter
@NoArgsConstructor
public class LiveMusicQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "music_session_id", nullable = false)
    private UUID musicSessionId;

    @Column(name = "video_id", nullable = false, length = 20)
    private String videoId;

    @Column(length = 300)
    private String title;

    @Column(name = "channel_title", length = 200)
    private String channelTitle;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "requested_by_user_id")
    private UUID requestedByUserId;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    /** Solo tiene sentido (y es único) mientras status=QUEUED — ver índice parcial en la
     * migración. PENDING/PLAYED/SKIPPED/REJECTED/REMOVED no ordenan entre sí. */
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QueueItemStatus status = QueueItemStatus.QUEUED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;
}
