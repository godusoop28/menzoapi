package com.menzo.menzo.dto.post;

import jakarta.validation.constraints.NotBlank;

public record AbstractVisualRequest(@NotBlank String preset, String caption) {
}
