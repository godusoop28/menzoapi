package com.menzo.menzo.repository.post;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.post.Comment;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    long countByPostId(UUID postId);
}
