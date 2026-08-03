package com.menzo.menzo.dto.communities;

import java.time.Instant;

/** Nunca incluye nivel/XP — eso es siempre global (ver User), esta membresía solo describe la
 * relación con ESTA comunidad puntual. */
public record CommunityMembershipDto(
        String communityRole,
        String membershipStatus,
        Instant joinedAt,
        Instant lastVisitedAt,
        boolean favorite,
        boolean muted,
        String communityNickname,
        String communityBio,
        int contributionCount,
        String customTitle) {
}
