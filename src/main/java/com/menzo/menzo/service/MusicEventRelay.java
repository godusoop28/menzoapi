package com.menzo.menzo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.menzo.menzo.dto.music.MusicEvent;
import com.menzo.menzo.dto.music.MusicSessionResponse;

/**
 * Única puerta de salida de los eventos de Menzi DJ hacia STOMP. MusicService nunca llama a
 * SimpMessagingTemplate directamente: publica un MusicEvent como ApplicationEvent de Spring
 * (ver MusicService.publish), y este listener lo recoge con phase = AFTER_COMMIT.
 *
 * Esto es lo que corrige que "solo el anfitrión escuchara": antes, convertAndSend corría dentro
 * del método @Transactional, así que el mensaje WebSocket podía llegar a otro dispositivo, ese
 * dispositivo hacer un GET de reconciliación, y esa lectura ocurrir antes de que el commit de la
 * transacción original fuera visible para su conexión — recibía el estado anterior. Con
 * AFTER_COMMIT, para cuando este método corre, el commit ya se hizo: cualquier GET posterior al
 * mensaje ve el estado nuevo, sin condición de carrera.
 *
 * Si la transacción termina en rollback, este listener no se ejecuta — Spring lo garantiza — así
 * que nunca se emite un evento sobre un cambio que nunca existió en la base.
 */
@Component
public class MusicEventRelay {

    private static final Logger log = LoggerFactory.getLogger(MusicEventRelay.class);

    private final SimpMessagingTemplate messagingTemplate;

    public MusicEventRelay(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMusicEvent(MusicEvent event) {
        String destination = "/topic/rooms/" + event.roomId() + "/music";
        messagingTemplate.convertAndSend(destination, event);
        if (event.payload() instanceof MusicSessionResponse session) {
            log.info(
                    "Menzi DJ event published after commit: type={}, roomId={}, liveSessionId={}, "
                            + "musicSessionId={}, version={}, eventId={}, videoId={}, status={}, positionSeconds={}",
                    event.type(), event.roomId(), event.liveSessionId(), event.musicSessionId(), event.version(),
                    event.eventId(), session.currentVideoId(), session.status(), session.positionSeconds());
        } else {
            log.info(
                    "Menzi DJ event published after commit: type={}, roomId={}, liveSessionId={}, "
                            + "musicSessionId={}, version={}, eventId={}",
                    event.type(), event.roomId(), event.liveSessionId(), event.musicSessionId(), event.version(),
                    event.eventId());
        }
    }
}
