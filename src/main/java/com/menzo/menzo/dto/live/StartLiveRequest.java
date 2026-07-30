package com.menzo.menzo.dto.live;

import jakarta.validation.constraints.Size;

public record StartLiveRequest(
        @Size(max = 100) String title,
        @Size(max = 500) String description,
        @Size(max = 300) String announcement) {
}
