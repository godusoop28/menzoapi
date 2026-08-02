package com.menzo.menzo.dto.sticker;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record StickerPackDetailResponse(
        UUID id,
        String name,
        UserSummary creator,
        List<StickerResponse> stickers,
        Instant createdAt) {
}
