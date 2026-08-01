package com.menzo.menzo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.menzo.menzo.dto.live.LiveEvent;

/**
 * Única puerta de salida de los eventos de LIVE (join/leave/mute/kick/demote/speaking-requests)
 * hacia STOMP — mismo rol que MusicEventRelay, para el mismo bug: LiveService.publish ya no
 * llama a SimpMessagingTemplate directamente, publica un LiveEvent como ApplicationEvent y este
 * listener lo despacha recién AFTER_COMMIT. Sin esto, un FORCE_MUTE o un KICKED podía llegarle al
 * cliente destino antes de que el UPDATE sobre live_participants estuviera confirmado — y si ese
 * cliente reaccionaba con un GET (participantes, estado del LIVE), corría el riesgo de leer el
 * estado anterior.
 */
@Component
public class LiveEventRelay {

    private static final Logger log = LoggerFactory.getLogger(LiveEventRelay.class);

    private final SimpMessagingTemplate messagingTemplate;

    public LiveEventRelay(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLiveEvent(LiveEvent event) {
        String destination = "/topic/rooms/" + event.roomId() + "/live";
        messagingTemplate.convertAndSend(destination, event);
        log.info(
                "Live event published after commit: type={}, roomId={}, liveSessionId={}, eventId={}",
                event.type(), event.roomId(), event.liveSessionId(), event.eventId());
    }
}
