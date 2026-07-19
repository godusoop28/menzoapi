package com.menzo.menzo.repository.activity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.activity.ActivityKind;
import com.menzo.menzo.domain.activity.RecentlyViewed;

public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, UUID> {

    List<RecentlyViewed> findTop20ByUserIdOrderByViewedAtDesc(UUID userId);

    @Transactional
    void deleteByUserIdAndKindAndTargetId(UUID userId, ActivityKind kind, UUID targetId);
}
