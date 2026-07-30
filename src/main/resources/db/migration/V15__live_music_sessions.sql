CREATE TABLE live_music_sessions (
    id                       UUID PRIMARY KEY,
    live_session_id          UUID NOT NULL REFERENCES chat_live_sessions(id) ON DELETE CASCADE,
    room_id                  UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    status                   VARCHAR(10) NOT NULL DEFAULT 'IDLE'
                                 CHECK (status IN ('IDLE', 'PLAYING', 'PAUSED', 'STOPPED', 'ERROR')),
    current_queue_item_id    UUID,
    current_video_id         VARCHAR(20),
    current_title            VARCHAR(300),
    current_channel_title    VARCHAR(200),
    current_thumbnail_url    TEXT,
    duration_seconds         INTEGER,
    position_seconds         INTEGER NOT NULL DEFAULT 0 CHECK (position_seconds >= 0),
    playback_started_at      TIMESTAMPTZ,
    paused_at                TIMESTAMPTZ,
    started_by_user_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    controlled_by_user_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    allow_requests           BOOLEAN NOT NULL DEFAULT true,
    version                  BIGINT NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_music_sessions_live_session UNIQUE (live_session_id)
);

CREATE INDEX idx_music_sessions_room ON live_music_sessions (room_id);
