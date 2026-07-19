-- Lookup / reference tables -------------------------------------------------

CREATE TABLE auras (
    id          VARCHAR(30)  PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    gradient    VARCHAR(30)  NOT NULL
);

CREATE TABLE interests (
    id       VARCHAR(30) PRIMARY KEY,
    label    VARCHAR(50) NOT NULL,
    icon     VARCHAR(50) NOT NULL,
    gradient VARCHAR(30) NOT NULL
);

CREATE TABLE badges (
    id          VARCHAR(30)  PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    icon        VARCHAR(50)  NOT NULL,
    gradient    VARCHAR(30)  NOT NULL
);

-- Users -----------------------------------------------------------------

CREATE TABLE users (
    id                   UUID PRIMARY KEY,
    email                VARCHAR(255) NOT NULL UNIQUE,
    username             VARCHAR(30)  NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    display_name         VARCHAR(60)  NOT NULL,
    avatar_uri           TEXT,
    avatar_gradient      VARCHAR(30)  NOT NULL DEFAULT 'fire',
    aura_id              VARCHAR(30)  NOT NULL REFERENCES auras(id),
    bio                  VARCHAR(280) NOT NULL DEFAULT '',
    status_text          VARCHAR(140) NOT NULL DEFAULT '',
    level                INT          NOT NULL DEFAULT 1,
    xp                   INT          NOT NULL DEFAULT 0,
    reputation           INT          NOT NULL DEFAULT 0,
    is_online            BOOLEAN      NOT NULL DEFAULT false,
    last_active_at       TIMESTAMPTZ,
    role                 VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    enabled              BOOLEAN      NOT NULL DEFAULT true,
    onboarding_completed BOOLEAN      NOT NULL DEFAULT false,
    joined_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_is_online ON users (is_online);

CREATE TABLE user_interests (
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interest_id VARCHAR(30) NOT NULL REFERENCES interests(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, interest_id)
);

CREATE TABLE user_badges (
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id   VARCHAR(30) NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, badge_id)
);

CREATE TABLE follows (
    follower_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, following_id),
    CONSTRAINT chk_follow_not_self CHECK (follower_id <> following_id)
);

CREATE INDEX idx_follows_following ON follows (following_id);

