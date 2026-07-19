package com.menzo.menzo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.community.CommunityConfig;
import com.menzo.menzo.domain.community.CommunityEvent;
import com.menzo.menzo.domain.community.EventAttendee;
import com.menzo.menzo.domain.community.Notification;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.community.CommunityConfigResponse;
import com.menzo.menzo.dto.community.CreateEventRequest;
import com.menzo.menzo.dto.community.EventResponse;
import com.menzo.menzo.dto.community.NotificationResponse;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.community.CommunityConfigRepository;
import com.menzo.menzo.repository.community.CommunityEventRepository;
import com.menzo.menzo.repository.community.EventAttendeeRepository;
import com.menzo.menzo.repository.community.NotificationRepository;
import com.menzo.menzo.repository.user.UserRepository;

@Service
public class CommunityService {

    private final CommunityConfigRepository communityConfigRepository;
    private final CommunityEventRepository communityEventRepository;
    private final EventAttendeeRepository eventAttendeeRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public CommunityService(
            CommunityConfigRepository communityConfigRepository,
            CommunityEventRepository communityEventRepository,
            EventAttendeeRepository eventAttendeeRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.communityConfigRepository = communityConfigRepository;
        this.communityEventRepository = communityEventRepository;
        this.eventAttendeeRepository = eventAttendeeRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CommunityConfigResponse getConfig() {
        CommunityConfig config = communityConfigRepository.findById((short) 1)
                .orElseThrow(() -> new NotFoundException("Configuración de la comunidad no encontrada"));

        long memberCount = userRepository.count();
        long onlineCount = userRepository.findAll().stream().filter(User::isOnline).count();

        return new CommunityConfigResponse(
                config.getName(),
                config.getSubtitle(),
                config.getDescription(),
                config.getMotto(),
                memberCount,
                onlineCount,
                config.getTags());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents(User viewer) {
        return communityEventRepository.findAllByOrderByDateAscTimeAsc().stream()
                .map(event -> toEventResponse(event, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID eventId, User viewer) {
        CommunityEvent event = communityEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        return toEventResponse(event, viewer);
    }

    @Transactional
    public EventResponse createEvent(User me, CreateEventRequest request) {
        CommunityEvent event = new CommunityEvent();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setDate(request.date());
        event.setTime(request.time());
        event.setKind(request.kind());
        event.setCreatedBy(me);
        event = communityEventRepository.save(event);
        return toEventResponse(event, me);
    }

    @Transactional
    public void attendEvent(User me, UUID eventId) {
        if (!communityEventRepository.existsById(eventId)) {
            throw new NotFoundException("Evento no encontrado");
        }
        if (!eventAttendeeRepository.existsByEventIdAndUserId(eventId, me.getId())) {
            eventAttendeeRepository.save(new EventAttendee(eventId, me.getId()));
        }
    }

    @Transactional
    public void unattendEvent(User me, UUID eventId) {
        eventAttendeeRepository.deleteByEventIdAndUserId(eventId, me.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listNotifications(User me, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(me.getId(), pageable);
        return PageResponse.of(page, this::toNotificationResponse);
    }

    @Transactional
    public void markNotificationRead(User me, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notificación no encontrada"));
        if (!notification.getRecipient().getId().equals(me.getId())) {
            throw new NotFoundException("Notificación no encontrada");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllNotificationsRead(User me) {
        notificationRepository.markAllRead(me.getId());
    }

    private EventResponse toEventResponse(CommunityEvent event, User viewer) {
        long attendeeCount = eventAttendeeRepository.findByEventId(event.getId()).size();
        boolean attendingByMe = viewer != null
                && eventAttendeeRepository.existsByEventIdAndUserId(event.getId(), viewer.getId());

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getTime(),
                event.getKind(),
                attendeeCount,
                attendingByMe);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCategory().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getCreatedAt(),
                notification.isRead(),
                notification.getRelatedPost() != null ? notification.getRelatedPost().getId() : null,
                notification.getRelatedRoom() != null ? notification.getRelatedRoom().getId() : null,
                notification.getRelatedUser() != null ? notification.getRelatedUser().getId() : null,
                notification.getRelatedEvent() != null ? notification.getRelatedEvent().getId() : null);
    }
}
