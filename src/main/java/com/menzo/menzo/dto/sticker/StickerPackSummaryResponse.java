package com.menzo.menzo.dto.sticker;

import java.time.Instant;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

/** Para el listado/grilla del picker — coverImageUrl es el sticker de menor sortOrder, sin traer
 * el pack completo (ver StickerPackDetailResponse para eso). */
public record StickerPackSummaryResponse(
        UUID id,
        String name,
        UserSummary creator,
        String coverImageUrl,
        int stickerCount,
        Instant createdAt) {
}
