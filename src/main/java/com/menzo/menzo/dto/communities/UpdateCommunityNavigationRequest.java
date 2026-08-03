package com.menzo.menzo.dto.communities;

import java.util.Map;

/**
 * Reemplaza Community.navigationConfig por completo. Shape esperado por convención (no forzado
 * por el backend): una entrada por sección (home/posts/blogs/chats/live/members/events/about),
 * cada una con {enabled, order, label}. Igual que themeConfig, queda como JSONB libre para no
 * necesitar otra migración si el front agrega una sección nueva.
 */
public record UpdateCommunityNavigationRequest(Map<String, Object> navigationConfig) {
}
