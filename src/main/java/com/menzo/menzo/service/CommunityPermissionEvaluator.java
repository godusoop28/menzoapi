package com.menzo.menzo.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.menzo.menzo.domain.communities.Community;
import com.menzo.menzo.domain.communities.CommunityMembership;
import com.menzo.menzo.domain.communities.CommunityMembershipStatus;
import com.menzo.menzo.domain.communities.CommunityRole;
import com.menzo.menzo.domain.communities.CommunityVisibility;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.repository.communities.CommunityMembershipRepository;

/**
 * Punto único de validación de permisos de comunidad — ninguna pantalla/cliente decide esto
 * ocultando botones, todo pasa por acá en el backend (ver Contexto §19/29 del pedido). Fase A
 * solo cubre lo que join/leave/detalle necesitan hoy (ver membresía, unirse, rol mínimo);
 * publicar/moderar/administrar apariencia se agregan progresivamente en Fases C/D reusando este
 * mismo servicio, no uno paralelo.
 */
@Service
public class CommunityPermissionEvaluator {

    private final CommunityMembershipRepository membershipRepository;

    public CommunityPermissionEvaluator(CommunityMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Optional<CommunityMembership> findActiveMembership(UUID communityId, UUID userId) {
        return membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(m -> m.getMembershipStatus() == CommunityMembershipStatus.ACTIVE);
    }

    public boolean isActiveMember(UUID communityId, UUID userId) {
        return findActiveMembership(communityId, userId).isPresent();
    }

    /** PUBLIC/UNLISTED: cualquiera con sesión puede ver el detalle. PRIVATE: solo miembros
     * activos (o quien ya tiene una solicitud/invitación en curso — eso se afina en Fase B junto
     * con CommunityJoinRequest). */
    public boolean canView(Community community, User viewer) {
        if (community.getVisibility() != CommunityVisibility.PRIVATE) {
            return true;
        }
        return viewer != null && isActiveMember(community.getId(), viewer.getId());
    }

    public void requireCanView(Community community, User viewer) {
        if (!canView(community, viewer)) {
            throw new ForbiddenException("No tenés acceso a esta comunidad");
        }
    }

    public void requireActiveMember(UUID communityId, User user) {
        if (!isActiveMember(communityId, user.getId())) {
            throw new ForbiddenException("No sos miembro de esta comunidad");
        }
    }

    /** ordinal() da "al menos este nivel" — mismo patrón que AdminAuthorizationService con el
     * Role global. */
    public void requireMinRole(UUID communityId, User user, CommunityRole minRole) {
        CommunityRole role = findActiveMembership(communityId, user.getId())
                .map(CommunityMembership::getCommunityRole)
                .orElse(null);
        if (role == null || role.ordinal() < minRole.ordinal()) {
            throw new ForbiddenException("No tenés permisos suficientes en esta comunidad");
        }
    }
}
