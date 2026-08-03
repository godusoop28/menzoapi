package com.menzo.menzo.repository.communities;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.communities.Community;
import com.menzo.menzo.domain.communities.CommunityStatus;

public interface CommunityRepository extends JpaRepository<Community, UUID> {

    Optional<Community> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    Page<Community> findByStatusAndDiscoverableTrueOrderBySortOrderAsc(CommunityStatus status, Pageable pageable);

    @Query("""
            SELECT c FROM Community c
            WHERE c.status = :status AND c.discoverable = true
            AND (lower(c.name) LIKE lower(concat('%', :query, '%'))
               OR lower(c.category) LIKE lower(concat('%', :query, '%')))
            ORDER BY c.sortOrder ASC
            """)
    Page<Community> search(@Param("status") CommunityStatus status, @Param("query") String query, Pageable pageable);

    @Query("""
            SELECT c FROM Community c
            JOIN CommunityMembership m ON m.communityId = c.id
            WHERE m.userId = :userId AND m.membershipStatus = com.menzo.menzo.domain.communities.CommunityMembershipStatus.ACTIVE
            ORDER BY COALESCE(m.lastVisitedAt, m.joinedAt) DESC
            """)
    List<Community> findActiveCommunitiesForUser(@Param("userId") UUID userId);
}
