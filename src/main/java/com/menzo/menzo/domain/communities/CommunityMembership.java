package com.menzo.menzo.domain.communities;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relaciona un usuario con UNA comunidad — nunca duplica perfil/nivel/XP (eso sigue viviendo
 * enteramente en User, global). Clave compuesta (communityId, userId), mismo patrón que
 * RoomMember, que además satisface directamente unique(user_id, community_id) sin una
 * constraint aparte.
 */
@Entity
@Table(name = "community_memberships")
@IdClass(CommunityMembership.CommunityMembershipId.class)
@Getter
@Setter
@NoArgsConstructor
public class CommunityMembership {

    @Id
    @Column(name = "community_id")
    private UUID communityId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "community_role", nullable = false, length = 30)
    private CommunityRole communityRole = CommunityRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private CommunityMembershipStatus membershipStatus = CommunityMembershipStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "last_visited_at")
    private Instant lastVisitedAt;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite = false;

    @Column(name = "is_muted", nullable = false)
    private boolean muted = false;

    @Column(name = "community_nickname", length = 60)
    private String communityNickname;

    @Column(name = "community_bio", length = 280)
    private String communityBio;

    // Contadores puramente informativos de "cuánto aportó acá" — nunca alimentan XP/nivel
    // global (ver Contexto §6 del pedido: eso solo puede subir por acciones registradas en el
    // log de XP global, no por ser miembro de una comunidad).
    @Column(name = "contribution_count", nullable = false)
    private int contributionCount = 0;

    @Column(name = "posts_count", nullable = false)
    private int postsCount = 0;

    @Column(name = "comments_count", nullable = false)
    private int commentsCount = 0;

    @Column(name = "voice_participation_count", nullable = false)
    private int voiceParticipationCount = 0;

    @Column(name = "warnings_count", nullable = false)
    private int warningsCount = 0;

    @Column(name = "banned_until")
    private Instant bannedUntil;

    @Column(name = "custom_title", length = 60)
    private String customTitle;

    public CommunityMembership(UUID communityId, UUID userId) {
        this.communityId = communityId;
        this.userId = userId;
    }

    public CommunityMembership(UUID communityId, UUID userId, CommunityRole communityRole) {
        this.communityId = communityId;
        this.userId = userId;
        this.communityRole = communityRole;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CommunityMembershipId implements Serializable {
        private UUID communityId;
        private UUID userId;

        public CommunityMembershipId(UUID communityId, UUID userId) {
            this.communityId = communityId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CommunityMembershipId that)) return false;
            return Objects.equals(communityId, that.communityId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(communityId, userId);
        }
    }
}
