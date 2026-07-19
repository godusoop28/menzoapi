-- Reset solicitado para volver a probar la app desde cero: borra todas las
-- cuentas y su contenido. El perfil "dev" sigue activo en Render, así que
-- DevDataSeeder vuelve a sembrar los 3 usuarios de ejemplo (emy, dais, ren)
-- en el primer arranque después de este truncate.
-- Reference/seed data (auras, interests, badges, chat room shells, community
-- config) se deja intacta.

DELETE FROM chat_rooms WHERE type = 'DIRECT';

TRUNCATE TABLE
    poll_votes,
    poll_options,
    post_tags,
    post_likes,
    post_bookmarks,
    comments,
    posts,
    wall_messages,
    messages,
    room_members,
    room_favorites,
    notifications,
    event_attendees,
    community_events,
    recently_viewed,
    recent_searches,
    profile_visits,
    follows,
    user_interests,
    user_badges,
    user_settings,
    refresh_tokens,
    users
CASCADE;
