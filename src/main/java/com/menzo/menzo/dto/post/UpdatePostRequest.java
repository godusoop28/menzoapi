package com.menzo.menzo.dto.post;

import java.util.List;

import com.menzo.menzo.domain.post.PostBlock;

import jakarta.validation.constraints.Size;

/**
 * Edición mínima, sin historial de versiones — reemplaza título/bloques/tags tal cual, nunca el
 * tipo, las opciones de encuesta ni el evento vinculado (esos siguen siendo solo de creación,
 * fuera de alcance a propósito: convertir un post en otro tipo distinto no es "editar", es otra
 * cosa). Solo pensado para posts text/image (los únicos con `blocks`).
 */
public record UpdatePostRequest(@Size(max = 150) String title, List<PostBlock> blocks, List<String> tags) {
}
