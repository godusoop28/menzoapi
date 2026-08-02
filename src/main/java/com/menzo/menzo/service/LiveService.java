package com.menzo.menzo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.config.AgoraProperties;
import com.menzo.menzo.domain.chat.ChatLiveSession;
import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.LiveParticipant;
import com.menzo.menzo.domain.chat.LiveParticipantRole;
import com.menzo.menzo.domain.chat.LiveParticipantStatus;
import com.menzo.menzo.domain.chat.LiveSessionStatus;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.chat.RoomRole;
import com.menzo.menzo.domain.chat.RoomType;
import com.menzo.menzo.domain.community.NotificationCategory;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.chat.ChatRoomResponse;
import com.menzo.menzo.dto.live.LiveEvent;
import com.menzo.menzo.dto.live.LiveEventType;
import com.menzo.menzo.dto.live.LiveParticipantResponse;
import com.menzo.menzo.dto.live.LiveSessionResponse;
import com.menzo.menzo.dto.live.LiveTokenResponse;
import com.menzo.menzo.dto.live.StartLiveRequest;
import com.menzo.menzo.dto.live.UpdateLiveRequest;
import com.menzo.menzo.dto.user.UserSummary;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.ChatLiveSessionRepository;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.LiveParticipantRepository;
import com.menzo.menzo.repository.chat.RoomBanRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.security.agora.RtcTokenBuilder2;
import com.menzo.menzo.service.mapper.ProfileMapper;
import com.menzo.menzo.util.TextSanitizer;

/**
 * Sistema LIVE moderado: quién puede iniciar/terminar, quién es HOST/CO_HOST/SPEAKER/AUDIENCE,
 * solicitudes para hablar, y qué rol de Agora (publisher/subscriber) recibe cada quien. Única
 * autoridad real sobre join/leave/roles/mute — el flujo viejo (VoiceController, "cualquier
 * miembro = publisher") se retiró por completo tras verificar que ni menzomovil ni menzoweb lo
 * llamaban más (ver el comentario de clase en VoiceService). Ese servicio se redujo a
 * isLive()/liveRoomIds(), solo lectura, sobre chat_live_sessions — la misma tabla que esta clase
 * escribe, para que "¿esta sala está en vivo?" siga siendo una sola fuente de verdad.
 */
@Service
public class LiveService {

    private static final int TOKEN_EXPIRE_SECONDS = 3600;
    private static final long SPEAK_REQUEST_COOLDOWN_SECONDS = 30;
    private static final List<LiveParticipantRole> SPEAKING_ROLES =
            List.of(LiveParticipantRole.HOST, LiveParticipantRole.CO_HOST, LiveParticipantRole.SPEAKER);

    private final AgoraProperties agoraProperties;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomBanRepository roomBanRepository;
    private final UserRepository userRepository;
    private final ChatLiveSessionRepository chatLiveSessionRepository;
    private final LiveParticipantRepository liveParticipantRepository;
    private final ProfileMapper profileMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    public LiveService(
            AgoraProperties agoraProperties,
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository,
            RoomBanRepository roomBanRepository,
            UserRepository userRepository,
            ChatLiveSessionRepository chatLiveSessionRepository,
            LiveParticipantRepository liveParticipantRepository,
            ProfileMapper profileMapper,
            ApplicationEventPublisher eventPublisher,
            NotificationService notificationService) {
        this.agoraProperties = agoraProperties;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomBanRepository = roomBanRepository;
        this.userRepository = userRepository;
        this.chatLiveSessionRepository = chatLiveSessionRepository;
        this.liveParticipantRepository = liveParticipantRepository;
        this.profileMapper = profileMapper;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public LiveSessionResponse getState(UUID roomId, User viewer) {
        return chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE)
                .map(session -> toSessionResponse(session, viewer))
                .orElse(null);
    }

