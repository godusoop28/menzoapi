-- Sin FK con ON DELETE SET NULL a propósito: no existe ningún endpoint de borrado de mensajes
-- hoy, y de agregarse uno más adelante, un mensaje respondido no debería bloquear ni ser
-- silenciado por ese borrado. Guardar el UUID "suelto" (sin integridad referencial forzada) es lo
-- que permite que, si algún día un mensaje original desaparece, la respuesta siga mostrando
-- "Mensaje eliminado" con el id preservado en vez de perder la referencia sin más (ver
-- ChatService.toReplyPreview).
ALTER TABLE messages ADD COLUMN reply_to_message_id UUID NULL;
CREATE INDEX idx_messages_reply_to ON messages(reply_to_message_id);
