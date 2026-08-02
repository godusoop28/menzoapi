package com.menzo.menzo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.moderation.ModerationActionType;
import com.menzo.menzo.domain.user.Role;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.user.UserProfileResponse;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.auth.RefreshTokenRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;

/**
 * Gestión de staff global: suspensión/reactivación de cuentas (LEADER+, reversible), eliminación
 * completa de cuentas (MASTER-only, anonimiza en vez de borrar de la base — ver
 * Contexto/decisión #2 del plan) y asignación de roles CURATOR/LEADER (LEADER+, nunca MASTER vía
 * UI/API — MASTER queda fijo a la única cuenta configurada en menzo.admin.master-email).
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProfileMapper profileMapper;
    private final AdminAuthorizationService adminAuthorizationService;
    private final ModerationLogService moderationLogService;

    public AdminService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            ProfileMapper profileMapper,
            AdminAuthorizationService adminAuthorizationService,
            ModerationLogService moderationLogService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.profileMapper = profileMapper;
        this.adminAuthorizationService = adminAuthorizationService;
        this.moderationLogService = moderationLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> searchUsers(User me, String query, Pageable pageable) {
        adminAuthorizationService.requireCurator(me);
        Page<User> page = userRepository.search(query, pageable);
        return PageResponse.of(page, user -> profileMapper.toProfile(user, me.getId()));
    }

    /** Suspensión de plataforma (LEADER+) — bloquea el login, los datos quedan intactos. Distinto
     * y reversible, a diferencia del borrado completo de MASTER. */
    @Transactional
    public void suspendUser(User me, UUID targetUserId, String reason) {
        adminAuthorizationService.requireLeader(me);
        User target = requireUser(targetUserId);
        requireNotSelf(me, targetUserId);
        requireCannotTargetMaster(target);
        target.setSuspended(true);
        userRepository.save(target);
        refreshTokenRepository.revokeAllForUser(targetUserId);
        moderationLogService.record(me, ModerationActionType.SUSPEND_USER, "USER", targetUserId, reason);
    }

    @Transactional
    public void unsuspendUser(User me, UUID targetUserId, String reason) {
        adminAuthorizationService.requireLeader(me);
        User target = requireUser(targetUserId);
        target.setSuspended(false);
        userRepository.save(target);
        moderationLogService.record(me, ModerationActionType.UNSUSPEND_USER, "USER", targetUserId, reason);
    }

    /** MASTER-only. Anonimiza en vez de borrar de la base: la cuenta queda permanentemente
     * inutilizable (enabled=false, passwordHash inservible, email/username reescritos a
     * placeholders únicos y estables), pero sus posts/mensajes en salas públicas siguen visibles,
     * atribuidos a la identidad anonimizada — así no se rompen las respuestas/likes/historial de
     * otras personas. Mismo id, para que ninguna FK quede huérfana. Revoca toda sesión activa. */
    @Transactional
    public void deleteAccount(User me, UUID targetUserId, String reason) {
        adminAuthorizationService.requireMaster(me);
        User target = requireUser(targetUserId);
        requireNotSelf(me, targetUserId);

        String placeholder = "deleted_" + target.getId().toString().substring(0, 8);
        target.setDisplayName("Usuario eliminado");
        target.setUsername(uniqueDeletedUsername(placeholder));
        target.setEmail(placeholder + "@deleted.menzo.local");
        target.setAvatarUri(null);
        target.setCoverUri(null);
        target.setBackgroundUri(null);
        target.setBio("");
        target.setStatusText("");
        target.setPasswordHash("!disabled!");
        target.setEnabled(false);
        userRepository.save(target);

        refreshTokenRepository.revokeAllForUser(targetUserId);
        moderationLogService.record(me, ModerationActionType.DELETE_ACCOUNT, "USER", targetUserId, reason);
    }

    /** LEADER puede otorgar/revocar CURATOR y LEADER; MASTER también. Nunca se puede asignar
     * MASTER acá — esa cuenta queda fija a la única configurada en menzo.admin.master-email
     * (ver MasterAccountBootstrap/AuthService.register). */
    @Transactional
    public void changeRole(User me, UUID targetUserId, Role newRole, String reason) {
        adminAuthorizationService.requireLeader(me);
        if (newRole == Role.MASTER) {
            throw new BadRequestException("El rol MASTER no se puede asignar desde acá");
        }
        User target = requireUser(targetUserId);
        requireNotSelf(me, targetUserId);
        requireCannotTargetMaster(target);
        Role previousRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);
        ModerationActionType type = newRole.ordinal() > previousRole.ordinal()
                ? ModerationActionType.PROMOTE_ROLE
                : ModerationActionType.DEMOTE_ROLE;
        moderationLogService.record(me, type, "USER", targetUserId, reason);
    }

    private String uniqueDeletedUsername(String base) {
        String candidate = base;
        int suffix = 0;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private void requireNotSelf(User me, UUID targetUserId) {
        if (me.getId().equals(targetUserId)) {
            throw new BadRequestException("No podés aplicarte esta acción a vos mismo");
        }
    }

    private void requireCannotTargetMaster(User target) {
        if (target.getRole() == Role.MASTER) {
            throw new ForbiddenException("No se puede moderar a la cuenta maestra");
        }
    }
}
