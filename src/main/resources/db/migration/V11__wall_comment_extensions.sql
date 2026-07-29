ALTER TABLE wall_messages ADD COLUMN image_uri TEXT;

ALTER TABLE wall_comments ADD COLUMN image_uri TEXT;
ALTER TABLE wall_comments ADD COLUMN parent_comment_id UUID REFERENCES wall_comments(id) ON DELETE CASCADE;

CREATE INDEX idx_wall_comments_parent ON wall_comments (parent_comment_id);
