CREATE TABLE chat_live_sessions (
    id                  UUID PRIMARY KEY,
    room_id             UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    type                VARCHAR(20) NOT NULL DEFAULT 'VOICE' CHECK (type IN ('VOICE', 'WATCH_PARTY', 'EVENT')),
    status              VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED')),
    started_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at            TIMESTAMPTZ,
    participant_count   INTEGER NOT NULL DEFAULT 0,
    agora_channel_name  VARCHAR(100),
    last_heartbeat_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- El directorio de salas en vivo filtra por (room_id, status='ACTIVE') para saber si ya existe
-- una sesión abierta al hacer join, y por (status, last_heartbeat_at) para listar "en vivo ahora".
CREATE INDEX idx_live_sessions_room_status ON chat_live_sessions (room_id, status);
CREATE INDEX idx_live_sessions_status_heartbeat ON chat_live_sessions (status, last_heartbeat_at);
