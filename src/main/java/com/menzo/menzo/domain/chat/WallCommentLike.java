package com.menzo.menzo.domain.chat;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wall_comment_likes")
@IdClass(WallCommentLike.WallCommentLikeId.class)
@Getter
@Setter
@NoArgsConstructor
public class WallCommentLike {

    @Id
    @Column(name = "comment_id")
    private UUID commentId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WallCommentLike(UUID commentId, UUID userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class WallCommentLikeId implements Serializable {
        private UUID commentId;
        private UUID userId;

        public WallCommentLikeId(UUID commentId, UUID userId) {
            this.commentId = commentId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WallCommentLikeId that)) return false;
            return Objects.equals(commentId, that.commentId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(commentId, userId);
        }
    }
}
