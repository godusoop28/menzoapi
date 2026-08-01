package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.menzo.menzo.domain.chat.ChatLiveSession;
import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.LiveMusicQueueItem;
import com.menzo.menzo.domain.chat.LiveMusicSession;
import com.menzo.menzo.domain.chat.LiveSessionStatus;
import com.menzo.menzo.domain.chat.QueueItemStatus;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.chat.RoomRole;
import com.menzo.menzo.domain.user.Aura;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.music.MusicSessionResponse;
import com.menzo.menzo.dto.music.VersionedRequest;
import com.menzo.menzo.exception.ApiException;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.repository.chat.ChatLiveSessionRepository;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.LiveMusicQueueItemRepository;
import com.menzo.menzo.repository.chat.LiveMusicSessionRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.UserRepository;

/**
 * Cubre reglas de negocio de Menzi DJ que Fase 21 pedía explícitamente y que
 * MusicServiceAfterCommitTest no toca (ese se enfoca solo en el timing AFTER_COMMIT): posición
 * conservada en pause/resume, versión avanzando en seek, optimistic locking real (columna
 * @Version de Hibernate, no un contador manual) y permisos por rol. Corre contra el mismo
 * Postgres real que el resto de la suite — @MockitoBean solo reemplaza el envío STOMP en sí, no
 * la base de datos.
 */
@SpringBootTest
class MusicServiceBehaviorTest {

    @Autowired private MusicService musicService;
    @Autowired private AuraRepository auraRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private RoomMemberRepository roomMemberRepository;
    @Autowired private ChatLiveSessionRepository chatLiveSessionRepository;
    @Autowired private LiveMusicSessionRepository musicSessionRepository;
    @Autowired private LiveMusicQueueItemRepository queueItemRepository;

    @MockitoBean private SimpMessagingTemplate messagingTemplate;

    private UUID roomId;
    private User owner;
    private User coHost;
    private User member;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Aura aura = auraRepository.findById("fire").orElseGet(() -> {
            Aura a = new Aura();
            a.setId("fire");
            a.setName("Fire");
            a.setDescription("Fixture aura for tests");
            a.setGradient("fire");
            return auraRepository.save(a);
        });

        owner = createUser(aura, "owner-" + suffix);
        coHost = createUser(aura, "cohost-" + suffix);
        member = createUser(aura, "member-" + suffix);

        ChatRoom room = new ChatRoom();
        room.setSlug("music-behavior-" + suffix);
        room.setName("Music behavior room " + suffix);
        room = chatRoomRepository.save(room);
        roomId = room.getId();

        roomMemberRepository.save(new RoomMember(roomId, owner.getId(), RoomRole.OWNER));
        roomMemberRepository.save(new RoomMember(roomId, coHost.getId(), RoomRole.CO_HOST));
        roomMemberRepository.save(new RoomMember(roomId, member.getId(), RoomRole.MEMBER));

