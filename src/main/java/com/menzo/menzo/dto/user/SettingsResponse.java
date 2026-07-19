package com.menzo.menzo.dto.user;

public record SettingsResponse(
        String theme,
        String effectIntensity,
        boolean hapticsEnabled,
        boolean animationsEnabled,
        boolean showSimulatedActivity,
        boolean confirmationsEnabled,
        boolean showOnlineStatus,
        boolean allowProfileVisits,
        boolean showInterests) {
}
