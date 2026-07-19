package com.menzo.menzo.repository.post;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.post.PostBookmark;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, PostBookmark.PostBookmarkId> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    List<PostBookmark> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds);

    @Transactional
    void deleteByPostIdAndUserId(UUID postId, UUID userId);
}
