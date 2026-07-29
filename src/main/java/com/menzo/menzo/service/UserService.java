package com.menzo.menzo.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.WallComment;
import com.menzo.menzo.domain.chat.WallCommentLike;
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
import com.menzo.menzo.dto.user.WallCommentEvent;
import com.menzo.menzo.dto.user.WallCommentRequest;
import com.menzo.menzo.dto.user.WallCommentResponse;
import com.menzo.menzo.dto.user.WallMessageRequest;
import com.menzo.menzo.dto.user.WallMessageResponse;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.WallCommentLikeRepository;
import com.menzo.menzo.repository.chat.WallCommentRepository;
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

    private static final String NEWCOMER_BADGE_ID = "recien-llegado";

    private final UserRepository userRepository;
    private final AuraRepository auraRepository;
    private final InterestRepository interestRepository;
    private final BadgeRepository badgeRepository;
    private final FollowRepository followRepository;
    private final ProfileVisitRepository profileVisitRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final NotificationRepository notificationRepository;
    private final WallMessageRepository wallMessageRepository;
    private final WallCommentRepository wallCommentRepository;
    private final WallCommentLikeRepository wallCommentLikeRepository;
    private final ProfileMapper profileMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public UserService(
            UserRepository userRepository,
            AuraRepository auraRepository,
            InterestRepository interestRepository,
            BadgeRepository badgeRepository,
            FollowRepository followRepository,
            ProfileVisitRepository profileVisitRepository,
            UserSettingsRepository userSettingsRepository,
            NotificationRepository notificationRepository,
            WallMessageRepository wallMessageRepository,
            WallCommentRepository wallCommentRepository,
            WallCommentLikeRepository wallCommentLikeRepository,
            ProfileMapper profileMapper,
            SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.auraRepository = auraRepository;
        this.interestRepository = interestRepository;
        this.badgeRepository = badgeRepository;
        this.followRepository = followRepository;
        this.profileVisitRepository = profileVisitRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.notificationRepository = notificationRepository;
        this.wallMessageRepository = wallMessageRepository;
        this.wallCommentRepository = wallCommentRepository;
        this.wallCommentLikeRepository = wallCommentLikeRepository;
        this.profileMapper = profileMapper;
        this.messagingTemplate = messagingTemplate;
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

        Aura aura = auraRepository.findById(request.aura())
                .orElseThrow(() -> new BadRequestException("Aura desconocida: " + request.aura()));

        me.setDisplayName(request.displayName().trim());
        me.setAura(aura);
        me.setAvatarUri(request.avatarUri());
        me.setAvatarGradient(request.avatarGradient());
        me.setStatusText("");
        me.setBio("");
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

        sendWelcomeNotification(me);

        return profileMapper.toProfile(me, me.getId());
    }

    private void sendWelcomeNotification(User user) {
        Notification notification = new Notification();
        notification.setRecipient(user);
        notification.setCategory(NotificationCategory.seguimientos);
        notification.setTitle("¡Bienvenido a Menzo, " + user.getDisplayName() + "!");
        notification.setBody("Tu perfil ya está listo. Explorá salas y empezá a conectar.");
        notificationRepository.save(notification);
    }

    @Transactional
    public UserProfileResponse updateProfile(User principal, UpdateProfileRequest request) {
        User me = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (request.displayName() != null) {
            me.setDisplayName(request.displayName().trim());
        }
        if (request.username() != null) {
            String username = request.username().trim().toLowerCase(Locale.ROOT);
            if (!username.equalsIgnoreCase(me.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
                throw new ConflictException("Ese nombre de usuario ya está en uso");
            }
            me.setUsername(username);
        }
        if (request.avatarUri() != null) {
            me.setAvatarUri(request.avatarUri());
        }
        if (request.avatarGradient() != null) {
            me.setAvatarGradient(request.avatarGradient());
        }
        if (request.coverUri() != null) {
            me.setCoverUri(request.coverUri());
        }
        if (request.backgroundUri() != null) {
            me.setBackgroundUri(request.backgroundUri().isBlank() ? null : request.backgroundUri());
        }
        if (request.backgroundColor() != null) {
            me.setBackgroundColor(request.backgroundColor().isBlank() ? null : request.backgroundColor());
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
        List<UUID> followerIds = followRepository.findByFollowingId(userId).stream().map(Follow::getFollowerId).toList();
        List<User> users = userRepository.findAllById(followerIds);
        return profileMapper.toProfiles(users, viewerId);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getFollowing(UUID userId, User viewer) {
        UUID viewerId = viewer != null ? viewer.getId() : null;
        List<UUID> followingIds = followRepository.findByFollowerId(userId).stream().map(Follow::getFollowingId).toList();
        List<User> users = userRepository.findAllById(followingIds);
        return profileMapper.toProfiles(users, viewerId);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> search(String query, Pageable pageable, User viewer) {
        UUID viewerId = viewer != null ? viewer.getId() : null;
        Page<User> results = userRepository.search(query, pageable);
        List<UserProfileResponse> profiles = profileMapper.toProfiles(results.getContent(), viewerId);
        return new PageResponse<>(
                profiles, results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages(), results.hasNext());
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

        String body = requireBodyOrImage(request.body(), request.imageUri());

        WallMessage message = new WallMessage();
        message.setProfile(profile);
        message.setAuthor(me);
        message.setBody(body);
        message.setImageUri(blankToNull(request.imageUri()));
        // saveAndFlush: @CreationTimestamp recién completa createdAt al ejecutarse el INSERT
        // (al hacer flush), no al llamar a save(). Sin esto, toWallMessageResponse (misma
        // transacción, un par de líneas abajo) leería createdAt en null.
        message = wallMessageRepository.saveAndFlush(message);

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
                message.getImageUri(),
                message.getCreatedAt(),
                wallCommentRepository.countByWallMessageId(message.getId()));
    }

    @Transactional
    public WallCommentResponse addWallComment(User me, UUID wallMessageId, WallCommentRequest request) {
        WallMessage wallMessage = wallMessageRepository.findById(wallMessageId)
                .orElseThrow(() -> new NotFoundException("Mensaje de muro no encontrado"));

        String body = requireBodyOrImage(request.body(), request.imageUri());

        WallComment parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = wallCommentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new NotFoundException("Comentario no encontrado"));
            if (!parentComment.getWallMessage().getId().equals(wallMessageId)) {
                throw new BadRequestException("El comentario al que respondés no pertenece a esta publicación");
            }
        }

        WallComment comment = new WallComment();
        comment.setWallMessage(wallMessage);
        comment.setAuthor(me);
        comment.setParentComment(parentComment);
        comment.setBody(body);
        comment.setImageUri(blankToNull(request.imageUri()));
        comment = wallCommentRepository.saveAndFlush(comment);

        WallCommentResponse response = toWallCommentResponse(comment, me.getId());
        messagingTemplate.convertAndSend(
                "/topic/wall/" + wallMessageId + "/comments", WallCommentEvent.created(response));

        notifyWallComment(me, wallMessage, parentComment);

        return response;
    }

    private void notifyWallComment(User commenter, WallMessage wallMessage, WallComment parentComment) {
        User postAuthor = wallMessage.getAuthor();
        if (!postAuthor.getId().equals(commenter.getId())) {
            Notification notification = new Notification();
            notification.setRecipient(postAuthor);
            notification.setCategory(NotificationCategory.comentarios);
            notification.setTitle(commenter.getDisplayName() + " comentó en tu muro");
            notification.setBody("Revisa qué te escribió.");
            notification.setRelatedUser(commenter);
            notificationRepository.save(notification);
        }

        if (parentComment != null) {
            User parentAuthor = parentComment.getAuthor();
            boolean alreadyNotified = parentAuthor.getId().equals(postAuthor.getId());
            if (!parentAuthor.getId().equals(commenter.getId()) && !alreadyNotified) {
                Notification notification = new Notification();
                notification.setRecipient(parentAuthor);
                notification.setCategory(NotificationCategory.comentarios);
                notification.setTitle(commenter.getDisplayName() + " respondió tu comentario");
                notification.setBody("Revisa qué te escribió.");
                notification.setRelatedUser(commenter);
                notificationRepository.save(notification);
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<WallCommentResponse> listWallComments(UUID wallMessageId, Pageable pageable, User viewer) {
        UUID viewerId = viewer != null ? viewer.getId() : null;
        return PageResponse.of(
                wallCommentRepository.findByWallMessageIdOrderByCreatedAtAsc(wallMessageId, pageable),
                comment -> toWallCommentResponse(comment, viewerId));
    }

    @Transactional
    public void deleteWallComment(User me, UUID commentId) {
        WallComment comment = wallCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comentario no encontrado"));

        boolean isAuthor = comment.getAuthor().getId().equals(me.getId());
        boolean isWallOwner = comment.getWallMessage().getProfile().getId().equals(me.getId());
        if (!isAuthor && !isWallOwner) {
            throw new BadRequestException("No podés borrar este comentario");
        }

        UUID wallMessageId = comment.getWallMessage().getId();
        wallCommentRepository.delete(comment);

        messagingTemplate.convertAndSend(
                "/topic/wall/" + wallMessageId + "/comments", WallCommentEvent.deleted(commentId));
    }

    @Transactional
    public void likeWallComment(User me, UUID commentId) {
        if (!wallCommentRepository.existsById(commentId)) {
            throw new NotFoundException("Comentario no encontrado");
        }
        if (!wallCommentLikeRepository.existsByCommentIdAndUserId(commentId, me.getId())) {
            wallCommentLikeRepository.save(new WallCommentLike(commentId, me.getId()));
        }
    }

    @Transactional
    public void unlikeWallComment(User me, UUID commentId) {
        wallCommentLikeRepository.deleteByCommentIdAndUserId(commentId, me.getId());
    }

    private WallCommentResponse toWallCommentResponse(WallComment comment, UUID viewerId) {
        long likeCount = wallCommentLikeRepository.countByCommentId(comment.getId());
        boolean likedByMe = viewerId != null
                && wallCommentLikeRepository.existsByCommentIdAndUserId(comment.getId(), viewerId);
        return new WallCommentResponse(
                comment.getId(),
                comment.getWallMessage().getId(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                profileMapper.toSummary(comment.getAuthor()),
                comment.getBody(),
                comment.getImageUri(),
                comment.getCreatedAt(),
                likeCount,
                likedByMe);
    }

    /** Body y/o imagen: al menos uno de los dos tiene que traer contenido real (una publicación o
     * comentario no puede quedar completamente vacío), pero ninguno es obligatorio por separado —
     * así se permite un comentario solo de imagen o solo de texto. La columna `body` sigue siendo
     * NOT NULL en la base, así que un body ausente se guarda como cadena vacía. */
    private String requireBodyOrImage(String body, String imageUri) {
        String trimmedBody = body == null ? "" : body.trim();
        boolean hasImage = imageUri != null && !imageUri.isBlank();
        if (trimmedBody.isEmpty() && !hasImage) {
            throw new BadRequestException("Escribí algo o agregá una imagen");
        }
        return trimmedBody;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
