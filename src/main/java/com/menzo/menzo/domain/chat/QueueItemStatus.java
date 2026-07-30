package com.menzo.menzo.domain.chat;

public enum QueueItemStatus {
    /** Solicitud de un MEMBER pendiente de aprobación — no suena hasta que se aprueba. */
    PENDING,
    /** Aprobado / agregado directamente por OWNER-CO_HOST — en la cola, esperando su turno. */
    QUEUED,
    PLAYING,
    PLAYED,
    SKIPPED,
    REJECTED,
    REMOVED
}
