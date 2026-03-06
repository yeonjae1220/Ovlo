-- V1: member 테이블 생성

CREATE TABLE member (
    id                      BIGSERIAL    PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL,
    hometown                VARCHAR(100) NOT NULL,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    home_university_id      BIGINT       NOT NULL,
    profile_image_media_id  VARCHAR(500),
    bio                     TEXT,
    birth_date              DATE,
    major_name              VARCHAR(255) NOT NULL,
    degree_type             VARCHAR(50)  NOT NULL,
    grade_level             INT          NOT NULL DEFAULT 1,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_member_email  ON member (email);
CREATE INDEX idx_member_status ON member (status);

-- ElementCollection: 언어 능력
CREATE TABLE member_language_skill (
    member_id     BIGINT      NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    cefr_level    VARCHAR(10) NOT NULL
);

-- ElementCollection: 대학 교환 이력
CREATE TABLE member_university_experience (
    member_id     BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    university_id BIGINT NOT NULL,
    start_date    DATE   NOT NULL,
    end_date      DATE
);

-- ElementCollection: 연락처 정보
CREATE TABLE member_contact_info (
    member_id     BIGINT       NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    contact_type  VARCHAR(30)  NOT NULL,
    contact_value VARCHAR(500) NOT NULL
);
