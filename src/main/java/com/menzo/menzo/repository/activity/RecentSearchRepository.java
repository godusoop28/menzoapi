package com.menzo.menzo.repository.activity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.activity.RecentSearch;

public interface RecentSearchRepository extends JpaRepository<RecentSearch, UUID> {

    List<RecentSearch> findTop8ByUserIdOrderBySearchedAtDesc(UUID userId);

    @Transactional
    void deleteByUserIdAndQueryIgnoreCase(UUID userId, String query);

    @Transactional
    void deleteByUserId(UUID userId);
}
