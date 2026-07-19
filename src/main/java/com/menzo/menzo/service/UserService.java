package com.menzo.menzo.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.Message;
import com.menzo.menzo.domain.chat.MessageType;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.chat.WallMessage;
import com.menzo.menzo.domain.community.Notification;
import com.menzo.menzo.domain.community.NotificationCategory;
import com.menzo.menzo.domain.user.Aura;
import com.menzo.menzo.domain.user.Follow;
import com.menzo.menzo.domain.user.Interest;
import com.menzo.menzo.domain.user.ProfileVisit;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.domain.user.UserBadge;
import com.menzo.menzo.domain.user.UserSettings;
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
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.MessageRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.chat.WallMessageRepository;
import com.menzo.menzo.repository.community.NotificationRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.BadgeRepository;
import com.menzo.menzo.repository.user.FollowRepository;
import com.menzo.menzo.repository.user.InterestRepository;
import com.menzo.menzo.repository.user.ProfileVisitRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.repository.user.UserSettingsRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;

@Service
public class UserService {

    private static final String MAIN_ROOM_SLUG = "main";
    private static final String NEWCOMER_BADGE_ID = "recien-llegado";

    private final UserRepository userRepository;
    private final AuraRepository auraRepository;
    private final InterestRepository interestRepository;
    private final BadgeRepository badgeRepository;
    private final FollowRepository followRepository;
    private final ProfileVisitRepository profileVisitRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final WallMessageRepository wallMessageRepository;
    private final ProfileMapper profileMapper;

    public UserService(
            UserRepository userRepository,
            AuraRepository auraRepository,
            InterestRepository interestRepository,
            BadgeRepository badgeRepository,
            FollowRepository followRepository,
            ProfileVisitRepository profileVisitRepository,
            UserSettingsRepository userSettingsRepository,
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository,
            MessageRepository messageRepository,
            NotificationRepository notificationRepository,
            WallMessageRepository wallMessageRepository,
            ProfileMapper profileMapper) {
        this.userRepository = userRepository;
        this.auraRepository = auraRepository;
        this.interestRepository = interestRepository;
        this.badgeRepository = badgeRepository;
        this.followRepository = followRepository;
        this.profileVisitRepository = profileVisitRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.wallMessageRepository = wallMessageRepository;
        this.profileMapper = profileMapper;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID targetId, User viewer) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (viewer != null && !viewer.getId().equals(targetId)) {
            registerVisit(viewer.getId(), targetId);
        }

