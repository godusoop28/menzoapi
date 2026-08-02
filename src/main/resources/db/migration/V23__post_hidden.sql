-- Acción de CURATOR+ ("ocultar", distinto de "borrar") — ver PostService.hidePost/unhidePost.
ALTER TABLE posts ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT false;
CREATE INDEX idx_posts_hidden ON posts(hidden);
