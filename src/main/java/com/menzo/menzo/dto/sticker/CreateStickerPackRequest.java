package com.menzo.menzo.dto.sticker;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** El cliente sube cada imagen primero (reusa /api/uploads → Cloudinary, ya existente) y manda
 * acá las URLs resultantes, en el orden en que deben aparecer en el pack. */
public record CreateStickerPackRequest(
        @NotBlank @Size(max = 60) String name,
        @NotEmpty @Size(max = 30) List<@NotBlank String> imageUrls) {
}
