package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.menzo.menzo.domain.chat.LiveParticipantStatus;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.chat.RoomRole;
import com.menzo.menzo.domain.user.Aura;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.dto.live.LiveEvent;
import com.menzo.menzo.dto.live.LiveEventType;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.repository.chat.ChatLiveSessionRepository;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.LiveParticipantRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.domain.chat.LiveParticipant;

/**
 * Mismo fix que MusicServiceAfterCommitTest, aplicado a LiveService: FORCE_MUTE/KICK/LIVE_ENDED
 * deben salir por STOMP recién después del commit, nunca antes, y nunca si la transacción hace
 * rollback. También cubre que forceMute/removeParticipant produzcan el efecto real esperado en
 * la fila de LiveParticipant (lo que el cliente Flutter necesita para actuar sobre Agora).
 */
@SpringBootTest
class LiveServiceAfterCommitTest {

    @Autowired private LiveService liveService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AuraRepository auraRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private RoomMemberRepository roomMemberRepository;
    @Autowired private ChatLiveSessionRepository chatLiveSessionRepository;
    @Autowired private LiveParticipantRepository liveParticipantRepository;

    @MockitoBean private SimpMessagingTemplate messagingTemplate;

    private UUID roomId;
    private User owner;
    private User target;

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

        owner = createUser(aura, "live-owner-" + suffix);
        target = createUser(aura, "live-target-" + suffix);

        ChatRoom room = new ChatRoom();
        room.setSlug("live-aftercommit-" + suffix);
        room.setName("Live after-commit room " + suffix);
        room = chatRoomRepository.save(room);
        roomId = room.getId();

        roomMemberRepository.save(new RoomMember(roomId, owner.getId(), RoomRole.OWNER));
        roomMemberRepository.save(new RoomMember(roomId, target.getId(), RoomRole.MEMBER));

        liveService.startLive(owner, roomId, null);
        reset(messagingTemplate);
        liveService.joinLive(target, roomId);
        reset(messagingTemplate);

