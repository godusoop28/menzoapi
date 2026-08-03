package com.menzo.menzo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.communities.Community;
import com.menzo.menzo.domain.communities.CommunityAccessType;
import com.menzo.menzo.domain.communities.CommunityMembership;
import com.menzo.menzo.domain.communities.CommunityMembershipStatus;
import com.menzo.menzo.domain.communities.CommunityRole;
import com.menzo.menzo.domain.communities.CommunityStatus;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.communities.CommunityDetailDto;
import com.menzo.menzo.dto.communities.CommunityMembershipDto;
import com.menzo.menzo.dto.communities.CommunitySummaryDto;
import com.menzo.menzo.dto.communities.MyCommunityDto;
import com.menzo.menzo.dto.communities.UpdateCommunityAppearanceRequest;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.communities.CommunityMembershipRepository;
import com.menzo.menzo.repository.communities.CommunityRepository;

/**
 * Fase A: listar/descubrir/mis-comunidades/detalle/unirse/abandonar. Publicar contenido,
 * moderar y administrar apariencia llegan en Fases C/D reusando {@link Community}/
 * {@link CommunityMembership} tal cual, sin volver a tocar este shape (ver Contexto §41).
 *
 * Nunca toca User.level/xp/reputation — el nivel es y sigue siendo enteramente global (ver
 * Contexto §6): pertenecer a N comunidades no crea N niveles, solo N filas de membresía sin
 * ningún campo de progreso/nivel.
 */
@Service
public class CommunitiesService {

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityPermissionEvaluator permissionEvaluator;

