package com.menzo.menzo.repository.post;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.post.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {

    // Todo listado de feed/búsqueda/muro filtra hidden = false (acción de CURATOR+) — getPost
    // (fetch directo por id, en PostService) NO filtra, así que el autor o el staff que lo está
    // revisando directamente lo siguen viendo.
    Page<Post> findByHiddenFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByFeaturedTrueAndHiddenFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByAuthorIdAndHiddenFalseOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            JOIN PostBookmark b ON b.postId = p.id
            WHERE b.userId = :userId AND p.hidden = false
            ORDER BY b.createdAt DESC
            """)
    Page<Post> findBookmarkedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE p.hidden = false
            AND (lower(p.body) LIKE lower(concat('%', :query, '%'))
               OR lower(p.title) LIKE lower(concat('%', :query, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> search(@Param("query") String query, Pageable pageable);

    // Para la pantalla de moderación de publicaciones (CURATOR+): a diferencia de search(), esto
    // NO filtra hidden — el staff necesita poder encontrar un post ya oculto para des-ocultarlo.
    @Query("""
            SELECT p FROM Post p
            WHERE lower(p.body) LIKE lower(concat('%', :query, '%'))
               OR lower(p.title) LIKE lower(concat('%', :query, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> searchForAdmin(@Param("query") String query, Pageable pageable);
}
