package com.menzo.menzo.domain.communities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.menzo.menzo.domain.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

/**
 * Una comunidad temática dentro de Menzo (p. ej. Naruto, Anime, Fútbol) — NO confundir con
 * {@link com.menzo.menzo.domain.community.CommunityConfig} (paquete singular ya existente), que
 * es la configuración de "Menzo" como plataforma única (nombre del sitio, motto, etc.) y no
 * tiene relación con este dominio. Se dejó ese paquete/clases intactos a propósito — este nuevo
 * concepto vive en el paquete `domain.communities` (plural) y se expone en `/api/communities`
 * (plural), separado de `/api/community/*` (singular, ya existente).
 *
 * themeConfig/navigationConfig/moderationConfig quedan como JSONB desde ahora (Fase A) aunque
 * todavía no exista UI/API para editarlos (eso es Fase D) — así el shape de la entidad no
 * necesita volver a tocarse cuando se implemente esa parte.
 */
@Entity
@Table(name = "communities")
@Getter
@Setter
@NoArgsConstructor
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "short_description", length = 200)
    private String shortDescription;

    @Column(name = "full_description", columnDefinition = "text")
    private String fullDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityStatus status = CommunityStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityVisibility visibility = CommunityVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 20)
    private CommunityAccessType accessType = CommunityAccessType.OPEN;

    @Column(name = "primary_language", length = 10)
    private String primaryLanguage = "es";

    @Column(length = 50)
    private String category;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "community_tags", joinColumns = @JoinColumn(name = "community_id"))
    @Column(name = "tag", length = 40)
    private Set<String> tags = new HashSet<>();

    @Column(name = "icon_url", columnDefinition = "text")
    private String iconUrl;

    @Column(name = "logo_url", columnDefinition = "text")
    private String logoUrl;

    @Column(name = "cover_url", columnDefinition = "text")
    private String coverUrl;

    @Column(name = "background_url", columnDefinition = "text")
    private String backgroundUrl;

    @Column(name = "banner_url", columnDefinition = "text")
    private String bannerUrl;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", length = 20)
    private String secondaryColor;

    @Column(name = "accent_color", length = 20)
    private String accentColor;

    @Column(name = "text_color", length = 20)
    private String textColor;

    @Column(name = "surface_color", length = 20)
    private String surfaceColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;

    @Column(name = "online_member_count", nullable = false)
    private int onlineMemberCount = 0;

    @Column(name = "post_count", nullable = false)
    private int postCount = 0;

    @Column(name = "chat_count", nullable = false)
    private int chatCount = 0;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Column(name = "is_official", nullable = false)
    private boolean official = false;

    @Column(name = "is_discoverable", nullable = false)
    private boolean discoverable = true;

    @Column(name = "allow_join_requests", nullable = false)
    private boolean allowJoinRequests = true;

    @Column(name = "allow_public_chats", nullable = false)
    private boolean allowPublicChats = true;

    @Column(name = "allow_blogs", nullable = false)
    private boolean allowBlogs = true;

    @Column(name = "allow_voice_rooms", nullable = false)
    private boolean allowVoiceRooms = true;

    @Column(name = "allow_member_posts", nullable = false)
    private boolean allowMemberPosts = true;

    @Column(name = "minimum_global_level_to_join", nullable = false)
    private int minimumGlobalLevelToJoin = 0;

    @Column(name = "minimum_global_level_to_post", nullable = false)
    private int minimumGlobalLevelToPost = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> themeConfig = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "navigation_config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> navigationConfig = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "moderation_config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> moderationConfig = Map.of();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
