package com.menzo.menzo.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.chat.TypingEvent;

/**
 * Señales efímeras de "está escribiendo" — nunca tocan la base de datos, solo se retransmiten en
 * vivo a quien esté suscrito a la sala. Igual que la presencia de voz de {@code VoiceService}, no
 * necesitan persistencia: si el mensaje se pierde no pasa nada, el próximo tick lo corrige.
 */
@Controller
public class TypingController {

    public record TypingRequest(boolean typing) {
    }

    private final SimpMessagingTemplate messagingTemplate;

    public TypingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/rooms/{roomId}/typing")
    public void typing(@DestinationVariable UUID roomId, @Payload TypingRequest request, Principal principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/typing",
                new TypingEvent(user.getId(), user.getDisplayName(), request.typing()));
    }

    private User resolveUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
