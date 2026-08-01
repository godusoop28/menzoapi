package com.menzo.menzo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.ChatLiveSession;
import com.menzo.menzo.domain.chat.LiveMusicQueueItem;
import com.menzo.menzo.domain.chat.LiveMusicSession;
import com.menzo.menzo.domain.chat.LiveSessionStatus;
import com.menzo.menzo.domain.chat.MusicSessionStatus;
import com.menzo.menzo.domain.chat.QueueItemStatus;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.chat.RoomRole;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.music.AddQueueItemRequest;
import com.menzo.menzo.dto.music.MusicEvent;
import com.menzo.menzo.dto.music.MusicEventType;
import com.menzo.menzo.dto.music.MusicSessionResponse;
import com.menzo.menzo.dto.music.MusicSettingsRequest;
import com.menzo.menzo.dto.music.QueueItemResponse;
import com.menzo.menzo.dto.music.ReorderQueueRequest;
import com.menzo.menzo.dto.music.RequestSongRequest;
import com.menzo.menzo.dto.music.SeekRequest;
import com.menzo.menzo.dto.music.VersionedRequest;
import com.menzo.menzo.dto.music.YoutubeSearchResult;
import com.menzo.menzo.dto.user.UserSummary;
import com.menzo.menzo.exception.ApiException;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.ChatLiveSessionRepository;
import com.menzo.menzo.repository.chat.LiveMusicQueueItemRepository;
import com.menzo.menzo.repository.chat.LiveMusicSessionRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;

/**
 * Menzi DJ: no es un usuario ni un participante de Agora — es este servicio, que mantiene el
 * estado canónico de "qué video, en qué momento, en qué estado" para una sala en vivo. Agora
 * sigue transportando únicamente las voces; la música la reproduce el YouTube IFrame Player de
 * cada dispositivo, sincronizado contra este estado (ver MusicSessionResponse.positionSeconds,
 * ya calculado, y los eventos LIVE_MUSIC_* publicados en /topic/rooms/{roomId}/music).
 */
