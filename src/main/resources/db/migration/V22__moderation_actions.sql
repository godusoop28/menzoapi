-- Log de auditoría insert-only para todas las acciones de moderación de staff global
-- (CURATOR/LEADER/MASTER). ON DELETE RESTRICT en actor_id: el historial de un moderador no debe
-- poder desaparecer aunque esa cuenta luego sea eliminada (anonimizada) por MASTER.
CREATE TABLE moderation_actions (
    id          UUID PRIMARY KEY,
    actor_id    UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action_type VARCHAR(30) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   UUID NOT NULL,
    reason      VARCHAR(300) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_moderation_actions_actor ON moderation_actions(actor_id);
CREATE INDEX idx_moderation_actions_created_at ON moderation_actions(created_at DESC);