        return profileMapper.toProfile(target, viewer != null ? viewer.getId() : null);
    }

    private void registerVisit(UUID visitorId, UUID profileId) {
        if (!profileVisitRepository.existsByVisitorIdAndProfileId(visitorId, profileId)) {
            profileVisitRepository.save(new ProfileVisit(visitorId, profileId));
        }
    }

    @Transactional
    public UserProfileResponse completeOnboarding(User principal, OnboardingRequest request) {
        User me = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (!username.equalsIgnoreCase(me.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Ese nombre de usuario ya está en uso");
        }

        Aura aura = auraRepository.findById(request.aura())
                .orElseThrow(() -> new BadRequestException("Aura desconocida: " + request.aura()));

        me.setDisplayName(request.displayName().trim());
        me.setUsername(username);
        me.setAura(aura);
        me.setAvatarUri(request.avatarUri());
        me.setAvatarGradient(request.avatarGradient());
        me.setStatusText("Acaba de regresar");
        me.setBio("Volví para encontrar a las personas que hicieron especial aquella época.");
        me.setOnboardingCompleted(true);

        me.getInterests().clear();
        for (String interestId : request.interests()) {
            Interest interest = interestRepository.findById(interestId)
                    .orElseThrow(() -> new BadRequestException("Interés desconocido: " + interestId));
            me.getInterests().add(interest);
        }

        boolean alreadyHasNewcomerBadge = me.getBadges().stream()
                .anyMatch(badge -> badge.getBadgeId().equals(NEWCOMER_BADGE_ID));
        if (!alreadyHasNewcomerBadge) {
            me.getBadges().add(new UserBadge(NEWCOMER_BADGE_ID, Instant.now()));
        }

        userRepository.save(me);

        welcomeIntoCommunity(me);

        return profileMapper.toProfile(me, me.getId());
    }

    private void welcomeIntoCommunity(User user) {
        Notification notification = new Notification();
        notification.setRecipient(user);
        notification.setCategory(NotificationCategory.seguimientos);
        notification.setTitle("Bienvenido de vuelta, " + user.getDisplayName());
        notification.setBody("Un lugar para volver a encontrarnos. Tu perfil ya está listo.");
        notificationRepository.save(notification);

        chatRoomRepository.findBySlug(MAIN_ROOM_SLUG).ifPresent(mainRoom -> {
            if (!roomMemberRepository.existsByRoomIdAndUserId(mainRoom.getId(), user.getId())) {
                roomMemberRepository.save(new RoomMember(mainRoom.getId(), user.getId()));
            }
            Message systemMessage = new Message();
            systemMessage.setRoom(mainRoom);
            systemMessage.setAuthor(null);
            systemMessage.setType(MessageType.system);
            systemMessage.setBody(user.getDisplayName() + " volvió a la comunidad.");
            messageRepository.save(systemMessage);
        });
    }

    @Transactional
    public UserProfileResponse updateProfile(User principal, UpdateProfileRequest request) {
        User me = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (request.displayName() != null) {
            me.setDisplayName(request.displayName().trim());
        }
        if (request.avatarUri() != null) {
            me.setAvatarUri(request.avatarUri());
        }
        if (request.avatarGradient() != null) {
            me.setAvatarGradient(request.avatarGradient());
        }
        if (request.aura() != null) {
            Aura aura = auraRepository.findById(request.aura())
                    .orElseThrow(() -> new BadRequestException("Aura desconocida: " + request.aura()));
            me.setAura(aura);
        }
        if (request.bio() != null) {
            me.setBio(request.bio());
        }
        if (request.statusText() != null) {
            me.setStatusText(request.statusText());
        }
        if (request.interests() != null) {
            me.getInterests().clear();
            for (String interestId : request.interests()) {
                Interest interest = interestRepository.findById(interestId)
                        .orElseThrow(() -> new BadRequestException("Interés desconocido: " + interestId));
                me.getInterests().add(interest);
            }
        }

        userRepository.save(me);
        return profileMapper.toProfile(me, me.getId());
    }

    @Transactional
    public void follow(User me, UUID targetId) {
        if (me.getId().equals(targetId)) {
            throw new BadRequestException("No puedes seguirte a ti mismo");
        }
        if (!userRepository.existsById(targetId)) {
            throw new NotFoundException("Usuario no encontrado");
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(me.getId(), targetId)) {
            followRepository.save(new Follow(me.getId(), targetId));

            userRepository.findById(targetId).ifPresent(target -> {
                Notification notification = new Notification();
                notification.setRecipient(target);
                notification.setCategory(NotificationCategory.seguimientos);
                notification.setTitle(me.getDisplayName() + " empezó a seguirte");
                notification.setBody("Revisa su perfil y descubre qué tienen en común.");
                notification.setRelatedUser(me);
                notificationRepository.save(notification);
            });
        }
    }

    @Transactional
    public void unfollow(User me, UUID targetId) {
        followRepository.deleteByFollowerIdAndFollowingId(me.getId(), targetId);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getFollowers(UUID userId, User viewer) {
        UUID viewerId = viewer != null ? viewer.getId() : null;
        return followRepository.findByFollowingId(userId).stream()
                .map(Follow::getFollowerId)
                .map(id -> userRepository.findById(id).orElse(null))
                .filter(u -> u != null)
                .map(u -> profileMapper.toProfile(u, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getFollowing(UUID userId, User viewer) {
        UUID viewerId = viewer != null ? viewer.getId() : null;
        return followRepository.findByFollowerId(userId).stream()
                .map(Follow::getFollowingId)
                .map(id -> userRepository.findById(id).orElse(null))
                .filter(u -> u != null)
                .map(u -> profileMapper.toProfile(u, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> search(String query, Pageable pageable, User viewer) {
        UUID viewerId = viewer != null ? viewer.getId() : null;
        Page<User> results = userRepository.search(query, pageable);
        return PageResponse.of(results, u -> profileMapper.toProfile(u, viewerId));
    }

    @Transactional
    public void heartbeat(User me) {
        me.setOnline(true);
        me.setLastActiveAt(Instant.now());
        userRepository.save(me);
    }

    public SettingsResponse getSettings(User me) {
        UserSettings settings = userSettingsRepository.findById(me.getId())
                .orElseGet(() -> userSettingsRepository.save(new UserSettings(me.getId())));
        return toSettingsResponse(settings);
    }

    @Transactional
    public SettingsResponse updateSettings(User me, UpdateSettingsRequest request) {
        UserSettings settings = userSettingsRepository.findById(me.getId())
                .orElseGet(() -> new UserSettings(me.getId()));

        if (request.theme() != null) settings.setTheme(request.theme());
        if (request.effectIntensity() != null) settings.setEffectIntensity(request.effectIntensity());
        if (request.hapticsEnabled() != null) settings.setHapticsEnabled(request.hapticsEnabled());
        if (request.animationsEnabled() != null) settings.setAnimationsEnabled(request.animationsEnabled());
        if (request.showSimulatedActivity() != null) settings.setShowSimulatedActivity(request.showSimulatedActivity());
        if (request.confirmationsEnabled() != null) settings.setConfirmationsEnabled(request.confirmationsEnabled());
        if (request.showOnlineStatus() != null) settings.setShowOnlineStatus(request.showOnlineStatus());
        if (request.allowProfileVisits() != null) settings.setAllowProfileVisits(request.allowProfileVisits());
        if (request.showInterests() != null) settings.setShowInterests(request.showInterests());

        userSettingsRepository.save(settings);
        return toSettingsResponse(settings);
    }

    private SettingsResponse toSettingsResponse(UserSettings settings) {
        return new SettingsResponse(
                settings.getTheme(),
                settings.getEffectIntensity(),
                settings.isHapticsEnabled(),
                settings.isAnimationsEnabled(),
                settings.isShowSimulatedActivity(),
                settings.isConfirmationsEnabled(),
                settings.isShowOnlineStatus(),
                settings.isAllowProfileVisits(),
                settings.isShowInterests());
    }

    public List<AuraResponse> listAuras() {
        return auraRepository.findAll().stream()
                .map(a -> new AuraResponse(a.getId(), a.getName(), a.getDescription(), a.getGradient()))
                .toList();
    }

    public List<InterestResponse> listInterests() {
        return interestRepository.findAll().stream()
                .map(i -> new InterestResponse(i.getId(), i.getLabel(), i.getIcon(), i.getGradient()))
                .toList();
    }

    public List<BadgeResponse> listBadges() {
        return badgeRepository.findAll().stream()
                .map(b -> new BadgeResponse(b.getId(), b.getName(), b.getDescription(), b.getIcon(), b.getGradient()))
                .toList();
    }

    @Transactional
    public WallMessageResponse addWallMessage(User me, UUID profileId, WallMessageRequest request) {
        User profile = userRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        WallMessage message = new WallMessage();
        message.setProfile(profile);
        message.setAuthor(me);
        message.setBody(request.body());
        message = wallMessageRepository.save(message);

        return toWallMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public PageResponse<WallMessageResponse> listWallMessages(UUID profileId, Pageable pageable) {
        return PageResponse.of(
                wallMessageRepository.findByProfileIdOrderByCreatedAtDesc(profileId, pageable),
                this::toWallMessageResponse);
    }

    private WallMessageResponse toWallMessageResponse(WallMessage message) {
        return new WallMessageResponse(
                message.getId(),
                message.getProfile().getId(),
                profileMapper.toSummary(message.getAuthor()),
                message.getBody(),
                message.getCreatedAt());
    }
}
