package com.menzo.menzo.repository.post;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.post.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    List<PostLike> findByPostId(UUID postId);

    List<PostLike> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds);

    @Transactional
    void deleteByPostIdAndUserId(UUID postId, UUID userId);
}
