package com.menzo.menzo.domain.activity;

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
@Table(name = "recent_searches")
@Getter
@Setter
@NoArgsConstructor
public class RecentSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 140)
    private String query;

    @CreationTimestamp
    @Column(name = "searched_at", nullable = false, updatable = false)
    private Instant searchedAt;

    public RecentSearch(UUID userId, String query) {
        this.userId = userId;
        this.query = query;
    }
}
