CREATE TABLE marketplace_listing (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES member(id),
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    source_language VARCHAR(2) NOT NULL,
    category VARCHAR(20) NOT NULL,
    item_condition VARCHAR(20) NOT NULL,
    free BOOLEAN NOT NULL,
    bundle BOOLEAN NOT NULL,
    partial_pickup BOOLEAN NOT NULL,
    amount NUMERIC(20,8) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    city VARCHAR(100) NOT NULL,
    university_id BIGINT REFERENCES global_universities(id),
    meeting_point VARCHAR(200) NOT NULL,
    pickup_from DATE,
    pickup_until DATE,
    tags VARCHAR(100) NOT NULL DEFAULT '',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((free AND amount = 0) OR (NOT free AND amount > 0)),
    CHECK (NOT partial_pickup OR bundle),
    CHECK (pickup_from IS NULL OR pickup_until IS NULL OR pickup_from <= pickup_until),
    CHECK (status IN ('AVAILABLE','RESERVED','PARTIALLY_TAKEN','COMPLETED'))
);
CREATE INDEX idx_marketplace_school ON marketplace_listing(university_id, id DESC) WHERE NOT deleted AND NOT hidden;
CREATE INDEX idx_marketplace_region ON marketplace_listing(country_code, lower(city), id DESC) WHERE NOT deleted AND NOT hidden;
CREATE INDEX idx_marketplace_seller ON marketplace_listing(seller_id, id DESC);

-- Isolated from generic /media downloads: every image read checks listing visibility.
CREATE TABLE marketplace_photo (
    id UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES member(id),
    storage_path TEXT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE marketplace_listing_photo (
    listing_id BIGINT NOT NULL REFERENCES marketplace_listing(id),
    photo_id UUID NOT NULL UNIQUE REFERENCES marketplace_photo(id),
    position INTEGER NOT NULL CHECK(position BETWEEN 0 AND 7),
    PRIMARY KEY(listing_id, position)
);
CREATE TABLE marketplace_chat (
    listing_id BIGINT NOT NULL REFERENCES marketplace_listing(id),
    buyer_id BIGINT NOT NULL REFERENCES member(id),
    seller_id BIGINT NOT NULL REFERENCES member(id),
    room_id BIGINT NOT NULL UNIQUE REFERENCES chat_room(id),
    PRIMARY KEY(listing_id, buyer_id)
);
CREATE TABLE marketplace_block (
    actor_id BIGINT NOT NULL REFERENCES member(id),
    target_id BIGINT NOT NULL REFERENCES member(id),
    PRIMARY KEY(actor_id, target_id), CHECK(actor_id <> target_id)
);
CREATE TABLE marketplace_report (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES marketplace_listing(id),
    reporter_id BIGINT NOT NULL REFERENCES member(id),
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(listing_id, reporter_id)
);
CREATE TABLE marketplace_audit (
    id BIGSERIAL PRIMARY KEY, listing_id BIGINT NOT NULL REFERENCES marketplace_listing(id),
    actor VARCHAR(255) NOT NULL, action VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE marketplace_translation (
    listing_id BIGINT NOT NULL REFERENCES marketplace_listing(id),
    version BIGINT NOT NULL, language VARCHAR(2) NOT NULL,
    title TEXT NOT NULL, description TEXT NOT NULL,
    PRIMARY KEY(listing_id, version, language)
);