@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    private static final Set<QueueItemStatus> STAGE_HISTORY_STATUSES =
            Set.of(QueueItemStatus.PLAYED, QueueItemStatus.SKIPPED);

    private final ChatLiveSessionRepository chatLiveSessionRepository;
    private final LiveMusicSessionRepository musicSessionRepository;
    private final LiveMusicQueueItemRepository queueItemRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final YoutubeClient youtubeClient;
    private final ApplicationEventPublisher eventPublisher;

    public MusicService(
            ChatLiveSessionRepository chatLiveSessionRepository,
            LiveMusicSessionRepository musicSessionRepository,
            LiveMusicQueueItemRepository queueItemRepository,
            RoomMemberRepository roomMemberRepository,
            UserRepository userRepository,
            ProfileMapper profileMapper,
            YoutubeClient youtubeClient,
            ApplicationEventPublisher eventPublisher) {
        this.chatLiveSessionRepository = chatLiveSessionRepository;
        this.musicSessionRepository = musicSessionRepository;
        this.queueItemRepository = queueItemRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.profileMapper = profileMapper;
        this.youtubeClient = youtubeClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MusicSessionResponse getSnapshot(UUID roomId, User viewer) {
        requireMember(roomId, viewer);
        ChatLiveSession liveSession = getActiveLiveSessionOrThrow(roomId);
        LiveMusicSession session = getOrCreateMusicSession(liveSession);
        return toSnapshot(session);
    }

    /** No es @Transactional: la llamada a YouTube es una petición HTTP saliente que puede tardar
     * varios segundos, y no hace ninguna escritura — mantenerla dentro de una transacción solo
     * retendría una conexión de la pool sin necesidad (ver sección 6 del pedido sobre no dejar
     * consultas/recursos abiertos más de lo necesario). */
    public List<YoutubeSearchResult> search(UUID roomId, User actor, String q) {
        requireMember(roomId, actor);
        getActiveLiveSessionOrThrow(roomId);
        log.info("Menzi DJ search started: roomId={}, userId={}, query={}", roomId, actor.getId(), q);
        try {
            List<YoutubeSearchResult> results = youtubeClient.search(actor.getId(), q);
            log.info("Menzi DJ search succeeded: roomId={}, userId={}, resultCount={}", roomId, actor.getId(), results.size());
            return results;
        } catch (ApiException ex) {
            // Ya es un error controlado (YoutubeClient.mapError) — se relanza tal cual para que
            // GlobalExceptionHandler lo traduzca al status/code correctos, solo se deja rastro acá.
            log.warn("Menzi DJ search failed (controlled): roomId={}, userId={}, code={}, status={}",
                    roomId, actor.getId(), ex.getCode(), ex.getStatus());
            throw ex;
        } catch (RuntimeException ex) {
            // Cualquier otra cosa no anticipada (bug de parseo, NPE, lo que sea) nunca debe llegar
            // al cliente como un 500 desnudo — se loguea completo (clase + stack trace) para poder
            // diagnosticarlo, y se traduce a un error de Menzi DJ legible en vez de "Internal Server Error".
            log.error("Menzi DJ search failed: roomId={}, userId={}, errorType={}",
                    roomId, actor.getId(), ex.getClass().getSimpleName(), ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_UNAVAILABLE",
                    "Menzi DJ no pudo buscar música en este momento.");
        }
    }

    @Transactional(readOnly = true)
    public List<QueueItemResponse> listQueue(UUID roomId, User viewer) {
        requireMember(roomId, viewer);
        ChatLiveSession liveSession = getActiveLiveSessionOrThrow(roomId);
        LiveMusicSession session = getOrCreateMusicSession(liveSession);
        return queueItemRepository.findByMusicSessionIdAndStatusOrderByPositionAsc(session.getId(), QueueItemStatus.QUEUED)
                .stream()
                .map(this::toQueueItemResponse)
                .toList();
    }

    @Transactional
    public MusicSessionResponse addToQueue(UUID roomId, User actor, AddQueueItemRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession liveSession = getActiveLiveSessionOrThrow(roomId);
        LiveMusicSession session = getOrCreateMusicSession(liveSession);
        checkVersion(session, request.expectedVersion());

        YoutubeSearchResult video = youtubeClient.getVideo(request.videoId());
        LiveMusicQueueItem item = createQueueItem(session, video, null, null, QueueItemStatus.QUEUED);

        boolean playNow = Boolean.TRUE.equals(request.playNow())
                || session.getStatus() == MusicSessionStatus.IDLE
                || session.getStatus() == MusicSessionStatus.STOPPED;

        if (playNow) {
            startPlaying(session, item, actor.getId());
            session = musicSessionRepository.saveAndFlush(session);
            publish(MusicEventType.LIVE_MUSIC_TRACK_ADDED, session, toQueueItemResponse(item));
            publish(MusicEventType.LIVE_MUSIC_STARTED, session, toSnapshot(session));
        } else {
            queueItemRepository.save(item);
            publish(MusicEventType.LIVE_MUSIC_TRACK_ADDED, session, toQueueItemResponse(item));
            publish(MusicEventType.LIVE_MUSIC_QUEUE_UPDATED, session, null);
        }
        return toSnapshot(session);
    }

    @Transactional
    public void requestSong(UUID roomId, User actor, RequestSongRequest request) {
        requireMember(roomId, actor);
        ChatLiveSession liveSession = getActiveLiveSessionOrThrow(roomId);
        LiveMusicSession session = getOrCreateMusicSession(liveSession);
        if (!session.isAllowRequests()) {
            throw new ForbiddenException("El anfitrión desactivó las solicitudes de canciones.");
        }

        YoutubeSearchResult video = youtubeClient.getVideo(request.videoId());
        LiveMusicQueueItem item = createQueueItem(session, video, actor.getId(), null, QueueItemStatus.PENDING);
        publish(MusicEventType.LIVE_MUSIC_REQUESTED, session, toQueueItemResponse(item));
    }

    @Transactional
    public void approveRequest(UUID roomId, User actor, UUID requestId) {
        RoomRole ignored = requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession liveSession = getActiveLiveSessionOrThrow(roomId);
        LiveMusicSession session = getOrCreateMusicSession(liveSession);
        LiveMusicQueueItem item = findQueueItemOrThrow(session.getId(), requestId);
        if (item.getStatus() != QueueItemStatus.PENDING) {
            throw new BadRequestException("Esa solicitud ya no está pendiente.");
        }
        item.setStatus(QueueItemStatus.QUEUED);
        item.setApprovedByUserId(actor.getId());
        item.setPosition(nextQueuePosition(session.getId()));
        queueItemRepository.save(item);
        publish(MusicEventType.LIVE_MUSIC_REQUEST_APPROVED, session, toQueueItemResponse(item));
        publish(MusicEventType.LIVE_MUSIC_QUEUE_UPDATED, session, null);
    }

    @Transactional
    public void rejectRequest(UUID roomId, User actor, UUID requestId) {
        requireOwnerOrCoHost(roomId, actor);
        ChatLiveSession liveSession = getActiveLiveSessionOrThrow(roomId);
        LiveMusicSession session = getOrCreateMusicSession(liveSession);
        LiveMusicQueueItem item = findQueueItemOrThrow(session.getId(), requestId);
        if (item.getStatus() != QueueItemStatus.PENDING) {
            throw new BadRequestException("Esa solicitud ya no está pendiente.");
        }
        item.setStatus(QueueItemStatus.REJECTED);
        queueItemRepository.save(item);
        publish(MusicEventType.LIVE_MUSIC_REQUEST_REJECTED, session, toQueueItemResponse(item));
    }

    @Transactional
    public MusicSessionResponse play(UUID roomId, User actor, VersionedRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        checkVersion(session, request != null ? request.expectedVersion() : null);

        if (session.getStatus() == MusicSessionStatus.PAUSED) {
            session.setPlaybackStartedAt(Instant.now());
            session.setPausedAt(null);
            session.setStatus(MusicSessionStatus.PLAYING);
            session.setControlledByUserId(actor.getId());
            session = musicSessionRepository.saveAndFlush(session);
            publish(MusicEventType.LIVE_MUSIC_RESUMED, session, toSnapshot(session));
            return toSnapshot(session);
        }

        if (session.getStatus() == MusicSessionStatus.PLAYING) {
            throw new BadRequestException("Ya se está reproduciendo música.");
        }

        LiveMusicQueueItem next = queueItemRepository
                .findFirstByMusicSessionIdAndStatusOrderByPositionAsc(session.getId(), QueueItemStatus.QUEUED)
                .orElseThrow(() -> new BadRequestException("La cola está vacía."));
        startPlaying(session, next, actor.getId());
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_STARTED, session, toSnapshot(session));
        return toSnapshot(session);
    }

    @Transactional
    public MusicSessionResponse pause(UUID roomId, User actor, VersionedRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        checkVersion(session, request != null ? request.expectedVersion() : null);
        if (session.getStatus() != MusicSessionStatus.PLAYING) {
            throw new BadRequestException("La música no se está reproduciendo.");
        }
        session.setPositionSeconds(computePosition(session));
        session.setPausedAt(Instant.now());
        session.setStatus(MusicSessionStatus.PAUSED);
        session.setControlledByUserId(actor.getId());
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_PAUSED, session, toSnapshot(session));
        return toSnapshot(session);
    }

    @Transactional
    public MusicSessionResponse resume(UUID roomId, User actor, VersionedRequest request) {
        return play(roomId, actor, request);
    }

    @Transactional
    public MusicSessionResponse seek(UUID roomId, User actor, SeekRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        checkVersion(session, request.expectedVersion());
        if (session.getStatus() != MusicSessionStatus.PLAYING && session.getStatus() != MusicSessionStatus.PAUSED) {
            throw new BadRequestException("No hay nada reproduciéndose para saltar de posición.");
        }
        int maxSeconds = session.getDurationSeconds() != null ? session.getDurationSeconds() : Integer.MAX_VALUE;
        if (request.positionSeconds() < 0 || request.positionSeconds() > maxSeconds) {
            throw new BadRequestException("Esa posición está fuera de la duración de la canción.");
        }
        session.setPositionSeconds(request.positionSeconds());
        if (session.getStatus() == MusicSessionStatus.PLAYING) {
            session.setPlaybackStartedAt(Instant.now());
        }
        session.setControlledByUserId(actor.getId());
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_SEEKED, session, toSnapshot(session));
        return toSnapshot(session);
    }

    @Transactional
    public MusicSessionResponse skip(UUID roomId, User actor, VersionedRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        checkVersion(session, request != null ? request.expectedVersion() : null);
        endCurrentItem(session, QueueItemStatus.SKIPPED);

        LiveMusicQueueItem next = queueItemRepository
                .findFirstByMusicSessionIdAndStatusOrderByPositionAsc(session.getId(), QueueItemStatus.QUEUED)
                .orElse(null);
        if (next != null) {
            startPlaying(session, next, actor.getId());
        } else {
            clearCurrent(session);
            session.setStatus(MusicSessionStatus.STOPPED);
        }
        session.setControlledByUserId(actor.getId());
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_SKIPPED, session, toSnapshot(session));
        return toSnapshot(session);
    }

    @Transactional
    public MusicSessionResponse stop(UUID roomId, User actor, VersionedRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        checkVersion(session, request != null ? request.expectedVersion() : null);
        endCurrentItem(session, QueueItemStatus.SKIPPED);
        clearCurrent(session);
        session.setStatus(MusicSessionStatus.STOPPED);
        session.setControlledByUserId(actor.getId());
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_STOPPED, session, toSnapshot(session));
        return toSnapshot(session);
    }

    @Transactional
    public MusicSessionResponse updateSettings(UUID roomId, User actor, MusicSettingsRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        if (request.allowRequests() != null) {
            session.setAllowRequests(request.allowRequests());
        }
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_SETTINGS_UPDATED, session, toSnapshot(session));
        return toSnapshot(session);
    }

    @Transactional
    public MusicSessionResponse reorderQueue(UUID roomId, User actor, ReorderQueueRequest request) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        checkVersion(session, request.expectedVersion());

        List<LiveMusicQueueItem> current =
                queueItemRepository.findByMusicSessionIdAndStatusOrderByPositionAsc(session.getId(), QueueItemStatus.QUEUED);
        Set<UUID> currentIds = current.stream().map(LiveMusicQueueItem::getId).collect(java.util.stream.Collectors.toSet());
        List<UUID> requestedIds = request.orderedQueueItemIds();
        if (requestedIds == null || requestedIds.size() != currentIds.size() || !currentIds.containsAll(requestedIds)) {
            throw new BadRequestException("El nuevo orden no coincide con la cola actual.");
        }

        int position = 0;
        for (UUID id : requestedIds) {
            LiveMusicQueueItem item = current.stream().filter(i -> i.getId().equals(id)).findFirst().orElseThrow();
            item.setPosition(position++);
            queueItemRepository.save(item);
        }
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_QUEUE_UPDATED, session, null);
        return toSnapshot(session);
    }

    @Transactional
    public void removeQueueItem(UUID roomId, User actor, UUID queueItemId) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        LiveMusicQueueItem item = findQueueItemOrThrow(session.getId(), queueItemId);
        if (item.getStatus() != QueueItemStatus.QUEUED) {
            throw new BadRequestException("Esa canción ya no está en la cola.");
        }
        item.setStatus(QueueItemStatus.REMOVED);
        item.setPosition(null);
        queueItemRepository.save(item);
        publish(MusicEventType.LIVE_MUSIC_QUEUE_UPDATED, session, null);
    }

    @Transactional
    public void clearQueue(UUID roomId, User actor) {
        requireOwnerOrCoHost(roomId, actor);
        LiveMusicSession session = getMusicSessionOrThrow(roomId);
        List<LiveMusicQueueItem> queued =
                queueItemRepository.findByMusicSessionIdAndStatusOrderByPositionAsc(session.getId(), QueueItemStatus.QUEUED);
        for (LiveMusicQueueItem item : queued) {
            item.setStatus(QueueItemStatus.REMOVED);
            item.setPosition(null);
        }
        queueItemRepository.saveAll(queued);
        publish(MusicEventType.LIVE_MUSIC_QUEUE_UPDATED, session, null);
    }

    // ---- avance automático (ver MusicAutoAdvanceScheduler) --------------------------------------

    /** Se llama desde el scheduler cuando la posición canónica de una sesión PLAYING alcanza su
     * duración — mismo efecto que un skip, pero el item termina como PLAYED (terminó solo) en vez
     * de SKIPPED (alguien lo cortó a propósito), y no hay "actor" real: se atribuye al que la iba
     * a controlar por última vez (controlledByUserId ya guardado). */
    @Transactional
    public void autoAdvance(UUID musicSessionId) {
        LiveMusicSession session = musicSessionRepository.findById(musicSessionId).orElse(null);
        if (session == null || session.getStatus() != MusicSessionStatus.PLAYING) {
            return;
        }
        endCurrentItem(session, QueueItemStatus.PLAYED);
        LiveMusicQueueItem next = queueItemRepository
                .findFirstByMusicSessionIdAndStatusOrderByPositionAsc(session.getId(), QueueItemStatus.QUEUED)
                .orElse(null);
        if (next != null) {
            startPlaying(session, next, session.getControlledByUserId());
        } else {
            clearCurrent(session);
            session.setStatus(MusicSessionStatus.STOPPED);
        }
        session = musicSessionRepository.saveAndFlush(session);
        publish(MusicEventType.LIVE_MUSIC_TRACK_CHANGED, session, toSnapshot(session));
    }

    /** Ids + posición canónica de todas las sesiones PLAYING que ya deberían haber terminado —
     * usado por el scheduler para no tener que traer entidades completas innecesariamente. */
    @Transactional(readOnly = true)
    public List<UUID> findSessionsNeedingAutoAdvance() {
        List<UUID> result = new ArrayList<>();
        // Antes era musicSessionRepository.findAll() + filtro en Java — traía la tabla completa
        // (incluidas sesiones IDLE/PAUSED/STOPPED de LIVEs ya terminados) cada 5s. Solo las
        // sesiones PLAYING pueden necesitar avanzar solas.
        for (LiveMusicSession session : musicSessionRepository.findByStatus(MusicSessionStatus.PLAYING)) {
            if (session.getDurationSeconds() != null && computePosition(session) >= session.getDurationSeconds()) {
                result.add(session.getId());
            }
        }
        return result;
    }

    // ---- helpers ------------------------------------------------------------------------------

    private LiveMusicQueueItem createQueueItem(
            LiveMusicSession session, YoutubeSearchResult video, UUID requestedBy, Integer position, QueueItemStatus status) {
        LiveMusicQueueItem item = new LiveMusicQueueItem();
        item.setMusicSessionId(session.getId());
        item.setVideoId(video.videoId());
        item.setTitle(video.title());
        item.setChannelTitle(video.channelTitle());
        item.setThumbnailUrl(video.thumbnailUrl());
        item.setDurationSeconds(video.durationSeconds());
        item.setRequestedByUserId(requestedBy);
        item.setStatus(status);
        item.setPosition(status == QueueItemStatus.QUEUED ? (position != null ? position : nextQueuePosition(session.getId())) : null);
        return queueItemRepository.save(item);
    }

    private int nextQueuePosition(UUID musicSessionId) {
        return queueItemRepository.findByMusicSessionIdAndStatusOrderByPositionAsc(musicSessionId, QueueItemStatus.QUEUED)
                .stream()
                .mapToInt(i -> i.getPosition() != null ? i.getPosition() : 0)
                .max()
                .orElse(-1) + 1;
    }

    private void startPlaying(LiveMusicSession session, LiveMusicQueueItem item, UUID actorId) {
        item.setStatus(QueueItemStatus.PLAYING);
        item.setStartedAt(Instant.now());
        item.setPosition(null);
        queueItemRepository.save(item);

        session.setStatus(MusicSessionStatus.PLAYING);
        session.setCurrentQueueItemId(item.getId());
        session.setCurrentVideoId(item.getVideoId());
        session.setCurrentTitle(item.getTitle());
        session.setCurrentChannelTitle(item.getChannelTitle());
        session.setCurrentThumbnailUrl(item.getThumbnailUrl());
        session.setDurationSeconds(item.getDurationSeconds());
        session.setPositionSeconds(0);
        session.setPlaybackStartedAt(Instant.now());
        session.setPausedAt(null);
        session.setControlledByUserId(actorId);
    }

    private void endCurrentItem(LiveMusicSession session, QueueItemStatus endStatus) {
        if (session.getCurrentQueueItemId() == null) {
            return;
        }
        queueItemRepository.findById(session.getCurrentQueueItemId()).ifPresent(item -> {
            if (item.getStatus() == QueueItemStatus.PLAYING) {
                item.setStatus(endStatus);
                item.setEndedAt(Instant.now());
                queueItemRepository.save(item);
            }
        });
    }

    private void clearCurrent(LiveMusicSession session) {
        session.setCurrentQueueItemId(null);
        session.setCurrentVideoId(null);
        session.setCurrentTitle(null);
        session.setCurrentChannelTitle(null);
        session.setCurrentThumbnailUrl(null);
        session.setDurationSeconds(null);
        session.setPositionSeconds(0);
        session.setPlaybackStartedAt(null);
        session.setPausedAt(null);
    }

    /** Posición real "ahora" — la única fuente de verdad para cualquier cliente que necesite
     * saber dónde va la canción, sin confiar en su propio reloj (ver sección 10 del pedido). */
    private int computePosition(LiveMusicSession session) {
        if (session.getStatus() == MusicSessionStatus.PLAYING && session.getPlaybackStartedAt() != null) {
            long elapsed = Duration.between(session.getPlaybackStartedAt(), Instant.now()).getSeconds();
            long raw = session.getPositionSeconds() + elapsed;
            if (session.getDurationSeconds() != null) {
                raw = Math.min(raw, session.getDurationSeconds());
            }
            return (int) Math.max(0, raw);
        }
        return session.getPositionSeconds();
    }

    private void checkVersion(LiveMusicSession session, Long expectedVersion) {
        if (expectedVersion != null && expectedVersion != session.getVersion()) {
            throw new ConflictException("Menzi DJ cambió mientras tanto — actualizá e intentá de nuevo.");
        }
    }

    private LiveMusicSession getOrCreateMusicSession(ChatLiveSession liveSession) {
        return musicSessionRepository.findByLiveSessionId(liveSession.getId()).orElseGet(() -> {
            LiveMusicSession created = new LiveMusicSession();
            created.setLiveSessionId(liveSession.getId());
            created.setRoomId(liveSession.getRoomId());
            LiveMusicSession saved = musicSessionRepository.saveAndFlush(created);
            publish(MusicEventType.LIVE_MUSIC_SESSION_CREATED, saved, toSnapshot(saved));
            return saved;
        });
    }

    private LiveMusicSession getMusicSessionOrThrow(UUID roomId) {
        ChatLiveSession liveSession = getActiveLiveSessionOrThrow(roomId);
        return getOrCreateMusicSession(liveSession);
    }

    private ChatLiveSession getActiveLiveSessionOrThrow(UUID roomId) {
        return chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "LIVE_NOT_ACTIVE", "No hay un LIVE activo en esta sala"));
    }

    private LiveMusicQueueItem findQueueItemOrThrow(UUID musicSessionId, UUID queueItemId) {
        return queueItemRepository.findByIdAndMusicSessionId(queueItemId, musicSessionId)
                .orElseThrow(() -> new NotFoundException("Esa canción no está en Menzi DJ"));
    }

    private void requireMember(UUID roomId, User me) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes acceso a esta sala");
        }
    }

    private RoomRole requireOwnerOrCoHost(UUID roomId, User actor) {
        RoomMember membership = roomMemberRepository.findByRoomIdAndUserId(roomId, actor.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes acceso a esta sala"));
        if (membership.getRole() == RoomRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Solo el anfitrión o un coanfitrión pueden controlar Menzi DJ");
        }
        return membership.getRole();
    }

    /** Publica un evento de dominio (Spring ApplicationEvent), nunca el mensaje STOMP directo.
     * {@link MusicEventRelay} lo recoge con @TransactionalEventListener(AFTER_COMMIT): si este
     * método corriera messagingTemplate.convertAndSend acá adentro, otro dispositivo podría recibir
     * el WebSocket, hacer un GET de reconciliación y llegar antes de que el commit de esta
     * transacción sea visible — vería el estado viejo (por eso "solo el anfitrión escuchaba": su
     * propia respuesta REST sí reflejaba el cambio porque lo escribió él mismo, pero los demás
     * consultaban desde otra conexión/transacción que aún no veía el commit). El snapshot se arma
     * ahora, con los valores ya aplicados en memoria — session/item vienen de un saveAndFlush, así
     * que version/id ya son los finales; si la transacción hace rollback después, el evento nunca
     * se entrega porque el listener AFTER_COMMIT simplemente no se dispara. */
    private void publish(MusicEventType type, LiveMusicSession session, Object payload) {
        eventPublisher.publishEvent(
                MusicEvent.of(type, session.getRoomId(), session.getLiveSessionId(), session.getId(), session.getVersion(), payload));
    }

    private MusicSessionResponse toSnapshot(LiveMusicSession session) {
        List<QueueItemResponse> queue = queueItemRepository
                .findByMusicSessionIdAndStatusOrderByPositionAsc(session.getId(), QueueItemStatus.QUEUED)
                .stream()
                .map(this::toQueueItemResponse)
                .toList();
        List<QueueItemResponse> pending = queueItemRepository
                .findByMusicSessionIdAndStatusOrderByCreatedAtAsc(session.getId(), QueueItemStatus.PENDING)
                .stream()
                .map(this::toQueueItemResponse)
                .toList();
        List<QueueItemResponse> history = queueItemRepository
                .findTop20ByMusicSessionIdAndStatusInOrderByCreatedAtDesc(session.getId(), List.copyOf(STAGE_HISTORY_STATUSES))
                .stream()
                .map(this::toQueueItemResponse)
                .toList();
        return new MusicSessionResponse(
                session.getId(),
                session.getRoomId(),
                session.getLiveSessionId(),
                session.getStatus().name(),
                session.getCurrentQueueItemId(),
                session.getCurrentVideoId(),
                session.getCurrentTitle(),
                session.getCurrentChannelTitle(),
                session.getCurrentThumbnailUrl(),
                session.getDurationSeconds(),
                computePosition(session),
                session.isAllowRequests(),
                session.getVersion(),
                queue,
                pending,
                history);
    }

    private QueueItemResponse toQueueItemResponse(LiveMusicQueueItem item) {
        UserSummary requestedBy = item.getRequestedByUserId() != null
                ? userRepository.findById(item.getRequestedByUserId()).map(profileMapper::toSummary).orElse(null)
                : null;
        UserSummary approvedBy = item.getApprovedByUserId() != null
                ? userRepository.findById(item.getApprovedByUserId()).map(profileMapper::toSummary).orElse(null)
                : null;
        return new QueueItemResponse(
                item.getId(),
                item.getVideoId(),
                item.getTitle(),
                item.getChannelTitle(),
                item.getThumbnailUrl(),
                item.getDurationSeconds(),
                requestedBy,
                approvedBy,
                item.getPosition(),
                item.getStatus().name(),
                item.getCreatedAt());
    }
}
