package com.menzo.menzo.dto.user;

public record UpdateSettingsRequest(
        String theme,
        String effectIntensity,
        Boolean hapticsEnabled,
        Boolean animationsEnabled,
        Boolean showSimulatedActivity,
        Boolean confirmationsEnabled,
        Boolean showOnlineStatus,
        Boolean allowProfileVisits,
        Boolean showInterests) {
}
