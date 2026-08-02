-- Público desde el instante en que se crea — no hay tabla de "packs agregados a mi bandeja"
-- (ver Contexto/decisión #4 del plan): cualquier usuario logueado puede usar el pack de cualquier
-- otro apenas existe.
CREATE TABLE sticker_packs (
    id         UUID PRIMARY KEY,
    creator_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stickers (
    id         UUID PRIMARY KEY,
    pack_id    UUID NOT NULL REFERENCES sticker_packs(id) ON DELETE CASCADE,
    image_url  TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_stickers_pack ON stickers(pack_id, sort_order);
