-- Los datos de referencia sembrados en V2 usaban una narrativa de "reencuentro" que ya no
-- corresponde al tono de la app (red social profesional, no un relato de reunión nostálgica).
-- Esto corrige el contenido real ya insertado en la base — el código que generaba mensajes
-- de sistema con esta narrativa en cada registro se elimina aparte, en UserService/ChatService.

UPDATE chat_rooms
SET name = 'Chat general',
    description = 'El punto de partida para conocer gente nueva.',
    topic = 'Bienvenidas y presentaciones'
WHERE id = '00000000-0000-0000-0000-000000000001';

UPDATE community_config
SET name = 'Menzo',
    subtitle = 'Red social',
    description = 'Una comunidad para conectar, compartir y descubrir gente con tus mismos intereses.',
    motto = 'Conecta. Comparte. Crea.'
WHERE id = 1;

UPDATE community_config_tags SET tag = 'Cultura pop' WHERE config_id = 1 AND tag = 'Nostalgia digital';

UPDATE badges SET name = 'Fundador', description = 'Estuvo aquí desde el primer día.'
WHERE id = 'fundador';

UPDATE badges SET name = 'Recién llegado', description = 'Se acaba de unir a Menzo.'
WHERE id = 'recien-llegado';

UPDATE badges SET name = 'Conector', description = 'Une a personas con intereses en común.'
WHERE id = 'conector';

UPDATE badges SET name = 'Veterano', description = 'Lleva mucho tiempo activo en Menzo.'
WHERE id = 'veterano';
