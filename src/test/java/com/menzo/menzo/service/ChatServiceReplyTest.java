package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.Message;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.user.Aura;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.chat.MessageResponse;
import com.menzo.menzo.dto.chat.SendMessageRequest;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.MessageRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.UserRepository;

/**
 * Cubre {@link ChatService#sendMessage} y el armado del {@link MessageResponse.ReplyPreview}
 * embebido — sin FK forzada en `reply_to_message_id` (ver V18__message_reply.sql), así que estos
 * tests son la única forma real de confirmar que "el id no resuelve" y "nunca fue una respuesta"
 * se distinguen correctamente sin necesidad de un endpoint de borrado de mensajes (que no existe).
 */
@SpringBootTest
class ChatServiceReplyTest {

    @Autowired private ChatService chatService;
    @Autowired private AuraRepository auraRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private RoomMemberRepository roomMemberRepository;
    @Autowired private MessageRepository messageRepository;

    @MockitoBean private SimpMessagingTemplate messagingTemplate;

    private User sender;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Aura aura = auraRepository.findById("fuego").orElseGet(() -> {
            Aura a = new Aura();
            a.setId("fuego");
            a.setName("Fuego");
            a.setDescription("Fixture aura for tests");
            a.setGradient("fire");
            return auraRepository.save(a);
        });

        sender = new User();
        sender.setEmail("reply-sender-" + suffix + "@test.menzo");
        sender.setUsername("replysender" + suffix);
        sender.setPasswordHash("x");
        sender.setDisplayName("Reply Sender " + suffix);
        sender.setAura(aura);
        sender.setJoinedAt(Instant.now());
        sender = userRepository.save(sender);
    }

    private ChatRoom createRoomWithMember(User member) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ChatRoom room = new ChatRoom();
        room.setSlug("reply-test-" + suffix);
        room.setName("Reply test room " + suffix);
        room = chatRoomRepository.save(room);
        roomMemberRepository.save(new RoomMember(room.getId(), member.getId()));
        return room;
    }

    @Test
    void replyWithinSameRoomEmbedsPreview() {
        ChatRoom room = createRoomWithMember(sender);
        MessageResponse original = chatService.sendMessage(
                sender, room.getId(), new SendMessageRequest("Mensaje original", null, null, null));

        MessageResponse reply = chatService.sendMessage(
                sender, room.getId(), new SendMessageRequest("Respondiendo esto", null, original.id(), null));

        assertThat(reply.replyTo()).isNotNull();
        assertThat(reply.replyTo().id()).isEqualTo(original.id());
        assertThat(reply.replyTo().authorName()).isEqualTo(sender.getDisplayName());
        assertThat(reply.replyTo().bodyPreview()).isEqualTo("Mensaje original");
        assertThat(reply.replyTo().deleted()).isFalse();
    }

    @Test
    void replyToMessageInDifferentRoomIsSilentlyIgnored() {
        ChatRoom roomA = createRoomWithMember(sender);
        ChatRoom roomB = createRoomWithMember(sender);
        MessageResponse inRoomA = chatService.sendMessage(
                sender, roomA.getId(), new SendMessageRequest("Vivo en la sala A", null, null, null));

        MessageResponse sentInRoomB = chatService.sendMessage(
                sender, roomB.getId(), new SendMessageRequest("Respuesta cruzada", null, inRoomA.id(), null));

        assertThat(sentInRoomB.replyTo()).isNull();
    }

    @Test
    void deletedOriginalFallsBackToDeletedPreviewWithIdPreserved() {
        ChatRoom room = createRoomWithMember(sender);
        UUID danglingId = UUID.randomUUID();

        // Sin pasar por sendMessage a propósito: sendMessage nunca deja crear una referencia
        // colgante a mano (valida que el original exista en la misma sala), así que la única
        // forma de simular "el original ya no existe" sin un endpoint de borrado real es escribir
        // el id suelto directo por el repositorio, tal como quedaría una fila vieja si algún día
        // se agrega un borrado físico de mensajes.
        Message reply = new Message();
        reply.setRoom(room);
        reply.setAuthor(sender);
        reply.setBody("Respondiendo a algo que ya no está");
        reply.setReplyToMessageId(danglingId);
        messageRepository.saveAndFlush(reply);

        PageResponse<MessageResponse> page = chatService.listMessages(room.getId(), PageRequest.of(0, 20), sender);
        List<MessageResponse> matches = page.items().stream()
                .filter(m -> m.id().equals(reply.getId()))
                .toList();

        assertThat(matches).hasSize(1);
        MessageResponse.ReplyPreview preview = matches.get(0).replyTo();
        assertThat(preview).isNotNull();
        assertThat(preview.id()).isEqualTo(danglingId);
        assertThat(preview.deleted()).isTrue();
    }
}
