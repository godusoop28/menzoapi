package com.menzo.menzo.domain.communities;

/**
 * Rol específico de UNA comunidad — no confundir con {@link com.menzo.menzo.domain.user.Role}
 * (global de plataforma: USER/CURATOR/LEADER/MASTER). Mismo patrón que ya coexiste hoy entre ese
 * Role global y RoomRole/LiveParticipantRole (por sala) — CommunityRole es "uno más" de esa
 * familia de roles por-ámbito, no una jerarquía paralela de autoridad de plataforma.
 *
 * Ascendente en poder — ordinal() da un chequeo limpio de "al menos este nivel" en
 * CommunityPermissionEvaluator.
 */
public enum CommunityRole {
    MEMBER,
    COMMUNITY_CURATOR,
    COMMUNITY_MODERATOR,
    COMMUNITY_ADMIN,
    COMMUNITY_OWNER
}
