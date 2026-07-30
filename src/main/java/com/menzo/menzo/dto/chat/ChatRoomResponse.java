package com.menzo.menzo.dto.chat;

import java.time.Instant;
import java.util.UUID;

import com.menzo.menzo.dto.user.UserSummary;

public record ChatRoomResponse(
        UUID id,
        String slug,
        String name,
        String description,
        String topic,
        String gradient,
        String icon,
        String type,
        String avatarUri,
        String coverUri,
        String backgroundUri,
        String category,
        Integer maxMembers,
        boolean requiresApproval,
        boolean allowMembersToInvite,
        boolean listed,
        UserSummary peer,
        long memberCount,
        long onlineCount,
        boolean favorite,
        boolean joined,
        String role,
        boolean live,
        LiveSummary liveSummary,
        Instant createdAt,
        Instant updatedAt,
        LastMessage lastMessage) {

    public record LastMessage(String body, boolean hasImage, String senderId, Instant createdAt) {
    }

    /** Resumen ligero del LIVE activo, para tarjetas de sala y listados — el detalle completo
     * (participantes, roles, solicitudes) se pide aparte a /live cuando el usuario entra. */
    public record LiveSummary(
            UUID liveSessionId,
            String title,
            String announcement,
            long participantCount,
            long speakerCount,
            UserSummary host) {
    }
}
