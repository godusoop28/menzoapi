package com.menzo.menzo.dto.post;

import java.util.List;
import java.util.UUID;

import com.menzo.menzo.domain.post.PostBlock;
import com.menzo.menzo.domain.post.PostType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code body} ya no es {@code @NotBlank} — para type text/image el cuerpo real vive en
 * {@code blocks} y {@code body} se deriva server-side (ver PostService/Post.body); sigue siendo
 * el campo directo para poll/question/event, que no usan bloques. PostService valida cuál de los
 * dos hace falta según el {@code type}, no esta anotación (una lista polimórfica como
 * {@code blocks} no se puede expresar del todo con bean validation).
 */
public record CreatePostRequest(
        @NotNull PostType type,
        @Size(max = 150) String title,
        String body,
        String imageUri,
        @Valid AbstractVisualRequest abstractVisual,
        String gradient,
        List<String> tags,
        List<@NotBlank String> pollOptions,
        UUID eventId,
        List<PostBlock> blocks) {
}
