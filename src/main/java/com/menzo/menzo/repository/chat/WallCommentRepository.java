package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.WallComment;

public interface WallCommentRepository extends JpaRepository<WallComment, UUID> {

    List<WallComment> findByWallMessageIdOrderByCreatedAtAsc(UUID wallMessageId);

    long countByWallMessageId(UUID wallMessageId);
}
