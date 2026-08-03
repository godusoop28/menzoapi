-- A pedido explícito del usuario ("borrar a todos los usuarios menos al admin, los chats,
-- blogs y todo eso, para empezar de 0"). Las comunidades (Naruto, Anime, etc.) NO se tocan —
-- solo se vacían de miembros/posts/chats; sus nombres/colores/imágenes configuradas quedan
-- intactos. Tampoco se toca community_config (config de Menzo como plataforma) ni las tablas de
-- referencia (auras/interests/badges).
--
-- Orden importa: moderation_actions.actor_id es ON DELETE RESTRICT (el historial de un
-- moderador no debe poder desaparecer solo, ver V22) — hay que vaciarla primero o el DELETE de
-- users más abajo falla si algún usuario borrado alguna vez actuó como moderador.
--
-- posts y chat_rooms se borran en su totalidad (no solo lo de otros usuarios) — "blogs y chats,
-- todo" fue explícito, no una limpieza parcial. El resto (comments, likes, bookmarks,
-- room_members, messages, live sessions, etc.) cae en cascada desde posts/chat_rooms/users por
-- las FKs ON DELETE CASCADE ya existentes (ver V1 y migraciones posteriores).
DELETE FROM moderation_actions;
DELETE FROM posts;
DELETE FROM chat_rooms;

DELETE FROM users WHERE lower(email) <> lower('emy.rodriguezc28@gmail.com');

-- Los contadores de comunidad son columnas propias (no calculadas), así que hay que
-- recomputarlos a mano tras el borrado — si no, quedan mostrando números viejos.
UPDATE communities c
SET member_count = (
        SELECT count(*) FROM community_memberships m
        WHERE m.community_id = c.id AND m.membership_status = 'ACTIVE'
    ),
    post_count = 0,
    chat_count = 0,
    online_member_count = 0;
