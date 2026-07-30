CREATE TABLE live_participants (
    id                     UUID PRIMARY KEY,
    live_session_id        UUID NOT NULL REFERENCES chat_live_sessions(id) ON DELETE CASCADE,
    room_id                UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role                   VARCHAR(12) NOT NULL DEFAULT 'AUDIENCE'
                               CHECK (role IN ('HOST', 'CO_HOST', 'SPEAKER', 'AUDIENCE', 'REQUESTED')),
    status                 VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LEFT')),
    microphone_enabled     BOOLEAN NOT NULL DEFAULT false,
    requested_to_speak_at  TIMESTAMPTZ,
    approved_at            TIMESTAMPTZ,
    approved_by            UUID REFERENCES users(id) ON DELETE SET NULL,
    rejected_at            TIMESTAMPTZ,
    joined_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at                TIMESTAMPTZ,
    last_seen_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_live_participants_session_user UNIQUE (live_session_id, user_id)
);

CREATE INDEX idx_live_participants_session_status ON live_participants (live_session_id, status);
CREATE INDEX idx_live_participants_room_user ON live_participants (room_id, user_id);
