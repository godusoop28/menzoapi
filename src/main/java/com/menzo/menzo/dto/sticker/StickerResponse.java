package com.menzo.menzo.dto.sticker;

import java.util.UUID;

public record StickerResponse(UUID id, String imageUrl, int sortOrder) {
}
