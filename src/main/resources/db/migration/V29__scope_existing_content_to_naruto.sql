-- A pedido explícito del usuario: nada de lo existente debe quedar "sin comunidad" mientras se
-- construye el resto del sistema de comunidades (Fase C) — se asocia todo a Naruto (la primera
-- comunidad sembrada). community_id queda nullable a propósito (mismo criterio ya usado en el
-- resto del pedido original: "puede utilizarse temporalmente communityId nullable durante la
-- migración") — todavía no existe un flujo de creación que exija elegir comunidad.
--
-- Las conversaciones DIRECT (mensajes privados 1 a 1) quedan FUERA de esto a propósito: son
-- globales por diseño, no negociable (ver Contexto del pedido original: "Los mensajes privados
-- directos entre usuarios siguen siendo globales... no depende de communityId"). Nunca se les
-- asigna comunidad, ni acá ni en ningún flujo futuro.
ALTER TABLE posts ADD COLUMN community_id UUID REFERENCES communities(id) ON DELETE SET NULL;
ALTER TABLE chat_rooms ADD COLUMN community_id UUID REFERENCES communities(id) ON DELETE SET NULL;

CREATE INDEX idx_posts_community ON posts(community_id);
CREATE INDEX idx_chat_rooms_community ON chat_rooms(community_id);

UPDATE posts
SET community_id = (SELECT id FROM communities WHERE slug = 'naruto')
WHERE community_id IS NULL;

UPDATE chat_rooms
SET community_id = (SELECT id FROM communities WHERE slug = 'naruto')
WHERE community_id IS NULL AND type = 'PUBLIC';

-- Todo usuario existente que todavía no tenga ninguna fila de membresía en Naruto (MASTER/LEADER
-- ya la tienen desde V28, con su rol correspondiente — esto no los toca ni los degrada) queda
-- como miembro común.
INSERT INTO community_memberships (community_id, user_id, community_role, membership_status)
SELECT (SELECT id FROM communities WHERE slug = 'naruto'), u.id, 'MEMBER', 'ACTIVE'
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM community_memberships m
    WHERE m.community_id = (SELECT id FROM communities WHERE slug = 'naruto') AND m.user_id = u.id
);

UPDATE communities c
SET member_count = (SELECT count(*) FROM community_memberships m WHERE m.community_id = c.id AND m.membership_status = 'ACTIVE'),
    post_count = (SELECT count(*) FROM posts p WHERE p.community_id = c.id),
    chat_count = (SELECT count(*) FROM chat_rooms r WHERE r.community_id = c.id)
WHERE c.slug = 'naruto';
