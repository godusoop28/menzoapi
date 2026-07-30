package com.menzo.menzo.dto.live;

/** Tipos de evento publicados en /topic/rooms/{roomId}/live (y /room para los dos de sala).
 * Nota: no existe un evento de "active speaker"/volumen — esa señal viene directo del SDK de
 * Agora en el cliente y no se debe persistir ni transmitir por WebSocket (ver auditoría). */
public enum LiveEventType {
    CHAT_LIVE_STARTED,
    CHAT_LIVE_ENDED,
    CHAT_LIVE_UPDATED,
    CHAT_LIVE_PARTICIPANT_JOINED,
    CHAT_LIVE_PARTICIPANT_LEFT,
    CHAT_LIVE_SPEAKING_REQUESTED,
    CHAT_LIVE_SPEAKING_APPROVED,
    CHAT_LIVE_SPEAKING_REJECTED,
    CHAT_LIVE_PARTICIPANT_PROMOTED,
    CHAT_LIVE_PARTICIPANT_DEMOTED,
    CHAT_LIVE_MICROPHONE_CHANGED,
    CHAT_ROOM_UPDATED,
    CHAT_ROOM_APPEARANCE_UPDATED
}
