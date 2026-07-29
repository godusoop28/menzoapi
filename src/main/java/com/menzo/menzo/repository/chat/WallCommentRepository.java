package com.menzo.menzo.repository.chat;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.WallComment;

public interface WallCommentRepository extends JpaRepository<WallComment, UUID> {

    Page<WallComment> findByWallMessageIdOrderByCreatedAtAsc(UUID wallMessageId, Pageable pageable);

    long countByWallMessageId(UUID wallMessageId);
}
