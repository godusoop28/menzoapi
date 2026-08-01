package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.menzo.menzo.domain.chat.ChatLiveSession;
import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.LiveMusicQueueItem;
import com.menzo.menzo.domain.chat.LiveMusicSession;
import com.menzo.menzo.domain.chat.LiveSessionStatus;
import com.menzo.menzo.domain.chat.MusicSessionStatus;
import com.menzo.menzo.domain.chat.QueueItemStatus;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.chat.RoomRole;
import com.menzo.menzo.domain.user.Aura;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.music.MusicEvent;
import com.menzo.menzo.dto.music.MusicEventType;
import com.menzo.menzo.repository.chat.ChatLiveSessionRepository;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.LiveMusicQueueItemRepository;
import com.menzo.menzo.repository.chat.LiveMusicSessionRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.UserRepository;

/**
 * Prueba de integración contra el Postgres real del entorno (no mockea repositorios) que
 * reproduce exactamente el bug reportado: "el anfitrión escucha, los demás no".
 *
 * Antes del fix, MusicService.publish() llamaba a SimpMessagingTemplate.convertAndSend()
 * directamente dentro del método @Transactional — el mensaje STOMP podía salir, y un segundo
 * dispositivo reaccionar con un GET, antes de que esta misma transacción hiciera commit. Ese GET
 * corría en otra conexión/transacción que todavía no veía el UPDATE, así que devolvía el estado
 * viejo. El propio anfitrión no notaba nada raro porque su respuesta REST synchronous sí reflejaba
 * el cambio (la escribió él mismo, en la misma transacción).
 *
 * Con el fix, MusicService.publish() solo emite un ApplicationEvent; MusicEventRelay lo recoge
 * con @TransactionalEventListener(phase = AFTER_COMMIT). Estas dos pruebas verifican el contrato
 * exacto pedido: (1) el relay no corre mientras la transacción sigue abierta, y sí corre una sola
 * vez apenas se confirma, con la version ya committeada; (2) un rollback no emite ningún evento.
 */
@SpringBootTest
class MusicServiceAfterCommitTest {

    @Autowired private MusicService musicService;
    @Autowired private PlatformTransactionManager transactionManager;
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
    private LiveMusicSession session;

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

        owner = new User();
        owner.setEmail("aftercommit-" + suffix + "@test.menzo");
        owner.setUsername("aftercommit" + suffix);
        owner.setPasswordHash("x");
        owner.setDisplayName("After Commit Test");
        owner.setAura(aura);
        owner.setJoinedAt(Instant.now());
        owner = userRepository.save(owner);

        ChatRoom room = new ChatRoom();
        room.setSlug("aftercommit-" + suffix);
        room.setName("After commit room " + suffix);
        room = chatRoomRepository.save(room);
        roomId = room.getId();

        roomMemberRepository.save(new RoomMember(roomId, owner.getId(), RoomRole.OWNER));

        ChatLiveSession live = new ChatLiveSession();
        live.setRoomId(roomId);
        live.setStatus(LiveSessionStatus.ACTIVE);
        live.setStartedByUserId(owner.getId());
        live.setStartedAt(Instant.now());
        live.setLastHeartbeatAt(Instant.now());
        live = chatLiveSessionRepository.save(live);

        session = new LiveMusicSession();
        session.setLiveSessionId(live.getId());
        session.setRoomId(roomId);
        session = musicSessionRepository.saveAndFlush(session);

        LiveMusicQueueItem item = new LiveMusicQueueItem();
        item.setMusicSessionId(session.getId());
        item.setVideoId("dQw4w9WgXcQ");
        item.setTitle("Test track");
        item.setDurationSeconds(180);
        item.setStatus(QueueItemStatus.QUEUED);
        item.setPosition(0);
        queueItemRepository.save(item);

        reset(messagingTemplate);
    }

    @Test
    void doesNotPublishBeforeCommit_andPublishesExactlyOnceAfterCommit() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.execute(status -> {
            musicService.play(roomId, owner, null);
            // Todavía dentro de la transacción de MusicService.play(): si el bug estuviera
            // presente, convertAndSend ya se habría llamado acá. Con el fix, cero interacciones.
            verifyNoInteractions(messagingTemplate);
            return null;
        });

        // La transacción ya hizo commit al salir de tx.execute(): recién ahora debe haberse
        // publicado, exactamente una vez.
        ArgumentCaptor<MusicEvent> captor = ArgumentCaptor.forClass(MusicEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), captor.capture());

        MusicEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MusicEventType.LIVE_MUSIC_STARTED);
        assertThat(event.roomId()).isEqualTo(roomId);
        assertThat(event.musicSessionId()).isEqualTo(session.getId());

        LiveMusicSession committed = musicSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(committed.getStatus()).isEqualTo(MusicSessionStatus.PLAYING);
        // La version del evento coincide con la version realmente committeada, no con un valor
        // stale leído antes del flush (por eso MusicService ahora usa saveAndFlush antes de
        // construir el snapshot que viaja en el evento).
        assertThat(event.version()).isEqualTo(committed.getVersion());
    }

    @Test
    void rollbackNeverPublishesEvent() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.execute(status -> {
            musicService.play(roomId, owner, null);
            status.setRollbackOnly();
            return null;
        });

        verifyNoInteractions(messagingTemplate);

        LiveMusicSession stillIdle = musicSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(stillIdle.getStatus()).isEqualTo(MusicSessionStatus.IDLE);
    }
}
