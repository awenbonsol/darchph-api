-- V1: Initial schema for Darch PH (admin, products, media_assets, product_media)

-- Single admin account (seeded via admin service, not here)
CREATE TABLE admin (
    id            BIGSERIAL PRIMARY KEY,
    username      TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Product listings
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200)          NOT NULL,
    slug        VARCHAR(240)          NOT NULL UNIQUE,
    description TEXT                  NOT NULL DEFAULT '',
    price       NUMERIC(12, 2)        NOT NULL CHECK (price >= 0),
    currency    VARCHAR(3)            NOT NULL DEFAULT 'PHP',
    buy_url     TEXT                  NOT NULL,
    is_active   BOOLEAN               NOT NULL DEFAULT true,
    featured    BOOLEAN               NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ           NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ           NOT NULL DEFAULT now()
);

-- Indexes on products
CREATE INDEX products_slug_idx ON products (slug);
CREATE INDEX products_active_idx ON products (is_active, created_at DESC);

-- Every uploaded file (image or video) tracked for cleanup
CREATE TABLE media_assets (
    id          BIGSERIAL PRIMARY KEY,
    media_type  TEXT        NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
    bucket      TEXT        NOT NULL,
    object_path TEXT        NOT NULL UNIQUE,
    public_url  TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Association between products and media, defining gallery order
CREATE TABLE product_media (
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT      NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    media_asset_id BIGINT      NOT NULL REFERENCES media_assets (id),
    media_type     TEXT        NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
    position       INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, media_asset_id)
);

-- Indexes on product_media
CREATE INDEX product_media_position_idx ON product_media (product_id, position);

-- Enforce at most one video per product (partial unique index)
CREATE UNIQUE INDEX product_media_one_video
    ON product_media (product_id)
    WHERE media_type = 'VIDEO';
