package com.menzo.menzo.dto.music;

import jakarta.validation.constraints.NotBlank;

/** playNow: si es true (o si Menzi DJ está IDLE/STOPPED), el video empieza a sonar de inmediato
 * en vez de solo agregarse al final de la cola — cubre el botón "Reproducir" de la búsqueda sin
 * necesitar un endpoint aparte. */
public record AddQueueItemRequest(@NotBlank String videoId, Long expectedVersion, Boolean playNow) {
}
