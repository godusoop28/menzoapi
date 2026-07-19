package com.menzo.menzo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.community.CommunityConfigResponse;
import com.menzo.menzo.dto.community.CreateEventRequest;
import com.menzo.menzo.dto.community.EventResponse;
import com.menzo.menzo.dto.community.NotificationResponse;
import com.menzo.menzo.service.CommunityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/community/config")
    public CommunityConfigResponse config() {
        return communityService.getConfig();
    }

    @GetMapping("/community/events")
    public List<EventResponse> listEvents(@AuthenticationPrincipal User viewer) {
        return communityService.listEvents(viewer);
    }

    @GetMapping("/community/events/{id}")
    public EventResponse getEvent(@PathVariable UUID id, @AuthenticationPrincipal User viewer) {
        return communityService.getEvent(id, viewer);
    }

    @PostMapping("/community/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@AuthenticationPrincipal User me, @Valid @RequestBody CreateEventRequest request) {
        return communityService.createEvent(me, request);
    }

    @PutMapping("/community/events/{id}/attend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attend(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        communityService.attendEvent(me, id);
    }

    @DeleteMapping("/community/events/{id}/attend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unattend(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        communityService.unattendEvent(me, id);
    }

    @GetMapping("/notifications")
    public PageResponse<NotificationResponse> notifications(
            @AuthenticationPrincipal User me, @PageableDefault(size = 30) Pageable pageable) {
        return communityService.listNotifications(me, pageable);
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        communityService.markNotificationRead(me, id);
    }

    @PostMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal User me) {
        communityService.markAllNotificationsRead(me);
    }
}
