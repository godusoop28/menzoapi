ALTER TABLE chat_live_sessions ADD COLUMN speaker_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE chat_live_sessions ADD COLUMN title VARCHAR(100);
ALTER TABLE chat_live_sessions ADD COLUMN description TEXT;
ALTER TABLE chat_live_sessions ADD COLUMN announcement TEXT;
