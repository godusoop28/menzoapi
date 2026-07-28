package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.WallCommentLike;

public interface WallCommentLikeRepository extends JpaRepository<WallCommentLike, WallCommentLike.WallCommentLikeId> {

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    long countByCommentId(UUID commentId);

    List<WallCommentLike> findByUserIdAndCommentIdIn(UUID userId, List<UUID> commentIds);

    @Transactional
    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);
}
