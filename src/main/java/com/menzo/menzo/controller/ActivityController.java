package com.menzo.menzo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.activity.RecentSearchRequest;
import com.menzo.menzo.dto.activity.RecentlyViewedRequest;
import com.menzo.menzo.dto.activity.RecentlyViewedResponse;
import com.menzo.menzo.service.ActivityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/recently-viewed")
    public List<RecentlyViewedResponse> recentlyViewed(@AuthenticationPrincipal User me) {
        return activityService.listRecentlyViewed(me);
    }

    @PostMapping("/recently-viewed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRecentlyViewed(@AuthenticationPrincipal User me, @Valid @RequestBody RecentlyViewedRequest request) {
        activityService.addRecentlyViewed(me, request);
    }

    @GetMapping("/recent-searches")
    public List<String> recentSearches(@AuthenticationPrincipal User me) {
        return activityService.listRecentSearches(me);
    }

    @PostMapping("/recent-searches")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRecentSearch(@AuthenticationPrincipal User me, @Valid @RequestBody RecentSearchRequest request) {
        activityService.addRecentSearch(me, request);
    }

    @DeleteMapping("/recent-searches")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearRecentSearches(@AuthenticationPrincipal User me) {
        activityService.clearRecentSearches(me);
    }
}
