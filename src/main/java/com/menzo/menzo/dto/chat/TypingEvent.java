package com.menzo.menzo.dto.chat;

import java.util.UUID;

public record TypingEvent(UUID userId, String displayName, boolean typing) {
}
