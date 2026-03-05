-- V1: member 테이블 생성

CREATE TABLE member (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    hometown             VARCHAR(100) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    home_university_id   BIGINT,
    profile_image_url    VARCHAR(500),
    bio                  TEXT,
    birth_date           DATE,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 이메일 검색 인덱스
CREATE INDEX idx_member_email ON member (email);
CREATE INDEX idx_member_status ON member (status);
