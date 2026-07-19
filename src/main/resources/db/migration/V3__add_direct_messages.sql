ALTER TABLE chat_rooms
    ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'PUBLIC' CHECK (type IN ('PUBLIC', 'DIRECT'));

ALTER TABLE chat_rooms ALTER COLUMN name DROP NOT NULL;

CREATE INDEX idx_chat_rooms_type ON chat_rooms (type);
