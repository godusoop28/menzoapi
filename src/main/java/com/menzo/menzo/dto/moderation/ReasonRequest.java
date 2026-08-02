package com.menzo.menzo.dto.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mandatory-reason body for every global staff moderation action (suspend, delete account,
 * delete/hide post, delete message, kick from live, promote/demote role, delete sticker pack).
 * @NotBlank here means an empty reason never reaches any service method. */
public record ReasonRequest(@NotBlank @Size(max = 300) String reason) {
}
