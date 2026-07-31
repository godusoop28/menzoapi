package com.menzo.menzo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.config.AgoraProperties;
import com.menzo.menzo.domain.chat.ChatLiveSession;
import com.menzo.menzo.domain.chat.LiveSessionStatus;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.user.UserSummary;
import com.menzo.menzo.dto.voice.VoiceParticipantsResponse;
import com.menzo.menzo.dto.voice.VoiceTokenResponse;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.ChatLiveSessionRepository;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.security.agora.RtcTokenBuilder2;
import com.menzo.menzo.service.mapper.ProfileMapper;

/**
 * Presencia de voz en tiempo real: quién está en la llamada de cada sala ahora mismo vive en
 * memoria (participantsByRoom) — es estado de sub-segundo (nivel de volumen, quién está
 * conectado ESTE instante) que no tiene sentido persistir tick a tick. Pero el DIRECTORIO de
 * "qué salas están en vivo" (para listados/carrusel) sí se persiste en chat_live_sessions,
 * porque el mapa en memoria se pierde en cada restart/deploy y no funcionaría con más de una
 * instancia de Render. lastHeartbeatAt se toca en cada join/leave/participants de este flujo
 * viejo (los clientes que todavía usan /voice/* hacen polling de participants() cada 5s, que
 * alcanza como heartbeat) — el flujo nuevo (/live/*, ver LiveService) tiene su propio endpoint
 * de heartbeat dedicado, llamado por el cliente cada 15s mientras hay un LIVE activo. Una
 * sesión ACTIVE cuyo heartbeat se puso viejo (proceso reiniciado, cliente caído sin avisar, o
 * simplemente sin heartbeat) deja de contar como "en vivo" sola.
 */
@Service
public class VoiceService {

    private static final int TOKEN_EXPIRE_SECONDS = 3600;
    // 3x el intervalo de heartbeat de LiveNotifier (15s) — suficiente margen para tolerar un
    // tick perdido por una red lenta sin que la sesión se dé por terminada de más.
    private static final long LIVE_HEARTBEAT_TTL_SECONDS = 45;

    private final AgoraProperties agoraProperties;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final ChatLiveSessionRepository chatLiveSessionRepository;
    private final Map<UUID, Set<UUID>> participantsByRoom = new ConcurrentHashMap<>();

    public VoiceService(
            AgoraProperties agoraProperties,
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository,
            UserRepository userRepository,
            ProfileMapper profileMapper,
            ChatLiveSessionRepository chatLiveSessionRepository) {
        this.agoraProperties = agoraProperties;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.profileMapper = profileMapper;
        this.chatLiveSessionRepository = chatLiveSessionRepository;
    }

    @Transactional(readOnly = true)
    public VoiceTokenResponse token(User me, UUID roomId) {
        requireMember(roomId, me);
        String channelName = "room-" + roomId;
        String uid = me.getId().toString();
        String token = new RtcTokenBuilder2().buildTokenWithUserAccount(
                agoraProperties.getAppId(),
                agoraProperties.getAppCertificate(),
                channelName,
                uid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                TOKEN_EXPIRE_SECONDS,
                TOKEN_EXPIRE_SECONDS);
        return new VoiceTokenResponse(agoraProperties.getAppId(), channelName, token, uid);
    }

    @Transactional
    public VoiceParticipantsResponse join(User me, UUID roomId) {
        requireMember(roomId, me);
        Set<UUID> current = participantsByRoom.computeIfAbsent(roomId, id -> ConcurrentHashMap.newKeySet());
        current.add(me.getId());
        openOrTouchSession(roomId, me.getId(), current.size());
        return participants(roomId);
    }

    @Transactional
    public VoiceParticipantsResponse leave(User me, UUID roomId) {
        Set<UUID> current = participantsByRoom.get(roomId);
        int remaining = 0;
        if (current != null) {
            current.remove(me.getId());
            remaining = current.size();
            if (current.isEmpty()) {
                participantsByRoom.remove(roomId);
            }
        }
        if (remaining == 0) {
            closeSession(roomId);
        } else {
            touchSession(roomId, remaining);
        }
        return participants(roomId);
    }

