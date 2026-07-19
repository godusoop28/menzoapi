package com.menzo.menzo.dto.post;

import java.util.List;
import java.util.UUID;

import com.menzo.menzo.domain.post.PostType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull PostType type,
        @Size(max = 150) String title,
        @NotBlank String body,
        String imageUri,
        @Valid AbstractVisualRequest abstractVisual,
        String gradient,
        List<String> tags,
        List<@NotBlank String> pollOptions,
        UUID eventId) {
}
