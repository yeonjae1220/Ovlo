-- V2: university 테이블 생성

CREATE TABLE university (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(300) NOT NULL,
    local_name   VARCHAR(300),
    country_code CHAR(2)      NOT NULL,
    city         VARCHAR(100) NOT NULL,
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,
    website_url  VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 검색·필터 인덱스
CREATE INDEX idx_university_country_code ON university (country_code);
CREATE INDEX idx_university_name ON university USING gin (to_tsvector('simple', name));
