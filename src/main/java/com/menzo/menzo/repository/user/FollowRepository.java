package com.menzo.menzo.repository.user;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.user.Follow;

public interface FollowRepository extends JpaRepository<Follow, Follow.FollowId> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    long countByFollowingId(UUID followingId);

    long countByFollowerId(UUID followerId);

    List<Follow> findByFollowerId(UUID followerId);

    List<Follow> findByFollowingId(UUID followingId);

    @Transactional
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    /** Conteo de seguidores para varios perfiles en una sola consulta — reemplaza N llamadas a
     * countByFollowingId cuando se listan varios usuarios (seguidores, siguiendo, búsqueda). */
    @Query("SELECT f.followingId, COUNT(f) FROM Follow f WHERE f.followingId IN :userIds GROUP BY f.followingId")
    List<Object[]> countFollowersForUsers(@Param("userIds") List<UUID> userIds);

    @Query("SELECT f.followerId, COUNT(f) FROM Follow f WHERE f.followerId IN :userIds GROUP BY f.followerId")
    List<Object[]> countFollowingForUsers(@Param("userIds") List<UUID> userIds);

    /** De la lista de userIds, cuáles sigue el viewer — una sola consulta en vez de un exists por
     * usuario. */
    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :viewerId AND f.followingId IN :userIds")
    List<UUID> findFollowedByViewerAmong(@Param("viewerId") UUID viewerId, @Param("userIds") List<UUID> userIds);

    /** De la lista de userIds, cuáles siguen al viewer. */
    @Query("SELECT f.followerId FROM Follow f WHERE f.followingId = :viewerId AND f.followerId IN :userIds")
    List<UUID> findFollowersOfViewerAmong(@Param("viewerId") UUID viewerId, @Param("userIds") List<UUID> userIds);
}
