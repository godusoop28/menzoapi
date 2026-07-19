package com.menzo.menzo.domain.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserBadge {

    @Column(name = "badge_id", length = 30, nullable = false)
    private String badgeId;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;
}
