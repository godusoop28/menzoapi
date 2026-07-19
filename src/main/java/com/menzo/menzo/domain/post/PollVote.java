package com.menzo.menzo.domain.post;

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
@Table(name = "poll_votes")
@Getter
@Setter
@NoArgsConstructor
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt;

    public PollVote(UUID optionId, UUID postId, UUID userId) {
        this.optionId = optionId;
        this.postId = postId;
        this.userId = userId;
    }
}