    @Transactional
    public VoiceParticipantsResponse participants(UUID roomId) {
        Set<UUID> ids = participantsByRoom.getOrDefault(roomId, Set.of());
        if (!ids.isEmpty()) {
            // Los clientes hacen polling de este endpoint cada 5s mientras están en la sala —
            // aprovechar esa llamada como heartbeat evita necesitar un endpoint/scheduler aparte.
            touchSession(roomId, ids.size());
        }
        List<UserSummary> summaries = new ArrayList<>();
        for (UUID userId : ids) {
            userRepository.findById(userId).ifPresent(user -> summaries.add(profileMapper.toSummary(user)));
        }
        return new VoiceParticipantsResponse(summaries);
    }

    /** Usado por ChatService para marcar una sala como EN VIVO en los listados. Consulta el
     * directorio persistido (no el mapa en memoria) filtrando por heartbeat reciente, así que una
     * sesión huérfana (proceso reiniciado, cliente caído sin avisar) deja de contar sola, sin
     * necesitar tocarla primero. */
    @Transactional(readOnly = true)
    public boolean isLive(UUID roomId) {
        return chatLiveSessionRepository.existsByRoomIdAndStatusAndLastHeartbeatAtAfter(
                roomId, LiveSessionStatus.ACTIVE, Instant.now().minusSeconds(LIVE_HEARTBEAT_TTL_SECONDS));
    }

    /** Ids de las salas realmente en vivo ahora mismo, para el directorio/carrusel — corre la
     * reconciliación perezosa primero (cierra sesiones ACTIVE con heartbeat viejo) así la tabla
     * no acumula filas huérfanas indefinidamente. */
    @Transactional
    public List<UUID> liveRoomIds() {
        Instant cutoff = Instant.now().minusSeconds(LIVE_HEARTBEAT_TTL_SECONDS);
        chatLiveSessionRepository.endStaleSessions(cutoff, Instant.now(), LiveSessionStatus.ACTIVE, LiveSessionStatus.ENDED);
        return chatLiveSessionRepository
                .findByStatusAndLastHeartbeatAtAfterOrderByStartedAtDesc(LiveSessionStatus.ACTIVE, cutoff)
                .stream()
                .map(ChatLiveSession::getRoomId)
                .toList();
    }

    private void openOrTouchSession(UUID roomId, UUID startedByUserId, int participantCount) {
        ChatLiveSession session = chatLiveSessionRepository
                .findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE)
                .orElseGet(() -> {
                    ChatLiveSession created = new ChatLiveSession();
                    created.setRoomId(roomId);
                    created.setStartedByUserId(startedByUserId);
                    created.setStartedAt(Instant.now());
                    created.setAgoraChannelName("room-" + roomId);
                    return created;
                });
        session.setParticipantCount(participantCount);
        session.setLastHeartbeatAt(Instant.now());
        chatLiveSessionRepository.save(session);
    }

    private void touchSession(UUID roomId, int participantCount) {
        chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE).ifPresent(session -> {
            session.setParticipantCount(participantCount);
            session.setLastHeartbeatAt(Instant.now());
            chatLiveSessionRepository.save(session);
        });
    }

    private void closeSession(UUID roomId) {
        chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE).ifPresent(session -> {
            session.setStatus(LiveSessionStatus.ENDED);
            session.setEndedAt(Instant.now());
            session.setParticipantCount(0);
            chatLiveSessionRepository.save(session);
        });
    }

    private void requireMember(UUID roomId, User me) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new NotFoundException("Sala no encontrada");
        }
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            throw new ForbiddenException("Tenés que unirte a la sala antes de entrar a la voz");
        }
    }
}
