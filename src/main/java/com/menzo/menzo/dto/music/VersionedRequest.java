package com.menzo.menzo.dto.music;

/** Body reutilizado por los controles que no necesitan más dato que la versión esperada:
 * play/pause/resume/skip/stop. */
public record VersionedRequest(Long expectedVersion) {
}
