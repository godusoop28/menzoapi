package com.menzo.menzo.repository.user;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.user.ProfileVisit;

public interface ProfileVisitRepository extends JpaRepository<ProfileVisit, UUID> {

    long countByProfileId(UUID profileId);

    boolean existsByVisitorIdAndProfileId(UUID visitorId, UUID profileId);
}