    public CommunitiesService(
            CommunityRepository communityRepository,
            CommunityMembershipRepository membershipRepository,
            CommunityPermissionEvaluator permissionEvaluator) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.permissionEvaluator = permissionEvaluator;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunitySummaryDto> list(Pageable pageable, User viewer) {
        Page<Community> page = communityRepository
                .findByStatusAndDiscoverableTrueOrderBySortOrderAsc(CommunityStatus.ACTIVE, pageable);
        return PageResponse.of(page, c -> toSummary(c, viewer));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunitySummaryDto> discover(String query, Pageable pageable, User viewer) {
        if (query == null || query.isBlank()) {
            return list(pageable, viewer);
        }
        Page<Community> page = communityRepository.search(CommunityStatus.ACTIVE, query.trim(), pageable);
        return PageResponse.of(page, c -> toSummary(c, viewer));
    }

    @Transactional(readOnly = true)
    public List<MyCommunityDto> my(User me) {
        return communityRepository.findActiveCommunitiesForUser(me.getId()).stream()
                .map(c -> {
                    CommunityMembership membership = membershipRepository
                            .findByCommunityIdAndUserId(c.getId(), me.getId())
                            .orElseThrow();
                    return new MyCommunityDto(toSummary(c, me), toMembershipDto(membership));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityDetailDto getBySlug(String slug, User viewer) {
        Community community = requireBySlug(slug);
        permissionEvaluator.requireCanView(community, viewer);
        return toDetail(community, viewer);
    }

    /** COMMUNITY_ADMIN+ de esa comunidad, o staff global LEADER+ (ver
     * CommunityPermissionEvaluator.requireCanEditAppearance). Cada campo es null = sin cambios,
     * "" = limpiar — mismo criterio que ChatService.updateRoom con avatarUri/coverUri. */
    @Transactional
    public CommunityDetailDto updateAppearance(UUID communityId, User actor, UpdateCommunityAppearanceRequest request) {
        Community community = requireById(communityId);
        permissionEvaluator.requireCanEditAppearance(communityId, actor);

        if (request.iconUrl() != null) community.setIconUrl(request.iconUrl().isBlank() ? null : request.iconUrl());
        if (request.logoUrl() != null) community.setLogoUrl(request.logoUrl().isBlank() ? null : request.logoUrl());
        if (request.coverUrl() != null) community.setCoverUrl(request.coverUrl().isBlank() ? null : request.coverUrl());
        if (request.backgroundUrl() != null) {
            community.setBackgroundUrl(request.backgroundUrl().isBlank() ? null : request.backgroundUrl());
        }
        if (request.bannerUrl() != null) community.setBannerUrl(request.bannerUrl().isBlank() ? null : request.bannerUrl());
        if (request.primaryColor() != null) {
            community.setPrimaryColor(request.primaryColor().isBlank() ? null : request.primaryColor());
        }
        if (request.secondaryColor() != null) {
            community.setSecondaryColor(request.secondaryColor().isBlank() ? null : request.secondaryColor());
        }
        if (request.accentColor() != null) {
            community.setAccentColor(request.accentColor().isBlank() ? null : request.accentColor());
        }

        community = communityRepository.save(community);
        return toDetail(community, actor);
    }

    /** OPEN: se une de inmediato como ACTIVE. REQUEST: queda PENDING (todavía no existe una
     * pantalla de aprobación — Fase B — pero la fila ya se crea con el estado correcto, sin
     * necesitar tocar el modelo cuando esa pantalla exista). INVITE_ONLY: no se puede auto-unir. */
    @Transactional
    public CommunityMembershipDto join(UUID communityId, User me) {
        Community community = requireById(communityId);
        if (community.getAccessType() == CommunityAccessType.INVITE_ONLY) {
            throw new ForbiddenException("Esta comunidad es solo por invitación");
        }
        if (me.getLevel() < community.getMinimumGlobalLevelToJoin()) {
            throw new ForbiddenException("Necesitás un nivel más alto para unirte a esta comunidad");
        }

        CommunityMembership existing = membershipRepository
                .findByCommunityIdAndUserId(communityId, me.getId())
                .orElse(null);
        if (existing != null && existing.getMembershipStatus() == CommunityMembershipStatus.ACTIVE) {
            throw new ConflictException("Ya sos miembro de esta comunidad");
        }
        if (existing != null && existing.getMembershipStatus() == CommunityMembershipStatus.BANNED) {
            throw new ForbiddenException("No podés volver a unirte a esta comunidad");
        }

        boolean opensDirectly = community.getAccessType() == CommunityAccessType.OPEN;
        CommunityMembershipStatus nextStatus = opensDirectly
                ? CommunityMembershipStatus.ACTIVE
                : CommunityMembershipStatus.PENDING;

        CommunityMembership membership = existing != null ? existing : new CommunityMembership(communityId, me.getId());
        boolean wasActive = existing != null && existing.getMembershipStatus() == CommunityMembershipStatus.ACTIVE;
        membership.setMembershipStatus(nextStatus);
        membership = membershipRepository.save(membership);

        if (opensDirectly && !wasActive) {
            community.setMemberCount(community.getMemberCount() + 1);
            communityRepository.save(community);
        }
        return toMembershipDto(membership);
    }

    @Transactional
    public void leave(UUID communityId, User me) {
        CommunityMembership membership = membershipRepository
                .findByCommunityIdAndUserId(communityId, me.getId())
                .orElseThrow(() -> new NotFoundException("No sos miembro de esta comunidad"));
        if (membership.getCommunityRole().name().equals("COMMUNITY_OWNER")) {
            throw new BadRequestException("El dueño de la comunidad no puede abandonarla — transferí la propiedad primero");
        }
        boolean wasActive = membership.getMembershipStatus() == CommunityMembershipStatus.ACTIVE;
        membership.setMembershipStatus(CommunityMembershipStatus.LEFT);
        membershipRepository.save(membership);

        if (wasActive) {
            Community community = requireById(communityId);
            community.setMemberCount(Math.max(0, community.getMemberCount() - 1));
            communityRepository.save(community);
        }
    }

    /** Cierra el flujo de acceso REQUEST: la membresía ya quedó creada en PENDING por join()
     * (ver ahí el porqué de no tener una entidad CommunityJoinRequest aparte todavía). */
    @Transactional
    public void approveMembership(UUID communityId, UUID targetUserId, User actor) {
        permissionEvaluator.requireMinRole(communityId, actor, CommunityRole.COMMUNITY_ADMIN);
        CommunityMembership membership = requirePendingMembership(communityId, targetUserId);
        membership.setMembershipStatus(CommunityMembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        Community community = requireById(communityId);
        community.setMemberCount(community.getMemberCount() + 1);
        communityRepository.save(community);
    }

    @Transactional
    public void rejectMembership(UUID communityId, UUID targetUserId, User actor) {
        permissionEvaluator.requireMinRole(communityId, actor, CommunityRole.COMMUNITY_ADMIN);
        CommunityMembership membership = requirePendingMembership(communityId, targetUserId);
        membership.setMembershipStatus(CommunityMembershipStatus.REMOVED);
        membershipRepository.save(membership);
    }

    private CommunityMembership requirePendingMembership(UUID communityId, UUID targetUserId) {
        CommunityMembership membership = membershipRepository
                .findByCommunityIdAndUserId(communityId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
        if (membership.getMembershipStatus() != CommunityMembershipStatus.PENDING) {
            throw new BadRequestException("Esa solicitud ya no está pendiente");
        }
        return membership;
    }

    private Community requireBySlug(String slug) {
        return communityRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new NotFoundException("Comunidad no encontrada"));
    }

    private Community requireById(UUID id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comunidad no encontrada"));
    }

    private CommunitySummaryDto toSummary(Community c, User viewer) {
        boolean joined = viewer != null && permissionEvaluator.isActiveMember(c.getId(), viewer.getId());
        return new CommunitySummaryDto(
                c.getId(),
                c.getSlug(),
                c.getName(),
                c.getShortDescription(),
                c.getCategory(),
                c.getTags().stream().sorted().toList(),
                c.getIconUrl(),
                c.getLogoUrl(),
                c.getCoverUrl(),
                c.getPrimaryColor(),
                c.getSecondaryColor(),
                c.getAccentColor(),
                c.getMemberCount(),
                c.isFeatured(),
                c.isOfficial(),
                c.getVisibility().name(),
                c.getAccessType().name(),
                joined);
    }

    private CommunityDetailDto toDetail(Community c, User viewer) {
        CommunityMembershipDto myMembership = viewer == null ? null : membershipRepository
                .findByCommunityIdAndUserId(c.getId(), viewer.getId())
                .map(this::toMembershipDto)
                .orElse(null);
        return new CommunityDetailDto(
                c.getId(),
                c.getSlug(),
                c.getName(),
                c.getShortDescription(),
                c.getFullDescription(),
                c.getStatus().name(),
                c.getVisibility().name(),
                c.getAccessType().name(),
                c.getPrimaryLanguage(),
                c.getCategory(),
                c.getTags().stream().sorted().toList(),
                c.getIconUrl(),
                c.getLogoUrl(),
                c.getCoverUrl(),
                c.getBackgroundUrl(),
                c.getBannerUrl(),
                c.getPrimaryColor(),
                c.getSecondaryColor(),
                c.getAccentColor(),
                c.getTextColor(),
                c.getSurfaceColor(),
                c.getMemberCount(),
                c.getOnlineMemberCount(),
                c.getPostCount(),
                c.getChatCount(),
                c.isFeatured(),
                c.isOfficial(),
                c.isAllowJoinRequests(),
                c.isAllowPublicChats(),
                c.isAllowBlogs(),
                c.isAllowVoiceRooms(),
                c.isAllowMemberPosts(),
                c.getMinimumGlobalLevelToJoin(),
                c.getMinimumGlobalLevelToPost(),
                c.getCreatedAt(),
                myMembership);
    }

    private CommunityMembershipDto toMembershipDto(CommunityMembership m) {
        return new CommunityMembershipDto(
                m.getCommunityRole().name(),
                m.getMembershipStatus().name(),
                m.getJoinedAt(),
                m.getLastVisitedAt(),
                m.isFavorite(),
                m.isMuted(),
                m.getCommunityNickname(),
                m.getCommunityBio(),
                m.getContributionCount(),
                m.getCustomTitle());
    }
}
