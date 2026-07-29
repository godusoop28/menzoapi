package com.menzo.menzo.repository.user;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.user.ProfileVisit;

public interface ProfileVisitRepository extends JpaRepository<ProfileVisit, UUID> {

    long countByProfileId(UUID profileId);

    boolean existsByVisitorIdAndProfileId(UUID visitorId, UUID profileId);

    @Query("SELECT v.profileId, COUNT(v) FROM ProfileVisit v WHERE v.profileId IN :profileIds GROUP BY v.profileId")
    List<Object[]> countVisitorsForProfiles(@Param("profileIds") List<UUID> profileIds);
}
