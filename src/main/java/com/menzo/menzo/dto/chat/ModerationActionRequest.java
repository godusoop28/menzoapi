package com.menzo.menzo.dto.chat;

import jakarta.validation.constraints.Size;

public record ModerationActionRequest(@Size(max = 300) String reason) {
}
