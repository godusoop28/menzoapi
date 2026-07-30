package com.menzo.menzo.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 150) String topic,
        String gradient,
        String icon,
        @Size(max = 40) String category) {
}
