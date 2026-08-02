package com.menzo.menzo.service.mapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.menzo.menzo.domain.user.Interest;
import com.menzo.menzo.domain.user.RelationshipStatus;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.domain.user.UserBadge;
import com.menzo.menzo.dto.user.UserProfileResponse;
import com.menzo.menzo.dto.user.UserSummary;
import com.menzo.menzo.repository.user.FollowRepository;
import com.menzo.menzo.repository.user.ProfileVisitRepository;

@Component
public class ProfileMapper {

    private final FollowRepository followRepository;
    private final ProfileVisitRepository profileVisitRepository;

    public ProfileMapper(FollowRepository followRepository, ProfileVisitRepository profileVisitRepository) {
        this.followRepository = followRepository;
        this.profileVisitRepository = profileVisitRepository;
    }

    public UserProfileResponse toProfile(User user, UUID viewerId) {
        long followers = followRepository.countByFollowingId(user.getId());
        long following = followRepository.countByFollowerId(user.getId());
        long visitors = profileVisitRepository.countByProfileId(user.getId());
        boolean isSelf = viewerId != null && viewerId.equals(user.getId());
        boolean followedByMe = !isSelf && viewerId != null
                && followRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId());
        boolean followsMe = !isSelf && viewerId != null
                && followRepository.existsByFollowerIdAndFollowingId(user.getId(), viewerId);

        return buildResponse(user, followers, following, visitors, isSelf, followedByMe, followsMe);
    }

    /** Misma respuesta que toProfile, pero para una lista de usuarios en un puñado de consultas
     * fijo en vez de 5 consultas por usuario (seguidores, siguiendo, visitas, sigo-yo, me-sigue) —
     * usado por los listados de seguidores/siguiendo/búsqueda, que antes hacían exactamente eso. */
    public List<UserProfileResponse> toProfiles(List<User> users, UUID viewerId) {
        if (users.isEmpty()) return List.of();
        List<UUID> ids = users.stream().map(User::getId).toList();

        Map<UUID, Long> followerCounts = toCountMap(followRepository.countFollowersForUsers(ids));
        Map<UUID, Long> followingCounts = toCountMap(followRepository.countFollowingForUsers(ids));
        Map<UUID, Long> visitorCounts = toCountMap(profileVisitRepository.countVisitorsForProfiles(ids));

        Set<UUID> followedByMeIds = viewerId != null
                ? new HashSet<>(followRepository.findFollowedByViewerAmong(viewerId, ids))
                : Set.of();
        Set<UUID> followsMeIds = viewerId != null
                ? new HashSet<>(followRepository.findFollowersOfViewerAmong(viewerId, ids))
                : Set.of();

        return users.stream()
                .map(user -> {
                    boolean isSelf = viewerId != null && viewerId.equals(user.getId());
                    boolean followedByMe = !isSelf && followedByMeIds.contains(user.getId());
                    boolean followsMe = !isSelf && followsMeIds.contains(user.getId());
                    return buildResponse(
                            user,
                            followerCounts.getOrDefault(user.getId(), 0L),
                            followingCounts.getOrDefault(user.getId(), 0L),
                            visitorCounts.getOrDefault(user.getId(), 0L),
                            isSelf,
                            followedByMe,
                            followsMe);
                })
                .toList();
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], (Long) row[1]);
        }
        return map;
    }

    private UserProfileResponse buildResponse(
            User user, long followers, long following, long visitors, boolean isSelf, boolean followedByMe, boolean followsMe) {
        var interestIds = user.getInterests().stream().map(Interest::getId).sorted().toList();
        var badgeIds = user.getBadges().stream().map(UserBadge::getBadgeId).sorted().toList();
        RelationshipStatus relationshipStatus = RelationshipStatus.of(isSelf, followedByMe, followsMe);

        return new UserProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                user.getAvatarUri(),
                user.getAvatarGradient(),
                user.getCoverUri(),
                user.getBackgroundUri(),
                user.getBackgroundColor(),
                user.getAura().getId(),
                user.getBio(),
                user.getStatusText(),
                interestIds,
                user.getJoinedAt(),
                user.getLevel(),
                user.getXp(),
                user.getReputation(),
                followers,
                following,
                visitors,
                user.isOnline(),
                badgeIds,
                followedByMe,
                followsMe,
                followedByMe && followsMe,
                relationshipStatus,
                user.getRole().name());
    }

    public UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                user.getAvatarUri(),
                user.getAvatarGradient(),
                user.isOnline());
    }
}