        // El target necesita poder hablar para que forceMute tenga sentido probarlo — se lo deja
        // como SPEAKER directamente vía el repositorio en vez de pasar por todo el flujo de
        // solicitar/aprobar, que no es lo que este test está verificando.
        LiveParticipant participant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), target.getId())
                .orElseThrow();
        participant.setRole(com.menzo.menzo.domain.chat.LiveParticipantRole.SPEAKER);
        participant.setMicrophoneEnabled(true);
        liveParticipantRepository.save(participant);
        reset(messagingTemplate);
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

    private UUID activeSessionId() {
        return chatLiveSessionRepository
                .findByRoomIdAndStatus(roomId, com.menzo.menzo.domain.chat.LiveSessionStatus.ACTIVE)
                .orElseThrow()
                .getId();
    }

    @Test
    void forceMuteDoesNotPublishBeforeCommit_publishesExactlyOnceAfterCommit_andMutesRealRow() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.execute(status -> {
            liveService.forceMute(owner, roomId, target.getId());
            verifyNoInteractions(messagingTemplate);
            return null;
        });

        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(LiveEventType.CHAT_LIVE_MICROPHONE_CHANGED);

        LiveParticipant reloaded = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), target.getId())
                .orElseThrow();
        assertThat(reloaded.isMicrophoneEnabled()).isFalse();
    }

    @Test
    void forceMuteRollbackNeverPublishesEvent() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            liveService.forceMute(owner, roomId, target.getId());
            status.setRollbackOnly();
            return null;
        });
        verifyNoInteractions(messagingTemplate);

        LiveParticipant reloaded = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), target.getId())
                .orElseThrow();
        // El rollback deshizo el mute — sigue como estaba antes (true, seteado en setUp).
        assertThat(reloaded.isMicrophoneEnabled()).isTrue();
    }

    /** Fase de estabilización: forceMute tenía su propio chequeo inline ("solo el OWNER puede
     * mutear al HOST"), distinto del que usan demoteParticipant/removeParticipant
     * (requireCanModerateLiveParticipant, que rechaza moderar al HOST sin excepción alguna).
     * Unificado a la misma regla — ahora ni el propio OWNER puede forceMute-ear al HOST por esta
     * vía (igual que ya pasaba con demote/remove), sin importar que sea la misma persona. */
    @Test
    void forceMuteOnHostIsForbiddenEvenForOwner() {
        assertThatThrownBy(() -> liveService.forceMute(owner, roomId, owner.getId()))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void removeParticipantMarksLeftAndPublishesAfterCommit() {
        liveService.removeParticipant(owner, roomId, target.getId());

        verify(messagingTemplate, times(1)).convertAndSend(anyString(), org.mockito.ArgumentMatchers.any(LiveEvent.class));
        LiveParticipant reloaded = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), target.getId())
                .orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(LiveParticipantStatus.LEFT);
        assertThat(reloaded.isMicrophoneEnabled()).isFalse();
    }

    @Test
    void endLivePublishesGlobalEndedEventAfterCommit() {
        UUID liveSessionId = activeSessionId();
        liveService.endLive(owner, roomId);

        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(LiveEventType.CHAT_LIVE_ENDED);

        boolean anyActive = liveParticipantRepository
                .findByLiveSessionIdAndStatus(liveSessionId, LiveParticipantStatus.ACTIVE)
                .stream()
                .findAny()
                .isPresent();
        assertThat(anyActive).isFalse();
    }

    @Test
    void setScreenSharingRejectsPlainSpeaker() {
        // `target` quedó como SPEAKER en setUp — puede hablar, pero eso no alcanza para compartir
        // pantalla (gate más angosto que SPEAKING_ROLES, ver LiveService.setScreenSharing).
        assertThatThrownBy(() -> liveService.setScreenSharing(target, roomId, true))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void setScreenSharingAllowsHostAndPublishesStarted() {
        liveService.setScreenSharing(owner, roomId, true);

        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(LiveEventType.CHAT_LIVE_SCREEN_SHARE_STARTED);

        LiveParticipant reloaded = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), owner.getId())
                .orElseThrow();
        assertThat(reloaded.isScreenSharing()).isTrue();
    }

    @Test
    void setScreenSharingAutoStopsThePreviousPresenter() {
        // Se promueve a `target` a CO_HOST directo por repositorio (mismo atajo que el resto de
        // este archivo usa para SPEAKER) — lo que este test verifica es el traspaso, no el flujo
        // de promoción en sí.
        LiveParticipant targetParticipant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), target.getId())
                .orElseThrow();
        targetParticipant.setRole(com.menzo.menzo.domain.chat.LiveParticipantRole.CO_HOST);
        liveParticipantRepository.save(targetParticipant);

        liveService.setScreenSharing(owner, roomId, true);
        reset(messagingTemplate);

        liveService.setScreenSharing(target, roomId, true);

        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), captor.capture());
        assertThat(captor.getAllValues().stream().map(LiveEvent::type))
                .containsExactly(LiveEventType.CHAT_LIVE_SCREEN_SHARE_STOPPED, LiveEventType.CHAT_LIVE_SCREEN_SHARE_STARTED);

        LiveParticipant ownerReloaded = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), owner.getId())
                .orElseThrow();
        LiveParticipant targetReloaded = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), target.getId())
                .orElseThrow();
        assertThat(ownerReloaded.isScreenSharing()).isFalse();
        assertThat(targetReloaded.isScreenSharing()).isTrue();
    }

    @Test
    void newHostStartsMutedNotUnmuted() {
        // Fase 14: el HOST que inicia el LIVE debe entrar con microphoneEnabled=false — Flutter
        // siempre publica el audio ya silenciado por defecto (ver LiveNotifier._publishMic), así
        // que si el backend dijera true acá, todos los demás verían la insignia del anfitrión
        // como "con micrófono" mientras en los hechos estaba mudo.
        LiveParticipant hostParticipant = liveParticipantRepository
                .findByLiveSessionIdAndUserId(activeSessionId(), owner.getId())
                .orElseThrow();
        assertThat(hostParticipant.isMicrophoneEnabled()).isFalse();
    }
}
