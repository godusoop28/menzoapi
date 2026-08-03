package com.menzo.menzo.repository.communities;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.communities.CommunityMembership;

public interface CommunityMembershipRepository
        extends JpaRepository<CommunityMembership, CommunityMembership.CommunityMembershipId> {

    Optional<CommunityMembership> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    boolean existsByCommunityIdAndUserId(UUID communityId, UUID userId);

    List<CommunityMembership> findByUserId(UUID userId);

    Page<CommunityMembership> findByCommunityId(UUID communityId, Pageable pageable);

    long countByCommunityId(UUID communityId);
}
