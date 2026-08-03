package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.menzo.menzo.dto.chat.ChatRoomResponse;

/**
 * Cubre {@link ChatService#INBOX_ORDER}, el comparador que ordena "Mis chats" — antes
 * {@code listRooms} no ordenaba nada, así que la bandeja mostraba las salas en el orden en que
 * las devolvía la consulta, sin relación con cuál tenía el mensaje más reciente. No requiere
 * base de datos: arma {@link ChatRoomResponse} a mano, con valores mínimos y `null` en todo lo
 * que no participa del orden.
 */
class ChatServiceInboxOrderTest {

    private static ChatRoomResponse roomWith(UUID id, Instant lastMessageAt, Instant updatedAt, Instant createdAt) {
        ChatRoomResponse.LastMessage lastMessage = lastMessageAt == null
                ? null
                : new ChatRoomResponse.LastMessage("hola", false, "user-1", lastMessageAt);
        return new ChatRoomResponse(
                id, "slug", "Sala", null, null, null, null, "PUBLIC", null, null, null, null, null,
                false, false, true, null, 0, 0, false, true, "MEMBER", false, null,
                createdAt, updatedAt, lastMessage, null);
    }

    @Test
    void ordenaPorUltimoMensajeMasReciente() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        ChatRoomResponse antiguo = roomWith(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                now.minusSeconds(3600), null, now.minusSeconds(7200));
        ChatRoomResponse reciente = roomWith(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                now, null, now.minusSeconds(7200));

        List<ChatRoomResponse> rooms = new ArrayList<>(List.of(antiguo, reciente));
        rooms.sort(ChatService.INBOX_ORDER);

        assertThat(rooms).containsExactly(reciente, antiguo);
    }

    @Test
    void salaSinMensajesUsaUpdatedAt() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        ChatRoomResponse conMensaje = roomWith(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                now.minusSeconds(100), null, now.minusSeconds(7200));
        ChatRoomResponse sinMensajesPeroActualizadaHaceUnRato = roomWith(
                UUID.fromString("00000000-0000-0000-0000-000000000002"), null, now, now.minusSeconds(7200));

        List<ChatRoomResponse> rooms = new ArrayList<>(List.of(conMensaje, sinMensajesPeroActualizadaHaceUnRato));
        rooms.sort(ChatService.INBOX_ORDER);

        assertThat(rooms).containsExactly(sinMensajesPeroActualizadaHaceUnRato, conMensaje);
    }

    @Test
    void salaSinMensajesNiUpdatedAtUsaCreatedAtYQuedaAlFinal() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        ChatRoomResponse activa = roomWith(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                now, null, now.minusSeconds(999999));
        ChatRoomResponse nuncaUsada = roomWith(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                null, null, now.minusSeconds(10));

        List<ChatRoomResponse> rooms = new ArrayList<>(List.of(nuncaUsada, activa));
        rooms.sort(ChatService.INBOX_ORDER);

        assertThat(rooms).containsExactly(activa, nuncaUsada);
    }

    @Test
    void empateExactoDesempataPorIdDeFormaEstable() {
        Instant same = Instant.parse("2026-08-01T12:00:00Z");
        ChatRoomResponse a = roomWith(UUID.fromString("00000000-0000-0000-0000-000000000001"), same, null, same);
        ChatRoomResponse b = roomWith(UUID.fromString("00000000-0000-0000-0000-000000000002"), same, null, same);

        List<ChatRoomResponse> rooms1 = new ArrayList<>(List.of(b, a));
        List<ChatRoomResponse> rooms2 = new ArrayList<>(List.of(a, b));
        rooms1.sort(ChatService.INBOX_ORDER);
        rooms2.sort(ChatService.INBOX_ORDER);

        assertThat(rooms1).containsExactlyElementsOf(rooms2);
    }
}