    /** Resumen liviano para tarjetas de sala/listados — solo se llama cuando ya se sabe que la
     * sala está en vivo (VoiceService.isLive), para no pagar esta consulta en salas inactivas. */
    @Transactional(readOnly = true)
    public ChatRoomResponse.LiveSummary getSummary(UUID roomId) {
        return chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE)
                .map(session -> {
                    UserSummary host = session.getStartedByUserId() != null
                            ? userRepository.findById(session.getStartedByUserId()).map(profileMapper::toSummary).orElse(null)
                            : null;
                    return new ChatRoomResponse.LiveSummary(
                            session.getId(),
                            session.getTitle(),
                            session.getAnnouncement(),
                            session.getParticipantCount(),
                            session.getSpeakerCount(),
                            host);
                })
                .orElse(null);
    }

    @Transactional
    public LiveSessionResponse startLive(User actor, UUID roomId, StartLiveRequest request) {
        ChatRoom room = getRoomOrThrow(roomId);
        RoomRole actorRole = requireOwnerOrCoHost(roomId, actor);
        if (chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE).isPresent()) {
            throw new ConflictException("Ya hay un LIVE activo en esta sala");
        }

        ChatLiveSession session = new ChatLiveSession();
        session.setRoomId(roomId);
        session.setStartedByUserId(actor.getId());
        session.setStartedAt(Instant.now());
        session.setLastHeartbeatAt(Instant.now());
        session.setAgoraChannelName("room-" + roomId);
        session.setTitle(TextSanitizer.normalizeToNull(request != null ? request.title() : null, 100));
        session.setDescription(TextSanitizer.normalizeToNull(request != null ? request.description() : null, 500));
        session.setAnnouncement(TextSanitizer.normalizeToNull(request != null ? request.announcement() : null, 300));
        session.setParticipantCount(1);
        session.setSpeakerCount(1);
        session = chatLiveSessionRepository.saveAndFlush(session);

        LiveParticipant host = new LiveParticipant();
        host.setLiveSessionId(session.getId());
        host.setRoomId(roomId);
        host.setUserId(actor.getId());
        host.setRole(actorRole == RoomRole.OWNER ? LiveParticipantRole.HOST : LiveParticipantRole.CO_HOST);
        // false, no true: el cliente (ver LiveNotifier._publishMic en Flutter) siempre publica
        // el audio ya silenciado por defecto y recién confirma acá con setMicrophone(true) si el
        // usuario lo activa — con esto en true, cualquier otro participante veía la insignia del
        // anfitrión como "con micrófono" mientras Agora lo tenía mudo de verdad, hasta que tocara
        // el botón dos veces (ver el fix del doble-toque en toggleMute).
        host.setMicrophoneEnabled(false);
        liveParticipantRepository.save(host);

        publish(LiveEventType.CHAT_LIVE_STARTED, roomId, session.getId(), toSessionResponse(session, actor));
        notifyLiveStarted(room, actor);
        return toSessionResponse(session, actor);
    }

    /** Notifica a los demás miembros de la sala (nunca al que lo inició) de que hay un LIVE
     * activo — a diferencia de un mensaje de chat, iniciar un LIVE es un evento poco frecuente y
     * de alta señal (como cuando Discord resalta un canal de voz que se activó), así que acá sí
     * tiene sentido avisarle a toda la sala en vez de solo al destinatario directo de una DM. */
    private void notifyLiveStarted(ChatRoom room, User actor) {
        String roomName = room.getName() != null ? room.getName() : "una sala";
        for (RoomMember member : roomMemberRepository.findByRoomId(room.getId())) {
            if (member.getUserId().equals(actor.getId())) continue;
            userRepository.findById(member.getUserId()).ifPresent(recipient ->
                    notificationService.create(
                            recipient,
                            NotificationCategory.en_vivo,
                            actor.getDisplayName() + " inició un LIVE",
                            "Se está reproduciendo en " + roomName + " — entrá para escuchar.",
                            null, room, actor, null));
        }
    }

    @Transactional
    public void endLive(User actor, UUID roomId) {
        requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);

        List<LiveParticipant> active = liveParticipantRepository.findByLiveSessionIdAndStatus(
                session.getId(), LiveParticipantStatus.ACTIVE);
        Instant now = Instant.now();
        for (LiveParticipant participant : active) {
            participant.setStatus(LiveParticipantStatus.LEFT);
            participant.setLeftAt(now);
            participant.setMicrophoneEnabled(false);
        }
        liveParticipantRepository.saveAll(active);

        session.setStatus(LiveSessionStatus.ENDED);
        session.setEndedAt(now);
        session.setParticipantCount(0);
        session.setSpeakerCount(0);
        chatLiveSessionRepository.save(session);

        publish(LiveEventType.CHAT_LIVE_ENDED, roomId, session.getId(), toSessionResponse(session, null));
    }

    @Transactional
    public LiveSessionResponse updateLiveInfo(User actor, UUID roomId, UpdateLiveRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        if (request.title() != null) {
            session.setTitle(TextSanitizer.normalizeToNull(request.title(), 100));
        }
        if (request.description() != null) {
            session.setDescription(TextSanitizer.normalizeToNull(request.description(), 500));
        }
        if (request.announcement() != null) {
            session.setAnnouncement(TextSanitizer.normalizeToNull(request.announcement(), 300));
        }
        session = chatLiveSessionRepository.save(session);
        publish(LiveEventType.CHAT_LIVE_UPDATED, roomId, session.getId(), toSessionResponse(session, actor));
        return toSessionResponse(session, actor);
    }

    @Transactional
    public LiveSessionResponse joinLive(User me, UUID roomId) {
        requireMember(roomId, me);
        if (roomBanRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            throw new ForbiddenException("Estás baneado de esta sala");
        }
        ChatLiveSession session = getActiveSessionOrThrow(roomId);

        LiveParticipant participant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(session.getId(), me.getId())
                .orElseGet(() -> {
                    LiveParticipant created = new LiveParticipant();
                    created.setLiveSessionId(session.getId());
                    created.setRoomId(roomId);
                    created.setUserId(me.getId());
                    created.setRole(defaultRoleFor(roomId, me));
                    return created;
                });
        participant.setStatus(LiveParticipantStatus.ACTIVE);
        participant.setLeftAt(null);
        participant.setLastSeenAt(Instant.now());
        liveParticipantRepository.save(participant);

        recomputeCounts(session);
        session.setLastHeartbeatAt(Instant.now());
        chatLiveSessionRepository.save(session);

        publish(LiveEventType.CHAT_LIVE_PARTICIPANT_JOINED, roomId, session.getId(), toParticipantResponse(participant));
        return toSessionResponse(session, me);
    }

    @Transactional
    public void leaveLive(User me, UUID roomId) {
        chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE).ifPresent(session -> {
            liveParticipantRepository.findByLiveSessionIdAndUserId(session.getId(), me.getId()).ifPresent(participant -> {
                if (participant.getStatus() == LiveParticipantStatus.ACTIVE) {
                    participant.setStatus(LiveParticipantStatus.LEFT);
                    participant.setLeftAt(Instant.now());
                    participant.setMicrophoneEnabled(false);
                    liveParticipantRepository.save(participant);
                    publish(LiveEventType.CHAT_LIVE_PARTICIPANT_LEFT, roomId, session.getId(),
                            toParticipantResponse(participant));
                }
            });
            recomputeCounts(session);
            session.setLastHeartbeatAt(Instant.now());
            chatLiveSessionRepository.save(session);
        });
    }

    @Transactional
    public LiveTokenResponse getToken(User me, UUID roomId) {
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant participant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(session.getId(), me.getId())
                .orElseThrow(() -> new ForbiddenException("Tenés que unirte al LIVE antes de pedir un token"));

        boolean canPublish = SPEAKING_ROLES.contains(participant.getRole());
        RtcTokenBuilder2.Role agoraRole = canPublish ? RtcTokenBuilder2.Role.ROLE_PUBLISHER : RtcTokenBuilder2.Role.ROLE_SUBSCRIBER;
        String uid = me.getId().toString();
        String token = new RtcTokenBuilder2().buildTokenWithUserAccount(
                agoraProperties.getAppId(),
                agoraProperties.getAppCertificate(),
                session.getAgoraChannelName(),
                uid,
                agoraRole,
                TOKEN_EXPIRE_SECONDS,
                TOKEN_EXPIRE_SECONDS);
        return new LiveTokenResponse(
                agoraProperties.getAppId(), session.getAgoraChannelName(), token, uid, canPublish ? "PUBLISHER" : "SUBSCRIBER");
    }

    /** Sin esto, una sesión ACTIVE que no tenga un join/leave/moderación de por medio se queda
     * con `lastHeartbeatAt` viejo para siempre — y VoiceService.liveRoomIds()/isLive() (que
     * corren en CUALQUIER lectura del directorio de salas, listados, carrusel de "en vivo",
     * etc.) barren como ENDED cualquier sesión ACTIVE cuyo heartbeat pasó de 30s
     * (LIVE_HEARTBEAT_TTL_SECONDS), sin importar que siga genuinamente en uso. El diseño viejo
     * (VoiceService, /voice/*) resolvía esto porque el cliente hacía polling de participants()
     * cada 5s; el flujo nuevo (/live/*) nunca heredó un mecanismo equivalente, así que un LIVE
     * que durara más de 30s sin que alguien se uniera o se fuera se auto-terminaba en silencio
     * — apenas alguien (el propio dueño, tras esperar y escribir una búsqueda en Menzi DJ, o
     * cualquier otro cliente refrescando la lista de salas) volvía a leer el estado. Ver
     * LiveNotifier._startHeartbeat en el cliente Flutter, que llama a esto cada 15s mientras
     * hay un LIVE activo.
     */
    @Transactional
    public void heartbeat(User me, UUID roomId) {
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        liveParticipantRepository.findByLiveSessionIdAndUserId(session.getId(), me.getId())
                .ifPresent(participant -> {
                    participant.setLastSeenAt(Instant.now());
                    liveParticipantRepository.save(participant);
                });
        session.setLastHeartbeatAt(Instant.now());
        chatLiveSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<LiveParticipantResponse> listParticipants(UUID roomId) {
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        List<LiveParticipant> participants = new ArrayList<>(
                liveParticipantRepository.findByLiveSessionIdAndStatus(session.getId(), LiveParticipantStatus.ACTIVE));
        participants.sort(Comparator.<LiveParticipant>comparingInt(p -> p.getRole().ordinal())
                .thenComparing(LiveParticipant::getJoinedAt));
        return participants.stream().map(this::toParticipantResponse).toList();
    }

    @Transactional
    public void requestToSpeak(User me, UUID roomId) {
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant participant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(session.getId(), me.getId())
                .orElseThrow(() -> new ForbiddenException("Tenés que estar escuchando el LIVE antes de solicitar hablar"));

        if (SPEAKING_ROLES.contains(participant.getRole())) {
            throw new BadRequestException("Ya podés hablar en este LIVE");
        }
        if (participant.getRole() == LiveParticipantRole.REQUESTED) {
            throw new ConflictException("Ya enviaste una solicitud para hablar");
        }
        if (participant.getRejectedAt() != null
                && participant.getRejectedAt().isAfter(Instant.now().minusSeconds(SPEAK_REQUEST_COOLDOWN_SECONDS))) {
            throw new ForbiddenException("Esperá un momento antes de volver a solicitar hablar");
        }

        participant.setRole(LiveParticipantRole.REQUESTED);
        participant.setRequestedToSpeakAt(Instant.now());
        liveParticipantRepository.save(participant);
        publish(LiveEventType.CHAT_LIVE_SPEAKING_REQUESTED, roomId, session.getId(), toParticipantResponse(participant));
    }

    /** El propio usuario retira su solicitud (distinto de que el OWNER/CO_HOST la rechace): no
     * aplica el cooldown de rechazo, porque cancelar voluntariamente no debería penalizar a quien
     * simplemente cambió de opinión. Reutiliza CHAT_LIVE_SPEAKING_REJECTED — para el resto de los
     * clientes (el panel de solicitudes del anfitrión) el efecto observable es idéntico: esta
     * solicitud desaparece de la lista de pendientes. */
    @Transactional
    public void cancelSpeakRequest(User me, UUID roomId) {
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant participant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(session.getId(), me.getId())
                .orElseThrow(() -> new ForbiddenException("No estás en este LIVE"));
        if (participant.getRole() != LiveParticipantRole.REQUESTED) {
            throw new BadRequestException("No tenés una solicitud pendiente");
        }
        participant.setRole(LiveParticipantRole.AUDIENCE);
        participant.setRequestedToSpeakAt(null);
        liveParticipantRepository.save(participant);
        publish(LiveEventType.CHAT_LIVE_SPEAKING_REJECTED, roomId, session.getId(), toParticipantResponse(participant));
    }

    @Transactional(readOnly = true)
    public List<LiveParticipantResponse> listSpeakingRequests(User actor, UUID roomId) {
        requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        return liveParticipantRepository
                .findByLiveSessionIdAndStatusAndRoleOrderByRequestedToSpeakAtAsc(
                        session.getId(), LiveParticipantStatus.ACTIVE, LiveParticipantRole.REQUESTED)
                .stream()
                .map(this::toParticipantResponse)
                .toList();
    }

    @Transactional
    public void approveSpeaking(User actor, UUID roomId, UUID targetUserId) {
        requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant target = findParticipantOrThrow(session.getId(), targetUserId);
        if (target.getRole() != LiveParticipantRole.REQUESTED) {
            throw new BadRequestException("Ese usuario no tiene una solicitud pendiente");
        }
        target.setRole(LiveParticipantRole.SPEAKER);
        target.setApprovedAt(Instant.now());
        target.setApprovedBy(actor.getId());
        liveParticipantRepository.save(target);

        recomputeCounts(session);
        chatLiveSessionRepository.save(session);
        publish(LiveEventType.CHAT_LIVE_SPEAKING_APPROVED, roomId, session.getId(), toParticipantResponse(target));
    }

    @Transactional
    public void rejectSpeaking(User actor, UUID roomId, UUID targetUserId) {
        requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant target = findParticipantOrThrow(session.getId(), targetUserId);
        if (target.getRole() != LiveParticipantRole.REQUESTED) {
            throw new BadRequestException("Ese usuario no tiene una solicitud pendiente");
        }
        target.setRole(LiveParticipantRole.AUDIENCE);
        target.setRejectedAt(Instant.now());
        target.setRequestedToSpeakAt(null);
        liveParticipantRepository.save(target);
        publish(LiveEventType.CHAT_LIVE_SPEAKING_REJECTED, roomId, session.getId(), toParticipantResponse(target));
    }

    @Transactional
    public void demoteParticipant(User actor, UUID roomId, UUID targetUserId) {
        RoomRole actorRole = requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant target = findParticipantOrThrow(session.getId(), targetUserId);
        requireCanModerateLiveParticipant(actorRole, target);
        if (target.getRole() != LiveParticipantRole.SPEAKER && target.getRole() != LiveParticipantRole.CO_HOST) {
            throw new BadRequestException("Ese usuario no está en el escenario");
        }
        target.setRole(LiveParticipantRole.AUDIENCE);
        target.setMicrophoneEnabled(false);
        liveParticipantRepository.save(target);

        recomputeCounts(session);
        chatLiveSessionRepository.save(session);
        publish(LiveEventType.CHAT_LIVE_PARTICIPANT_DEMOTED, roomId, session.getId(), toParticipantResponse(target));
    }

    @Transactional
    public void removeParticipant(User actor, UUID roomId, UUID targetUserId) {
        RoomRole actorRole = requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant target = findParticipantOrThrow(session.getId(), targetUserId);
        requireCanModerateLiveParticipant(actorRole, target);

        target.setStatus(LiveParticipantStatus.LEFT);
        target.setLeftAt(Instant.now());
        target.setMicrophoneEnabled(false);
        liveParticipantRepository.save(target);

        recomputeCounts(session);
        chatLiveSessionRepository.save(session);
        publish(LiveEventType.CHAT_LIVE_PARTICIPANT_LEFT, roomId, session.getId(), toParticipantResponse(target));
    }

    /** Espejo de estado para que los demás vean el badge de micrófono en tiempo real — la
     * publicación/mute real del audio ocurre en el cliente vía Agora; esto no otorga permisos. */
    @Transactional
    public void setMicrophone(User me, UUID roomId, boolean enabled) {
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant participant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(session.getId(), me.getId())
                .orElseThrow(() -> new ForbiddenException("No estás en este LIVE"));
        if (enabled && !SPEAKING_ROLES.contains(participant.getRole())) {
            throw new ForbiddenException("No tenés permiso para hablar en este LIVE");
        }
        participant.setMicrophoneEnabled(enabled);
        participant.setLastSeenAt(Instant.now());
        liveParticipantRepository.save(participant);
        publish(LiveEventType.CHAT_LIVE_MICROPHONE_CHANGED, roomId, session.getId(), toParticipantResponse(participant));
    }

    @Transactional
    public void forceMute(User actor, UUID roomId, UUID targetUserId) {
        RoomRole actorRole = requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession session = getActiveSessionOrThrow(roomId);
        LiveParticipant target = findParticipantOrThrow(session.getId(), targetUserId);
        // Antes tenía su propio chequeo inline ("solo el OWNER puede mutear al HOST"), distinto
        // del que usan demoteParticipant/removeParticipant (requireCanModerateLiveParticipant,
        // que rechaza moderar al HOST sin excepción). Dos reglas distintas para "moderar a
        // alguien en el LIVE" es la asimetría real que quedó pendiente de la auditoría de
        // permisos — unificado acá a la misma función que ya usan las otras dos acciones.
        requireCanModerateLiveParticipant(actorRole, target);
        target.setMicrophoneEnabled(false);
        liveParticipantRepository.save(target);
        publish(LiveEventType.CHAT_LIVE_MICROPHONE_CHANGED, roomId, session.getId(), toParticipantResponse(target));
    }

    // ---- helpers -----------------------------------------------------------------------------

    private LiveParticipantRole defaultRoleFor(UUID roomId, User me) {
        RoomMember membership = roomMemberRepository.findByRoomIdAndUserId(roomId, me.getId()).orElse(null);
        if (membership == null) {
            return LiveParticipantRole.AUDIENCE;
        }
        return switch (membership.getRole()) {
            case OWNER -> LiveParticipantRole.HOST;
            case CO_HOST -> LiveParticipantRole.CO_HOST;
            case MEMBER -> LiveParticipantRole.AUDIENCE;
        };
    }

    private void recomputeCounts(ChatLiveSession session) {
        long total = liveParticipantRepository.countByLiveSessionIdAndStatus(session.getId(), LiveParticipantStatus.ACTIVE);
        long speakers = liveParticipantRepository.countByLiveSessionIdAndStatusAndRoleIn(
                session.getId(), LiveParticipantStatus.ACTIVE, SPEAKING_ROLES);
        session.setParticipantCount((int) total);
        session.setSpeakerCount((int) speakers);
    }

    private LiveParticipant findParticipantOrThrow(UUID liveSessionId, UUID userId) {
        return liveParticipantRepository.findByLiveSessionIdAndUserId(liveSessionId, userId)
                .orElseThrow(() -> new NotFoundException("Ese usuario no está en el LIVE"));
    }

    /** Nadie salvo el OWNER puede tocar al HOST; un CO_HOST tampoco puede tocar a otro CO_HOST —
     * mismo principio que ChatService.requireCanModerate, aplicado a roles de LIVE. */
    private void requireCanModerateLiveParticipant(RoomRole actorRole, LiveParticipant target) {
        if (target.getRole() == LiveParticipantRole.HOST) {
            throw new ForbiddenException("No se puede moderar al anfitrión del LIVE");
        }
        if (actorRole == RoomRole.CO_HOST && target.getRole() == LiveParticipantRole.CO_HOST) {
            throw new ForbiddenException("Un coanfitrión no puede moderar a otro coanfitrión");
        }
    }

    private ChatRoom getRoomOrThrow(UUID roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Sala no encontrada"));
    }

    private ChatLiveSession getActiveSessionOrThrow(UUID roomId) {
        return chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("No hay un LIVE activo en esta sala"));
    }

    /** Antes rechazaba con 403 a cualquiera que no fuera YA miembro de la sala — pero el
     * carrusel de "en vivo" (ver ChatService.listLiveRooms) lista TODAS las salas públicas en
     * vivo, sin filtrar por membresía, justo para que se puedan descubrir salas ajenas. Eso
     * dejaba a cualquiera que tocara un LIVE ajeno desde ahí completamente bloqueado, sin
     * ninguna forma de entrar desde la UI ("no pudimos conectar al LIVE" sin explicación).
     * Unirse a un LIVE de una sala PÚBLICA ahora se auto-une como miembro, exactamente el mismo
     * comportamiento que ya tiene ChatService.sendMessage al mandar el primer mensaje — solo las
     * salas DIRECT (donde la membresía queda fija a los dos participantes desde que se crean)
     * siguen exigiendo ser miembro de antes. */
    private void requireMember(UUID roomId, User me) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            return;
        }
        if (room.getType() == RoomType.DIRECT) {
            throw new ForbiddenException("No tienes acceso a esta sala");
        }
        roomMemberRepository.save(new RoomMember(roomId, me.getId()));
    }

    private RoomRole requireOwnerOrCoHost(UUID roomId, User actor) {
        RoomMember membership = roomMemberRepository.findByRoomIdAndUserId(roomId, actor.getId())
                .orElseThrow(() -> new ForbiddenException("No tienes acceso a esta sala"));
        if (membership.getRole() == RoomRole.MEMBER) {
            throw new ForbiddenException("Solo el anfitrión o un coanfitrión pueden hacer esto");
        }
        return membership.getRole();
    }

    private LiveSessionResponse toSessionResponse(ChatLiveSession session, User viewer) {
        LiveParticipant mine = viewer != null
                ? liveParticipantRepository.findByLiveSessionIdAndUserId(session.getId(), viewer.getId()).orElse(null)
                : null;
        boolean activeMine = mine != null && mine.getStatus() == LiveParticipantStatus.ACTIVE;
        return new LiveSessionResponse(
                session.getId(),
                session.getRoomId(),
                session.getType().name(),
                session.getStatus().name(),
                session.getTitle(),
                session.getDescription(),
                session.getAnnouncement(),
                session.getStartedByUserId(),
                session.getStartedAt(),
                session.getParticipantCount(),
                session.getSpeakerCount(),
                session.getAgoraChannelName(),
                activeMine ? mine.getRole().name() : null,
                activeMine && mine.isMicrophoneEnabled(),
                activeMine && mine.getRole() == LiveParticipantRole.REQUESTED);
    }

    private LiveParticipantResponse toParticipantResponse(LiveParticipant participant) {
        UserSummary user = userRepository.findById(participant.getUserId()).map(profileMapper::toSummary).orElse(null);
        return new LiveParticipantResponse(
                user,
                participant.getRole().name(),
                participant.isMicrophoneEnabled(),
                participant.getRequestedToSpeakAt(),
                participant.getJoinedAt());
    }

    /** Publica un evento de dominio, no el mensaje STOMP directo — ver {@link LiveEventRelay},
     * que lo recoge con @TransactionalEventListener(AFTER_COMMIT). Mismo fix que
     * MusicService.publish (ver comentario ahí): FORCE_MUTE/KICK/DEMOTED/LIVE_ENDED, etc.
     * saliendo antes del commit tenían el mismo riesgo de que el cliente destino reaccionara (o
     * hiciera un GET) contra un estado que Postgres todavía no había confirmado. */
    private void publish(LiveEventType type, UUID roomId, UUID liveSessionId, Object payload) {
        eventPublisher.publishEvent(LiveEvent.of(type, roomId, liveSessionId, payload));
    }
}
