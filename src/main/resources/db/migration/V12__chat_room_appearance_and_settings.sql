-- description pasa a nullable: "" ya no es el valor de "sin descripción", NULL lo es.
ALTER TABLE chat_rooms ALTER COLUMN description DROP NOT NULL;
UPDATE chat_rooms SET description = NULL WHERE description = '';

ALTER TABLE chat_rooms ADD COLUMN avatar_uri TEXT;
ALTER TABLE chat_rooms ADD COLUMN category VARCHAR(40);
ALTER TABLE chat_rooms ADD COLUMN max_members INTEGER;
ALTER TABLE chat_rooms ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE chat_rooms ADD COLUMN allow_members_to_invite BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE chat_rooms ADD COLUMN listed BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE chat_rooms ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE chat_rooms SET updated_at = created_at WHERE updated_at IS NULL;
