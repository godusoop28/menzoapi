-- Sistema de comunidades (Fase A) — dominio nuevo y completamente separado de `community_config`
-- (V2, singular: la configuración de "Menzo" como plataforma única) y de `chat_rooms` (salas de
-- chat, que en esta fase NO tienen todavía communityId — eso es Fase C). Ver Community.java para
-- el porqué del paquete/tabla en plural.
CREATE TABLE communities (
    id                            UUID PRIMARY KEY,
    slug                          VARCHAR(60)  NOT NULL UNIQUE,
    name                          VARCHAR(100) NOT NULL,
    short_description             VARCHAR(200),
    full_description              TEXT,
    status                        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                                       CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED', 'SUSPENDED')),
    visibility                    VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC'
                                       CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'UNLISTED')),
    access_type                   VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                                       CHECK (access_type IN ('OPEN', 'REQUEST', 'INVITE_ONLY')),
    primary_language              VARCHAR(10)  DEFAULT 'es',
    category                      VARCHAR(50),
    icon_url                      TEXT,
    logo_url                      TEXT,
    cover_url                     TEXT,
    background_url                TEXT,
    banner_url                    TEXT,
    primary_color                 VARCHAR(20),
    secondary_color                VARCHAR(20),
    accent_color                  VARCHAR(20),
    text_color                    VARCHAR(20),
    surface_color                 VARCHAR(20),
    created_by                    UUID REFERENCES users(id) ON DELETE SET NULL,
    member_count                  INT NOT NULL DEFAULT 0,
    online_member_count           INT NOT NULL DEFAULT 0,
    post_count                    INT NOT NULL DEFAULT 0,
    chat_count                    INT NOT NULL DEFAULT 0,
    sort_order                    INT NOT NULL DEFAULT 0,
    is_featured                   BOOLEAN NOT NULL DEFAULT false,
    is_official                   BOOLEAN NOT NULL DEFAULT false,
    is_discoverable               BOOLEAN NOT NULL DEFAULT true,
    allow_join_requests           BOOLEAN NOT NULL DEFAULT true,
    allow_public_chats            BOOLEAN NOT NULL DEFAULT true,
    allow_blogs                   BOOLEAN NOT NULL DEFAULT true,
    allow_voice_rooms             BOOLEAN NOT NULL DEFAULT true,
    allow_member_posts            BOOLEAN NOT NULL DEFAULT true,
    minimum_global_level_to_join  INT NOT NULL DEFAULT 0,
    minimum_global_level_to_post  INT NOT NULL DEFAULT 0,
    theme_config                  JSONB NOT NULL DEFAULT '{}',
    navigation_config             JSONB NOT NULL DEFAULT '{}',
    moderation_config             JSONB NOT NULL DEFAULT '{}',
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_communities_status ON communities(status);
CREATE INDEX idx_communities_discoverable ON communities(is_discoverable, sort_order);

CREATE TABLE community_tags (
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    tag          VARCHAR(40) NOT NULL
);
CREATE INDEX idx_community_tags_community ON community_tags(community_id);

CREATE TABLE community_memberships (
    community_id               UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id                     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    community_role              VARCHAR(30) NOT NULL DEFAULT 'MEMBER'
                                     CHECK (community_role IN
                                         ('MEMBER', 'COMMUNITY_CURATOR', 'COMMUNITY_MODERATOR', 'COMMUNITY_ADMIN', 'COMMUNITY_OWNER')),
    membership_status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                     CHECK (membership_status IN
                                         ('ACTIVE', 'PENDING', 'INVITED', 'LEFT', 'REMOVED', 'BANNED')),
    joined_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_visited_at             TIMESTAMPTZ,
    is_favorite                 BOOLEAN NOT NULL DEFAULT false,
    is_muted                    BOOLEAN NOT NULL DEFAULT false,
    community_nickname          VARCHAR(60),
    community_bio               VARCHAR(280),
    contribution_count          INT NOT NULL DEFAULT 0,
    posts_count                 INT NOT NULL DEFAULT 0,
    comments_count              INT NOT NULL DEFAULT 0,
    voice_participation_count   INT NOT NULL DEFAULT 0,
    warnings_count              INT NOT NULL DEFAULT 0,
    banned_until                TIMESTAMPTZ,
    custom_title                VARCHAR(60),
    PRIMARY KEY (community_id, user_id)
);
CREATE INDEX idx_community_memberships_user ON community_memberships(user_id);

