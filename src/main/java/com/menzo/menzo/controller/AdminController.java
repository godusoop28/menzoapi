package com.menzo.menzo.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.moderation.ModerationActionType;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.moderation.ChangeRoleRequest;
import com.menzo.menzo.dto.moderation.ModerationActionResponse;
import com.menzo.menzo.dto.moderation.ReasonRequest;
import com.menzo.menzo.dto.post.PostResponse;
import com.menzo.menzo.dto.user.UserProfileResponse;
import com.menzo.menzo.service.AdminAuthorizationService;
import com.menzo.menzo.service.AdminService;
import com.menzo.menzo.service.ModerationLogService;
import com.menzo.menzo.service.PostService;

import jakarta.validation.Valid;

/**
 * Panel de staff global (CURATOR/LEADER/MASTER) — usuarios, publicaciones y roles. Nunca expone
 * un endpoint para navegar mensajes/salas: el borrado de mensajes sigue viviendo en
 * /api/chat/rooms/{id}/messages/{messageId} (ChatController), donde el moderador ya necesita
 * tener visibilidad del mensaje como miembro de una sala pública — así MASTER nunca tiene una
 * pantalla desde la que "buscar" un chat DIRECT (ver Contexto/decisión #3 del plan).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final PostService postService;
    private final ModerationLogService moderationLogService;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminController(
            AdminService adminService,
            PostService postService,
            ModerationLogService moderationLogService,
            AdminAuthorizationService adminAuthorizationService) {
        this.adminService = adminService;
        this.postService = postService;
        this.moderationLogService = moderationLogService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @GetMapping("/users")
    public PageResponse<UserProfileResponse> searchUsers(
            @RequestParam(defaultValue = "") String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User me) {
        return adminService.searchUsers(me, query, pageable);
    }

    @PostMapping("/users/{id}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendUser(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody ReasonRequest request) {
        adminService.suspendUser(me, id, request.reason());
    }

    @PostMapping("/users/{id}/unsuspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsuspendUser(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody ReasonRequest request) {
        adminService.unsuspendUser(me, id, request.reason());
    }

    /** MASTER-only (ver AdminService.deleteAccount → requireMaster) — anonimiza, no borra de la
     * base. */
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody ReasonRequest request) {
        adminService.deleteAccount(me, id, request.reason());
    }

    @PostMapping("/users/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody ChangeRoleRequest request) {
        adminService.changeRole(me, id, request.role(), request.reason());
    }

    @GetMapping("/posts")
    public PageResponse<PostResponse> searchPosts(
            @RequestParam(defaultValue = "") String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User me) {
        return postService.searchForAdmin(me, query, pageable);
    }

    @PostMapping("/posts/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hidePost(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody ReasonRequest request) {
        postService.hidePost(me, id, request.reason());
    }

    @PostMapping("/posts/{id}/unhide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unhidePost(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody ReasonRequest request) {
        postService.unhidePost(me, id, request.reason());
    }

    /** MASTER-only — LEADER/CURATOR actúan con motivo, pero solo MASTER puede revisar el
     * historial completo (ver Contexto/decisión #5 del plan). */
    @GetMapping("/moderation-log")
    public PageResponse<ModerationActionResponse> moderationLog(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) ModerationActionType actionType,
            @PageableDefault(size = 30) Pageable pageable,
            @AuthenticationPrincipal User me) {
        adminAuthorizationService.requireMaster(me);
        return moderationLogService.search(actorId, actionType, pageable);
    }
}