CREATE TABLE profile_visits (
    id          UUID PRIMARY KEY,
    visitor_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profile_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visited_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_profile_visits_profile ON profile_visits (profile_id);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by_id  UUID REFERENCES refresh_tokens(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE user_settings (
    user_id                  UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme                    VARCHAR(20) NOT NULL DEFAULT 'medianoche',
    effect_intensity         VARCHAR(10) NOT NULL DEFAULT 'normal',
    haptics_enabled          BOOLEAN     NOT NULL DEFAULT true,
    animations_enabled       BOOLEAN     NOT NULL DEFAULT true,
    show_simulated_activity  BOOLEAN     NOT NULL DEFAULT true,
    confirmations_enabled    BOOLEAN     NOT NULL DEFAULT true,
    show_online_status       BOOLEAN     NOT NULL DEFAULT true,
    allow_profile_visits     BOOLEAN     NOT NULL DEFAULT true,
    show_interests           BOOLEAN     NOT NULL DEFAULT true
);

-- Community events --------------------------------------------------------

CREATE TABLE community_events (
    id          UUID PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    description TEXT         NOT NULL,
    event_date  DATE         NOT NULL,
    event_time  VARCHAR(10)  NOT NULL,
    kind        VARCHAR(50)  NOT NULL,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_date ON community_events (event_date);

CREATE TABLE event_attendees (
    event_id UUID NOT NULL REFERENCES community_events(id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, user_id)
);

-- Posts ---------------------------------------------------------------------

CREATE TABLE posts (
    id                       UUID PRIMARY KEY,
    author_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                     VARCHAR(20)  NOT NULL CHECK (type IN ('text', 'image', 'poll', 'question', 'event')),
    title                    VARCHAR(150),
    body                     TEXT         NOT NULL,
    image_uri                TEXT,
    abstract_visual_preset   VARCHAR(30),
    abstract_visual_caption  VARCHAR(255),
    gradient                 VARCHAR(30),
    featured                 BOOLEAN      NOT NULL DEFAULT false,
    comment_count            INT          NOT NULL DEFAULT 0,
    event_id                 UUID REFERENCES community_events(id) ON DELETE SET NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_author ON posts (author_id);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);
CREATE INDEX idx_posts_featured ON posts (featured);

CREATE TABLE post_tags (
    post_id UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag     VARCHAR(40) NOT NULL,
    PRIMARY KEY (post_id, tag)
);

CREATE TABLE poll_options (
    id         UUID PRIMARY KEY,
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    label      VARCHAR(120) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_poll_options_post ON poll_options (post_id);

CREATE TABLE poll_votes (
    id         UUID PRIMARY KEY,
    option_id  UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    voted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_poll_votes_post_user UNIQUE (post_id, user_id)
);

CREATE TABLE post_likes (
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE post_bookmarks (
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE INDEX idx_post_bookmarks_user ON post_bookmarks (user_id);

CREATE TABLE comments (
    id         UUID PRIMARY KEY,
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_post ON comments (post_id, created_at);

-- Chat ------------------------------------------------------------------

CREATE TABLE chat_rooms (
    id          UUID PRIMARY KEY,
    slug        VARCHAR(50) UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL DEFAULT '',
    topic       VARCHAR(150) NOT NULL DEFAULT '',
    gradient    VARCHAR(30)  NOT NULL DEFAULT 'community',
    icon        VARCHAR(50)  NOT NULL DEFAULT 'chatbubbles',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE room_members (
    room_id   UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);

CREATE INDEX idx_room_members_user ON room_members (user_id);

CREATE TABLE room_favorites (
    room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE messages (
    id         UUID PRIMARY KEY,
    room_id    UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    author_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    type       VARCHAR(10) NOT NULL DEFAULT 'text' CHECK (type IN ('text', 'system')),
    body       TEXT NOT NULL,
    image_uri  TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_room_created ON messages (room_id, created_at);

CREATE TABLE wall_messages (
    id         UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wall_messages_profile ON wall_messages (profile_id, created_at);

-- Notifications -----------------------------------------------------------

CREATE TABLE notifications (
    id                UUID PRIMARY KEY,
    recipient_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category          VARCHAR(20) NOT NULL CHECK (category IN ('comentarios', 'likes', 'mensajes', 'eventos', 'seguimientos')),
    title             VARCHAR(150) NOT NULL,
    body              TEXT NOT NULL,
    is_read           BOOLEAN NOT NULL DEFAULT false,
    related_post_id   UUID REFERENCES posts(id) ON DELETE CASCADE,
    related_room_id   UUID REFERENCES chat_rooms(id) ON DELETE CASCADE,
    related_user_id   UUID REFERENCES users(id) ON DELETE CASCADE,
    related_event_id  UUID REFERENCES community_events(id) ON DELETE CASCADE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at DESC);

-- Per-user activity ---------------------------------------------------------

CREATE TABLE recently_viewed (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind       VARCHAR(10) NOT NULL CHECK (kind IN ('post', 'member')),
    target_id  UUID NOT NULL,
    viewed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_recently_viewed UNIQUE (user_id, kind, target_id)
);

CREATE INDEX idx_recently_viewed_user ON recently_viewed (user_id, viewed_at DESC);

CREATE TABLE recent_searches (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query        VARCHAR(140) NOT NULL,
    searched_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recent_searches_user ON recent_searches (user_id, searched_at DESC);

-- Community config (singleton) ---------------------------------------------

CREATE TABLE community_config (
    id          SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    name        VARCHAR(100) NOT NULL,
    subtitle    VARCHAR(150) NOT NULL,
    description TEXT         NOT NULL,
    motto       VARCHAR(200) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE community_config_tags (
    config_id  SMALLINT NOT NULL REFERENCES community_config(id) ON DELETE CASCADE,
    tag        VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (config_id, sort_order)
);
