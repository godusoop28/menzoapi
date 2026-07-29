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
 * Directorio persistido de salas en vivo — sobrevive a un restart o a más de una instancia,
 * a diferencia del mapa en memoria de VoiceService (que sigue siendo la fuente de verdad para
 * "quién está conectado ahora mismo", ver ese archivo). startedAt/lastHeartbeatAt se setean a
 * mano en el servicio, no con @CreationTimestamp — evita el mismo problema de lectura-antes-de-
 * flush que ya se encontró y corrigió en ChatService/UserService/PostService.
 */
@Entity
@Table(name = "chat_live_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatLiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveSessionType type = LiveSessionType.VOICE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LiveSessionStatus status = LiveSessionStatus.ACTIVE;

    @Column(name = "started_by_user_id")
    private UUID startedByUserId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(name = "agora_channel_name", length = 100)
    private String agoraChannelName;

    @Column(name = "last_heartbeat_at", nullable = false)
    private Instant lastHeartbeatAt;
}
