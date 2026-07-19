package com.menzo.menzo.domain.user;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profile_visits")
@Getter
@Setter
@NoArgsConstructor
public class ProfileVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @CreationTimestamp
    @Column(name = "visited_at", nullable = false, updatable = false)
    private Instant visitedAt;

    public ProfileVisit(UUID visitorId, UUID profileId) {
        this.visitorId = visitorId;
        this.profileId = profileId;
    }
}
