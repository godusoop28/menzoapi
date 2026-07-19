package com.menzo.menzo.domain.post;

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
@Table(name = "post_likes")
@IdClass(PostLike.PostLikeId.class)
@Getter
@Setter
@NoArgsConstructor
public class PostLike {

    @Id
    @Column(name = "post_id")
    private UUID postId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PostLike(UUID postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PostLikeId implements Serializable {
        private UUID postId;
        private UUID userId;

        public PostLikeId(UUID postId, UUID userId) {
            this.postId = postId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostLikeId that)) return false;
            return Objects.equals(postId, that.postId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, userId);
        }
    }
}
