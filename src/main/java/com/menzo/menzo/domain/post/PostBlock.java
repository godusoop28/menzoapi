package com.menzo.menzo.domain.post;

/**
 * Un bloque del cuerpo de un post — leído/escrito siempre como parte de la lista completa de
 * {@link Post#getBlocks()} (columna JSONB, ver V19__post_blocks.sql), nunca consultado ni
 * referenciado individualmente desde otra tabla. Por eso es un record plano, no una entidad JPA
 * propia: no hace falta identidad de fila, cascada, ni fetch independiente.
 *
 * <p>{@code id} es generado por el cliente (UUID/string arbitrario), solo para que React/Flutter
 * tengan una key estable al reordenar — el servidor nunca le da significado propio más allá de
 * pasarlo de largo.
 *
 * <p>Tipos soportados a propósito, sin más (ver PostService para los límites de validación):
 * <ul>
 *   <li>{@code paragraph} / {@code heading}: usan {@code text}, ignoran {@code url}/{@code alt}.
 *   <li>{@code image} / {@code gif}: usan {@code url} (siempre una URL https ya subida — nunca
 *       una ruta local) y opcionalmente {@code alt}, ignoran {@code text}.
 *   <li>{@code divider}: no usa ningún campo de contenido, solo marca una pausa visual.
 * </ul>
 */
public record PostBlock(String id, String type, String text, String url, String alt) {

    public static final String TYPE_PARAGRAPH = "paragraph";
    public static final String TYPE_HEADING = "heading";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_GIF = "gif";
    public static final String TYPE_DIVIDER = "divider";
}
