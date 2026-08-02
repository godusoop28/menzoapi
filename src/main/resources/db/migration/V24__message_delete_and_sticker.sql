-- Soft-delete de mensajes (autor siempre puede borrar el suyo; CURATOR+ puede borrar el de otro
-- en salas públicas, ver ChatService.deleteMessage) — mismo patrón que "Mensaje eliminado" ya
-- usado para reply_to_message_id.
ALTER TABLE messages ADD COLUMN deleted_at TIMESTAMPTZ NULL;
ALTER TABLE messages ADD COLUMN deleted_by_user_id UUID NULL;

-- El CHECK original (V1__init_schema.sql) solo permitía 'text'/'system' — hay que ampliarlo o
-- MessageType.sticker falla a nivel de base de datos.
ALTER TABLE messages DROP CONSTRAINT messages_type_check;
ALTER TABLE messages ADD CONSTRAINT messages_type_check CHECK (type IN ('text', 'system', 'sticker'));

-- UUID suelto, sin FK — misma razón que reply_to_message_id: borrar un pack de stickers (V25) no
-- debería romper mensajes viejos que ya usaron un sticker de ese pack.
ALTER TABLE messages ADD COLUMN sticker_id UUID NULL;
CREATE INDEX idx_messages_sticker ON messages(sticker_id);
