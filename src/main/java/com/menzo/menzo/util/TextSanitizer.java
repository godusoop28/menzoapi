package com.menzo.menzo.util;

import java.util.regex.Pattern;

/** Normaliza texto libre ingresado por el usuario (descripciones, anuncios, títulos de LIVE):
 * quita cualquier etiqueta HTML/script (nunca se interpreta como HTML en el cliente igual, pero
 * se limpia en el backend como defensa en profundidad), recorta espacios y colapsa vacío a null.
 * No toca emojis ni caracteres Unicode. */
public final class TextSanitizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    private TextSanitizer() {
    }

    /** null si raw es null o queda vacío tras sanitizar/recortar; nunca devuelve "" ni un string
     * de solo espacios. */
    public static String normalizeToNull(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String cleaned = HTML_TAG.matcher(raw).replaceAll("").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
