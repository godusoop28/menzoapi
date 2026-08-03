package com.menzo.menzo.dto.communities;

import java.util.List;
import java.util.UUID;

/** Forma liviana usada en listados (discover/my/búsqueda) — nunca expone la entidad JPA
 * directamente ni sus configs JSONB completas (eso es CommunityDetailDto). */
public record CommunitySummaryDto(
        UUID id,
        String slug,
        String name,
        String shortDescription,
        String category,
        List<String> tags,
        String iconUrl,
        String logoUrl,
        String coverUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        long memberCount,
        boolean featured,
        boolean official,
        String visibility,
        String accessType,
        boolean joined) {
}
