package com.menzo.menzo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.user.AuraResponse;
import com.menzo.menzo.dto.user.BadgeResponse;
import com.menzo.menzo.dto.user.InterestResponse;
import com.menzo.menzo.dto.user.OnboardingRequest;
import com.menzo.menzo.dto.user.SettingsResponse;
import com.menzo.menzo.dto.user.UpdateProfileRequest;
import com.menzo.menzo.dto.user.UpdateSettingsRequest;
import com.menzo.menzo.dto.user.UserProfileResponse;
import com.menzo.menzo.dto.user.WallMessageRequest;
import com.menzo.menzo.dto.user.WallMessageResponse;
import com.menzo.menzo.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/me")
    public UserProfileResponse me(@AuthenticationPrincipal User me) {
        return userService.getProfile(me.getId(), me);
    }

    @PostMapping("/users/me/onboarding")
    public UserProfileResponse completeOnboarding(
            @AuthenticationPrincipal User me, @Valid @RequestBody OnboardingRequest request) {
        return userService.completeOnboarding(me, request);
    }

    @PatchMapping("/users/me")
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal User me, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(me, request);
    }

    @PostMapping("/users/me/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal User me) {
        userService.heartbeat(me);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/settings")
    public SettingsResponse getSettings(@AuthenticationPrincipal User me) {
        return userService.getSettings(me);
    }

    @PatchMapping("/users/me/settings")
    public SettingsResponse updateSettings(
            @AuthenticationPrincipal User me, @RequestBody UpdateSettingsRequest request) {
        return userService.updateSettings(me, request);
    }

    @GetMapping("/users/search")
    public PageResponse<UserProfileResponse> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User viewer) {
        return userService.search(query, pageable, viewer);
    }

    @GetMapping("/users/{id}")
    public UserProfileResponse getProfile(@PathVariable UUID id, @AuthenticationPrincipal User viewer) {
        return userService.getProfile(id, viewer);
    }

    @PutMapping("/users/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        userService.follow(me, id);
    }

    @DeleteMapping("/users/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        userService.unfollow(me, id);
    }

    @GetMapping("/users/{id}/followers")
    public List<UserProfileResponse> followers(@PathVariable UUID id, @AuthenticationPrincipal User viewer) {
        return userService.getFollowers(id, viewer);
    }

    @GetMapping("/users/{id}/following")
    public List<UserProfileResponse> following(@PathVariable UUID id, @AuthenticationPrincipal User viewer) {
        return userService.getFollowing(id, viewer);
    }

    @GetMapping("/users/{id}/wall")
    public PageResponse<WallMessageResponse> wall(
            @PathVariable UUID id, @PageableDefault(size = 20) Pageable pageable) {
        return userService.listWallMessages(id, pageable);
    }

    @PostMapping("/users/{id}/wall")
    @ResponseStatus(HttpStatus.CREATED)
    public WallMessageResponse addWallMessage(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody WallMessageRequest request) {
        return userService.addWallMessage(me, id, request);
    }

    @GetMapping("/lookups/auras")
    public List<AuraResponse> auras() {
        return userService.listAuras();
    }

    @GetMapping("/lookups/interests")
    public List<InterestResponse> interests() {
        return userService.listInterests();
    }

    @GetMapping("/lookups/badges")
    public List<BadgeResponse> badges() {
        return userService.listBadges();
    }
}
