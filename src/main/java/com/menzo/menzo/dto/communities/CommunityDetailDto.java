package com.menzo.menzo.dto.communities;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CommunityDetailDto(
        UUID id,
        String slug,
        String name,
        String shortDescription,
        String fullDescription,
        String status,
        String visibility,
        String accessType,
        String primaryLanguage,
        String category,
        List<String> tags,
        String iconUrl,
        String logoUrl,
        String coverUrl,
        String backgroundUrl,
        String bannerUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String textColor,
        String surfaceColor,
        long memberCount,
        long onlineMemberCount,
        long postCount,
        long chatCount,
        boolean featured,
        boolean official,
        boolean allowJoinRequests,
        boolean allowPublicChats,
        boolean allowBlogs,
        boolean allowVoiceRooms,
        boolean allowMemberPosts,
        int minimumGlobalLevelToJoin,
        int minimumGlobalLevelToPost,
        Map<String, Object> themeConfig,
        Map<String, Object> navigationConfig,
        Instant createdAt,
        CommunityMembershipDto myMembership) {
}