        ChatLiveSession live = new ChatLiveSession();
        live.setRoomId(roomId);
        live.setStatus(LiveSessionStatus.ACTIVE);
        live.setStartedByUserId(owner.getId());
        live.setStartedAt(Instant.now());
        live.setLastHeartbeatAt(Instant.now());
        chatLiveSessionRepository.save(live);
    }

    private User createUser(Aura aura, String tag) {
        User user = new User();
        user.setEmail(tag + "@test.menzo");
        user.setUsername(tag.replace("-", ""));
        user.setPasswordHash("x");
        user.setDisplayName(tag);
        user.setAura(aura);
        user.setJoinedAt(Instant.now());
        return userRepository.save(user);
    }

    private void queueTrack(int durationSeconds) {
        LiveMusicSession session = musicSessionRepository.findByLiveSessionId(
                chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE).orElseThrow().getId())
                .orElseGet(() -> {
                    LiveMusicSession created = new LiveMusicSession();
                    created.setLiveSessionId(
                            chatLiveSessionRepository.findByRoomIdAndStatus(roomId, LiveSessionStatus.ACTIVE).orElseThrow().getId());
                    created.setRoomId(roomId);
                    return musicSessionRepository.saveAndFlush(created);
                });
        LiveMusicQueueItem item = new LiveMusicQueueItem();
        item.setMusicSessionId(session.getId());
        item.setVideoId("dQw4w9WgXcQ");
        item.setTitle("Test track");
        item.setDurationSeconds(durationSeconds);
        item.setStatus(QueueItemStatus.QUEUED);
        item.setPosition(0);
        queueItemRepository.save(item);
    }

    @Test
    void pauseConservesPositionAndResumeContinuesFromThere() throws InterruptedException {
        queueTrack(300);
        musicService.play(roomId, owner, null);
        Thread.sleep(1100); // deja pasar >=1s real de reproducción antes de pausar

        MusicSessionResponse paused = musicService.pause(roomId, owner, null);
        assertThat(paused.status()).isEqualTo("PAUSED");
        assertThat(paused.positionSeconds()).isGreaterThanOrEqualTo(1);
        int pausedPosition = paused.positionSeconds();

        // Mientras sigue en pausa, la posición no debe seguir avanzando sola.
        Thread.sleep(500);
        MusicSessionResponse stillPaused = musicService.getSnapshot(roomId, owner);
        assertThat(stillPaused.positionSeconds()).isEqualTo(pausedPosition);

        MusicSessionResponse resumed = musicService.resume(roomId, owner, null);
        assertThat(resumed.status()).isEqualTo("PLAYING");
        assertThat(resumed.positionSeconds()).isEqualTo(pausedPosition);
    }

    @Test
    void seekUpdatesPositionAndAdvancesVersion() {
        queueTrack(300);
        MusicSessionResponse started = musicService.play(roomId, owner, null);
        long versionBeforeSeek = started.version();

        MusicSessionResponse sought = musicService.seek(
                roomId, owner, new com.menzo.menzo.dto.music.SeekRequest(120, versionBeforeSeek));
        assertThat(sought.positionSeconds()).isEqualTo(120);
        assertThat(sought.version()).isGreaterThan(versionBeforeSeek);
    }

    @Test
    void concurrentVersionMismatchIsRejectedWithConflict() {
        queueTrack(300);
        MusicSessionResponse started = musicService.play(roomId, owner, null);
        long staleVersion = started.version();

        // Alguien más pausa primero (la versión real en base avanza).
        musicService.pause(roomId, owner, new VersionedRequest(staleVersion));

        // Un segundo comando que todavía cree que la versión es la vieja debe rechazarse, no
        // pisar en silencio el cambio del primero.
        assertThatThrownBy(() -> musicService.resume(roomId, owner, new VersionedRequest(staleVersion)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void nonMemberCannotReadSnapshot() {
        User outsider = createUser(
                auraRepository.findById("fire").orElseThrow(),
                "outsider-" + UUID.randomUUID().toString().substring(0, 8));
        queueTrack(300);
        musicService.play(roomId, owner, null);

        assertThatThrownBy(() -> musicService.getSnapshot(roomId, outsider))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
    }

    @Test
    void memberCannotControlPlaybackButCoHostCan() {
        queueTrack(300);
        musicService.play(roomId, owner, null);

        // MusicService.requireOwnerOrCoHost tira un ApiException(FORBIDDEN) genérico, no el
        // subtipo ForbiddenException (a diferencia de LiveService) — lo que importa para el
        // contrato con el cliente es el status 403, no la clase Java exacta.
        assertThatThrownBy(() -> musicService.pause(roomId, member, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));

        // CO_HOST sí puede — no debe tirar ninguna excepción de permisos.
        MusicSessionResponse paused = musicService.pause(roomId, coHost, null);
        assertThat(paused.status()).isEqualTo("PAUSED");
    }

    @Test
    void lateJoinerSeesPositionAlreadyAdvanced() throws InterruptedException {
        queueTrack(300);
        musicService.play(roomId, owner, null);
        Thread.sleep(1500);

        // "Entrada tardía": un MEMBER que nunca vio MUSIC_STARTED simplemente hace GET — la
        // posición canónica ya refleja el tiempo transcurrido, sin depender de haber estado
        // presente cuando arrancó.
        MusicSessionResponse snapshot = musicService.getSnapshot(roomId, member);
        assertThat(snapshot.status()).isEqualTo("PLAYING");
        assertThat(snapshot.positionSeconds()).isGreaterThanOrEqualTo(1);
    }
}
