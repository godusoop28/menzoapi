package com.menzo.menzo.domain.moderation;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.menzo.menzo.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Insert-only audit log — enforced by simply never adding update/delete methods to
 * ModerationActionRepository, same convention this codebase already uses elsewhere instead of DB
 * triggers. actor uses ON DELETE RESTRICT (see V22__moderation_actions.sql) so a moderator's
 * history can never vanish even if that moderator's account is later deleted by MASTER.
 */
@Entity
@Table(name = "moderation_actions")
@Getter
@Setter
@NoArgsConstructor
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ModerationActionType actionType;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 300)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ModerationAction(User actor, ModerationActionType actionType, String targetType, UUID targetId, String reason) {
        this.actor = actor;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
    }
}
