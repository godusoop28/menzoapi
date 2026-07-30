package com.menzo.menzo.dto.music;

import jakarta.validation.constraints.NotBlank;

public record RequestSongRequest(@NotBlank String videoId) {
}
