-- A pedido explícito del usuario: en las comunidades iniciales, la autoridad queda en manos de
-- quien ya tiene rol global de staff. MASTER (la cuenta única configurada) queda como
-- COMMUNITY_OWNER de las siete comunidades — esto ya cubre "si no hay líderes, soy yo por
-- defecto" sin ninguna condición, MASTER siempre queda dueño independientemente de si existe
-- algún LEADER. Cualquier cuenta con rol global LEADER queda como COMMUNITY_ADMIN de las siete —
-- mismo mapeo de jerarquía que ya existe entre los roles globales (MASTER > LEADER) y los de
-- comunidad (COMMUNITY_OWNER > COMMUNITY_ADMIN, ver CommunityRole.java).
--
-- ON CONFLICT defensivo: en este punto ninguna comunidad tiene membresías todavía (recién se
-- sembraron en V27), así que no debería haber fila previa que pisar — pero por si esta cuenta ya
-- se hubiera unido como MEMBER común antes de correr esta migración, MASTER siempre queda en
-- OWNER (nunca se degrada), y LEADER no pisa un rol ya asignado.
INSERT INTO community_memberships (community_id, user_id, community_role, membership_status)
SELECT c.id, u.id, 'COMMUNITY_OWNER', 'ACTIVE'
FROM communities c
CROSS JOIN users u
WHERE u.role = 'MASTER'
ON CONFLICT (community_id, user_id)
    DO UPDATE SET community_role = 'COMMUNITY_OWNER', membership_status = 'ACTIVE';

INSERT INTO community_memberships (community_id, user_id, community_role, membership_status)
SELECT c.id, u.id, 'COMMUNITY_ADMIN', 'ACTIVE'
FROM communities c
CROSS JOIN users u
WHERE u.role = 'LEADER'
ON CONFLICT (community_id, user_id) DO NOTHING;

-- Recalcula member_count real por comunidad en vez de sumar a mano — más simple y a prueba de
-- que esta migración corra sobre datos con membresías previas.
UPDATE communities c
SET member_count = (
    SELECT count(*) FROM community_memberships m
    WHERE m.community_id = c.id AND m.membership_status = 'ACTIVE'
);
