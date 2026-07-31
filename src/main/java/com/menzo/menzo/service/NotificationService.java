package com.menzo.menzo.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.community.CommunityEvent;
import com.menzo.menzo.domain.community.Notification;
import com.menzo.menzo.domain.community.NotificationCategory;
import com.menzo.menzo.domain.post.Post;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.community.NotificationResponse;
import com.menzo.menzo.repository.community.NotificationRepository;

/**
 * Único punto donde se crea una Notification — antes cada servicio (UserService, PostService)
 * guardaba la fila directo contra el repositorio y ahí quedaba: la única forma de enterarse de
 * algo nuevo era abrir la pantalla de notificaciones y que hiciera un fetch manual. Ahora,
 * además de guardar, se empuja en tiempo real por WebSocket a /topic/users/{recipientId}/notifications
 * — mismo patrón de tópico por entidad que ya usa el resto de la API (/topic/rooms/{id}/...),
 * no el mecanismo de "user destinations" de Spring (no está habilitado en WebSocketConfig).
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Notification create(
            User recipient,
            NotificationCategory category,
            String title,
            String body,
            Post relatedPost,
            ChatRoom relatedRoom,
            User relatedUser,
            CommunityEvent relatedEvent) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setCategory(category);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRelatedPost(relatedPost);
        notification.setRelatedRoom(relatedRoom);
        notification.setRelatedUser(relatedUser);
        notification.setRelatedEvent(relatedEvent);
        notification = notificationRepository.save(notification);
        messagingTemplate.convertAndSend(
                "/topic/users/" + recipient.getId() + "/notifications",
                toResponse(notification));
        return notification;
    }

    public NotificationResponse toResponse(Notification notification) {
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
