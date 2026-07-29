ALTER TABLE room_members ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'MEMBER'
    CHECK (role IN ('OWNER', 'CO_HOST', 'MEMBER'));

-- No se registraba quién creaba cada sala antes de esta migración. Como aproximación segura,
-- el primer miembro que se unió a cada sala pública (el creador siempre fue el primer INSERT
-- en room_members al crear la sala) se promueve a OWNER.
WITH ranked AS (
    SELECT rm.room_id, rm.user_id,
           ROW_NUMBER() OVER (PARTITION BY rm.room_id ORDER BY rm.joined_at ASC) AS rn
    FROM room_members rm
    JOIN chat_rooms cr ON cr.id = rm.room_id
    WHERE cr.type = 'PUBLIC'
)
UPDATE room_members rm SET role = 'OWNER'
FROM ranked
WHERE rm.room_id = ranked.room_id AND rm.user_id = ranked.user_id AND ranked.rn = 1;

CREATE TABLE room_bans (
    room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);
CREATE INDEX idx_room_bans_user ON room_bans (user_id);
