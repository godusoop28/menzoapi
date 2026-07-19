package com.menzo.menzo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.activity.RecentSearch;
import com.menzo.menzo.domain.activity.RecentlyViewed;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.activity.RecentSearchRequest;
import com.menzo.menzo.dto.activity.RecentlyViewedRequest;
import com.menzo.menzo.dto.activity.RecentlyViewedResponse;
import com.menzo.menzo.repository.activity.RecentSearchRepository;
import com.menzo.menzo.repository.activity.RecentlyViewedRepository;

@Service
public class ActivityService {

    private final RecentlyViewedRepository recentlyViewedRepository;
    private final RecentSearchRepository recentSearchRepository;

    public ActivityService(RecentlyViewedRepository recentlyViewedRepository, RecentSearchRepository recentSearchRepository) {
        this.recentlyViewedRepository = recentlyViewedRepository;
        this.recentSearchRepository = recentSearchRepository;
    }

    @Transactional
    public void addRecentlyViewed(User me, RecentlyViewedRequest request) {
        recentlyViewedRepository.deleteByUserIdAndKindAndTargetId(me.getId(), request.kind(), request.id());
        recentlyViewedRepository.save(new RecentlyViewed(me.getId(), request.kind(), request.id()));
    }

    public List<RecentlyViewedResponse> listRecentlyViewed(User me) {
        return recentlyViewedRepository.findTop20ByUserIdOrderByViewedAtDesc(me.getId()).stream()
                .map(rv -> new RecentlyViewedResponse(rv.getKind().name(), rv.getTargetId(), rv.getViewedAt()))
                .toList();
    }

    @Transactional
    public void addRecentSearch(User me, RecentSearchRequest request) {
        String query = request.query().trim();
        if (query.isEmpty()) {
            return;
        }
        recentSearchRepository.deleteByUserIdAndQueryIgnoreCase(me.getId(), query);
        recentSearchRepository.save(new RecentSearch(me.getId(), query));
    }

    public List<String> listRecentSearches(User me) {
        return recentSearchRepository.findTop8ByUserIdOrderBySearchedAtDesc(me.getId()).stream()
                .map(RecentSearch::getQuery)
                .toList();
    }

    @Transactional
    public void clearRecentSearches(User me) {
        recentSearchRepository.deleteByUserId(me.getId());
    }
}
