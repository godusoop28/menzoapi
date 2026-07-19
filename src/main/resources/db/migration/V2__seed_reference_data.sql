-- Auras -----------------------------------------------------------------

INSERT INTO auras (id, name, description, gradient) VALUES
    ('fuego',    'Fuego',    'Impulso y energía.',                        'fire'),
    ('tormenta', 'Tormenta', 'Movimiento y conexión.',                    'connection'),
    ('eclipse',  'Eclipse',  'Misterio y creatividad.',                   'midnight'),
    ('renacer',  'Renacer',  'Calma y nuevos comienzos.',                 'community'),
    ('prisma',   'Prisma',   'Una mezcla que no necesita explicación.',   'creative');

-- Interests ---------------------------------------------------------------

INSERT INTO interests (id, label, icon, gradient) VALUES
    ('anime',       'Anime',       'sparkles',         'fire'),
    ('manga',       'Manga',       'book',              'creative'),
    ('videojuegos', 'Videojuegos', 'game-controller',   'connection'),
    ('arte',        'Arte',        'color-palette',     'midnight'),
    ('escritura',   'Escritura',   'pencil',            'community'),
    ('futbol',      'Fútbol',      'football',          'fire'),
    ('musica',      'Música',      'musical-notes',     'creative'),
    ('nostalgia',   'Nostalgia',   'time',               'midnight');

-- Badges ------------------------------------------------------------------

INSERT INTO badges (id, name, description, icon, gradient) VALUES
    ('fundador',      'Fundador del reencuentro', 'Estuvo aquí desde el primer día.',              'flame',          'fire'),
    ('recien-llegado','Recién llegado',           'Acaba de volver a casa.',                        'sparkles',       'community'),
    ('narrador',      'Narrador',                 'Sus historias siempre encuentran lectores.',     'book',           'creative'),
    ('artista',       'Alma artista',             'Comparte lo que otros no se atreven.',           'color-palette',  'midnight'),
    ('conector',      'Conector',                 'Une a quienes creían haberse perdido.',          'link',           'connection'),
    ('veterano',      'Veterano digital',         'Recuerda cómo se sentía esta época.',            'medal',          'fire'),
    ('noctambulo',    'Noctámbulo',               'Siempre presente después de medianoche.',        'moon',           'midnight'),
    ('guardian',      'Guardián del muro',        'Deja huellas que otros recuerdan.',              'shield',         'community');

-- Community config ----------------------------------------------------------

INSERT INTO community_config (id, name, subtitle, description, motto) VALUES
    (1, 'Menzo Central', 'El Reencuentro',
     'Una comunidad para quienes aún recuerdan cómo se sentía pertenecer a un lugar en internet.',
     'Un lugar para volver a encontrarnos.');

INSERT INTO community_config_tags (config_id, tag, sort_order) VALUES
    (1, 'Anime', 0),
    (1, 'Manga', 1),
    (1, 'Videojuegos', 2),
    (1, 'Arte', 3),
    (1, 'Nostalgia digital', 4);

-- Main chat room --------------------------------------------------------

INSERT INTO chat_rooms (id, slug, name, description, topic, gradient, icon) VALUES
    ('00000000-0000-0000-0000-000000000001', 'main', 'La sala del reencuentro',
     'El punto de partida para quienes acaban de volver.', 'Bienvenidas y reencuentros', 'fire', 'flame');
