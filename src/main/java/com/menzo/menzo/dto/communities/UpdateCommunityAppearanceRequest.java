package com.menzo.menzo.dto.communities;

/** Cadena vacía "" limpia el campo; omitirlo (null) lo deja sin cambios — mismo criterio que
 * UpdateRoomRequest. Cada imagen ya se subió antes a /api/uploads (Cloudinary genérico); acá solo
 * se guarda la URL resultante. */
public record UpdateCommunityAppearanceRequest(
        String iconUrl,
        String logoUrl,
        String coverUrl,
        String backgroundUrl,
        String bannerUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor) {
}
