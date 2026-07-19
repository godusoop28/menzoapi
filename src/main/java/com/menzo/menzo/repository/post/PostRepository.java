package com.menzo.menzo.repository.post;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.post.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByFeaturedTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            JOIN PostBookmark b ON b.postId = p.id
            WHERE b.userId = :userId
            ORDER BY b.createdAt DESC
            """)
    Page<Post> findBookmarkedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE lower(p.body) LIKE lower(concat('%', :query, '%'))
               OR lower(p.title) LIKE lower(concat('%', :query, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> search(@Param("query") String query, Pageable pageable);
}
