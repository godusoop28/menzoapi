-- Nueva categoría de notificación para "se inició un LIVE en una sala" — las categorías
-- existentes no encajan (eventos = CommunityEvent del calendario de la comunidad, no un LIVE de
-- voz). El CHECK inline de V1 se llama notifications_category_check (nombre por defecto de
-- Postgres para un CHECK de columna sin nombre explícito).
ALTER TABLE notifications DROP CONSTRAINT notifications_category_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_category_check
    CHECK (category IN ('comentarios', 'likes', 'mensajes', 'eventos', 'seguimientos', 'en_vivo'));
