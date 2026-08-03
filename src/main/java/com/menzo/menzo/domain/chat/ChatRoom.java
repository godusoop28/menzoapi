package com.menzo.menzo.domain.chat;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.menzo.menzo.domain.communities.Community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 50, unique = true)
    private String slug;

    @Column(nullable = false, length = 100)
    private String name;

    /** Nullable desde V12 — una sala sin descripción guarda NULL, no "". El valor se normaliza
     * (trim + blank-a-null) en ChatService antes de persistir, nunca acá. */
    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 150)
    private String topic = "";

    @Column(nullable = false, length = 30)
    private String gradient = "community";

    @Column(nullable = false, length = 50)
    private String icon = "chatbubbles";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomType type = RoomType.PUBLIC;

    /** Avatar circular pequeño que identifica la sala en listas/cabecera — distinto de coverUri
     * (portada horizontal) y backgroundUri (fondo detrás de los mensajes). */
    @Column(name = "avatar_uri", columnDefinition = "text")
    private String avatarUri;

    @Column(name = "cover_uri", columnDefinition = "text")
    private String coverUri;

    @Column(name = "background_uri", columnDefinition = "text")
    private String backgroundUri;

    @Column(length = 40)
    private String category;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = false;

    @Column(name = "allow_members_to_invite", nullable = false)
    private boolean allowMembersToInvite = true;

    /** Si aparece en "Chats públicos"/descubrir. Solo el OWNER puede cambiarlo (ver ChatService). */
    @Column(nullable = false)
    private boolean listed = true;

    // Nullable a propósito, y SIEMPRE null para salas DIRECT (ver V29__scope_existing_content_to_naruto.sql
    // y ChatService.requireCanAccess) — los mensajes privados 1 a 1 son globales por diseño y
    // nunca deben asociarse a una comunidad. Solo salas PUBLIC lo usan, y todavía no hay flujo de
    // creación que lo exija (Fase C).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
