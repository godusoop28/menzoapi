package com.menzo.menzo.dto.community;

import java.util.List;

public record CommunityConfigResponse(
        String name,
        String subtitle,
        String description,
        String motto,
        long memberCount,
        long onlineCount,
        List<String> tags) {
}
