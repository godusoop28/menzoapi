ALTER TABLE chat_rooms ADD COLUMN cover_uri TEXT;
ALTER TABLE chat_rooms ADD COLUMN background_uri TEXT;

ALTER TABLE users ADD COLUMN background_uri TEXT;
ALTER TABLE users ADD COLUMN background_color VARCHAR(20);

CREATE TABLE wall_comments (
    id              UUID PRIMARY KEY,
    wall_message_id UUID NOT NULL REFERENCES wall_messages(id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wall_comments_message ON wall_comments (wall_message_id, created_at);

CREATE TABLE wall_comment_likes (
    comment_id UUID NOT NULL REFERENCES wall_comments(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (comment_id, user_id)
);
