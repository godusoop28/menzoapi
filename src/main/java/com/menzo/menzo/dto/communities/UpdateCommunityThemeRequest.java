package com.menzo.menzo.dto.communities;

import java.util.Map;

/**
 * Reemplaza Community.themeConfig por completo (no merge parcial) — cubre fondos adicionales
 * (feedBackgroundUrl/chatBackgroundUrl), estilo de encabezado/tarjetas y la lista de
 * decoraciones (URLs de imagen: marcos, separadores, insignias, fondos de temporada). Shape
 * libre a propósito (JSONB): el cliente arma el mapa completo y lo manda tal cual, así se puede
 * agregar una clave nueva sin otra migración.
 */
public record UpdateCommunityThemeRequest(Map<String, Object> themeConfig) {
}
