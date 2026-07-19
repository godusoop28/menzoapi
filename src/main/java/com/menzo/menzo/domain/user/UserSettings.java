package com.menzo.menzo.domain.user;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String theme = "medianoche";

    @Column(name = "effect_intensity", nullable = false, length = 10)
    private String effectIntensity = "normal";

    @Column(name = "haptics_enabled", nullable = false)
    private boolean hapticsEnabled = true;

    @Column(name = "animations_enabled", nullable = false)
    private boolean animationsEnabled = true;

    @Column(name = "show_simulated_activity", nullable = false)
    private boolean showSimulatedActivity = true;

    @Column(name = "confirmations_enabled", nullable = false)
    private boolean confirmationsEnabled = true;

    @Column(name = "show_online_status", nullable = false)
    private boolean showOnlineStatus = true;

    @Column(name = "allow_profile_visits", nullable = false)
    private boolean allowProfileVisits = true;

    @Column(name = "show_interests", nullable = false)
    private boolean showInterests = true;

    public UserSettings(UUID userId) {
        this.userId = userId;
    }
}
