package com.menzo.menzo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.communities.CommunityDetailDto;
import com.menzo.menzo.dto.communities.CommunityMembershipDto;
import com.menzo.menzo.dto.communities.CommunitySummaryDto;
import com.menzo.menzo.dto.communities.MyCommunityDto;
import com.menzo.menzo.dto.communities.UpdateCommunityAppearanceRequest;
import com.menzo.menzo.service.CommunitiesService;

/**
 * `/api/communities` (plural) — deliberadamente separado de `/api/community/*` (singular,
 * {@link CommunityController}, ya existente: la config/eventos/notificaciones de "Menzo" como
 * plataforma única). Este controller es el nuevo dominio multi-comunidad (Naruto, Anime, etc.),
 * ver Community.java para el porqué completo de la separación de nombres.
 */
@RestController
@RequestMapping("/api/communities")
public class CommunitiesController {

    private final CommunitiesService communitiesService;

    public CommunitiesController(CommunitiesService communitiesService) {
        this.communitiesService = communitiesService;
    }

    @GetMapping
    public PageResponse<CommunitySummaryDto> list(
            @PageableDefault(size = 20) Pageable pageable, @AuthenticationPrincipal User viewer) {
        return communitiesService.list(pageable, viewer);
    }

    @GetMapping("/discover")
    public PageResponse<CommunitySummaryDto> discover(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User viewer) {
        return communitiesService.discover(query, pageable, viewer);
    }

    @GetMapping("/my")
    public List<MyCommunityDto> my(@AuthenticationPrincipal User me) {
        return communitiesService.my(me);
    }

    @GetMapping("/{slug}")
    public CommunityDetailDto getBySlug(@PathVariable String slug, @AuthenticationPrincipal User viewer) {
        return communitiesService.getBySlug(slug, viewer);
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityMembershipDto join(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return communitiesService.join(id, me);
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        communitiesService.leave(id, me);
    }

    /** COMMUNITY_ADMIN+ de esa comunidad, o staff global LEADER+ — ver
     * CommunityPermissionEvaluator.requireCanEditAppearance. */
    @PatchMapping("/{id}/appearance")
    public CommunityDetailDto updateAppearance(
            @PathVariable UUID id,
            @AuthenticationPrincipal User me,
            @RequestBody UpdateCommunityAppearanceRequest request) {
        return communitiesService.updateAppearance(id, me, request);
    }

    /** COMMUNITY_ADMIN+ aprueba/rechaza una solicitud de una comunidad con accessType=REQUEST
     * (la membresía ya existe en PENDING desde join()). */
    @PostMapping("/{id}/members/{userId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveMembership(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        communitiesService.approveMembership(id, userId, me);
    }

    @PostMapping("/{id}/members/{userId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectMembership(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        communitiesService.rejectMembership(id, userId, me);
    }
}
