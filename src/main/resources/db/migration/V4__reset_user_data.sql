-- One-time cleanup of accumulated test accounts and their content.
-- Reference/seed data (auras, interests, badges, chat room shells, community
-- config) is intentionally left untouched.

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
