CREATE TABLE live_music_queue_items (
    id                    UUID PRIMARY KEY,
    music_session_id      UUID NOT NULL REFERENCES live_music_sessions(id) ON DELETE CASCADE,
    video_id              VARCHAR(20) NOT NULL CHECK (video_id <> ''),
    title                 VARCHAR(300),
    channel_title         VARCHAR(200),
    thumbnail_url         TEXT,
    duration_seconds      INTEGER,
    requested_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by_user_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    position              INTEGER,
    status                VARCHAR(10) NOT NULL DEFAULT 'QUEUED'
                              CHECK (status IN ('PENDING', 'QUEUED', 'PLAYING', 'PLAYED', 'SKIPPED', 'REJECTED', 'REMOVED')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at            TIMESTAMPTZ,
    ended_at              TIMESTAMPTZ
);

CREATE INDEX idx_music_queue_session_status ON live_music_queue_items (music_session_id, status);
CREATE INDEX idx_music_queue_created_at ON live_music_queue_items (created_at);

-- El orden de la cola solo importa (y solo debe ser único) entre los items QUEUED de una misma
-- sesión — PENDING/PLAYED/SKIPPED/REJECTED/REMOVED no compiten por posición.
CREATE UNIQUE INDEX uq_music_queue_position_when_queued
    ON live_music_queue_items (music_session_id, position)
    WHERE status = 'QUEUED';