-- Semilla de las 7 comunidades iniciales — colores/gradientes provisionales, sin arte oficial ni
-- logos protegidos (ver Contexto §2 del pedido). id literal fijo por comunidad para que sea
-- trivial referenciarlas desde tests/fixtures futuros sin tener que buscarlas por slug primero.
INSERT INTO communities (
    id, slug, name, short_description, full_description, category, sort_order,
    primary_color, secondary_color, accent_color, text_color, surface_color
) VALUES
    ('11111111-1111-1111-1111-111111111101', 'naruto', 'Naruto',
     'Para quienes crecieron viendo a Konoha.',
     'Una comunidad para hablar de Naruto, sus personajes, arcos, teorías y todo lo relacionado — sin spoilers sin avisar.',
     'Anime', 1, '#FF6A00', '#1A1A2E', '#FFB800', '#F5F5F5', '#1E1E2E'),
    ('11111111-1111-1111-1111-111111111102', 'one-piece', 'One Piece',
     'La tripulación siempre suma un miembro más.',
     'Todo sobre One Piece: teorías del One Piece real, arcos favoritos, y la búsqueda eterna de spoilers.',
     'Anime', 2, '#1E88E5', '#B71C1C', '#FFD54F', '#F5F5F5', '#14202E'),
    ('11111111-1111-1111-1111-111111111103', 'futbol', 'Fútbol',
     'De la cancha del barrio a la Champions.',
     'Resultados, equipos, memes y discusiones eternas sobre quién es el mejor de la historia.',
     'Deportes', 3, '#2E7D32', '#FFFFFF', '#FFC107', '#F5F5F5', '#182D1A'),
    ('11111111-1111-1111-1111-111111111104', 'anime', 'Anime',
     'Todo el anime, sin importar la temporada.',
     'Espacio general para cualquier anime que no tenga su propia comunidad todavía — recomendaciones, watchlists y debate.',
     'Anime', 4, '#E91E63', '#3F51B5', '#FFEB3B', '#F5F5F5', '#241A2E'),
    ('11111111-1111-1111-1111-111111111105', 'league-of-legends', 'League of Legends',
     'GG WP, siempre hay revancha.',
     'Builds, meta, clips de jugadas y quejas sobre el último parche — la Grieta del Invocador nunca duerme.',
     'Videojuegos', 5, '#0AC8B9', '#091428', '#C89B3C', '#F5F5F5', '#0B1A2E'),
    ('11111111-1111-1111-1111-111111111106', 'valorant', 'Valorant',
     'Clutch o rendirse.',
     'Estrategia por agente, clips de highlights, y equipos buscando quinto para rankeds.',
     'Videojuegos', 6, '#FF4655', '#0F1923', '#ECE8E1', '#F5F5F5', '#1A1418'),
    ('11111111-1111-1111-1111-111111111107', 'among-us', 'Among Us',
     'Alguien acá es impostor.',
     'Partidas, memes, y esa sensación de que el rojo siempre es sospechoso aunque no haya hecho nada.',
     'Videojuegos', 7, '#C51111', '#132ED1', '#FFFFFF', '#F5F5F5', '#1E1E28');

INSERT INTO community_tags (community_id, tag) VALUES
    ('11111111-1111-1111-1111-111111111101', 'anime'),
    ('11111111-1111-1111-1111-111111111101', 'manga'),
    ('11111111-1111-1111-1111-111111111102', 'anime'),
    ('11111111-1111-1111-1111-111111111102', 'manga'),
    ('11111111-1111-1111-1111-111111111103', 'deportes'),
    ('11111111-1111-1111-1111-111111111104', 'anime'),
    ('11111111-1111-1111-1111-111111111105', 'videojuegos'),
    ('11111111-1111-1111-1111-111111111105', 'moba'),
    ('11111111-1111-1111-1111-111111111106', 'videojuegos'),
    ('11111111-1111-1111-1111-111111111106', 'fps'),
    ('11111111-1111-1111-1111-111111111107', 'videojuegos'),
    ('11111111-1111-1111-1111-111111111107', 'fiesta');
