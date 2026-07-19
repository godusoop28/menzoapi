package com.menzo.menzo.service.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.menzo.menzo.domain.user.Interest;
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
        boolean followedByMe = viewerId != null
                && !viewerId.equals(user.getId())
                && followRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId());

        var interestIds = user.getInterests().stream().map(Interest::getId).sorted().toList();
        var badgeIds = user.getBadges().stream().map(UserBadge::getBadgeId).sorted().toList();

        return new UserProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                user.getAvatarUri(),
                user.getAvatarGradient(),
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
                followedByMe);
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
