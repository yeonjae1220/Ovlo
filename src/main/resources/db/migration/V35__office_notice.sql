-- V35: 국제처 승인 자료 (office_notice) 와 번역 (office_notice_translation)
-- ============================================================================
-- docs/intl-office-pilot-design.md §4.3, §5.1
-- - 상태 DRAFT | APPROVED | EXPIRED | ARCHIVED. 삭제 대신 ARCHIVED
-- - 제목·본문이 바뀌면 version+1, 번역은 source_version 으로 stale 여부를 판정한다
-- - 전문 검색(tsvector) 컬럼은 S3(V36) 에서 PG 전용으로 추가한다 — 테스트 프로필(H2)은 엔티티로 스키마를 만든다
-- ============================================================================

CREATE TABLE office_notice (
    id                    BIGSERIAL    PRIMARY KEY,
    office_id             BIGINT       NOT NULL REFERENCES office(id),
    category              VARCHAR(30)  NOT NULL,
    title                 VARCHAR(300) NOT NULL,
    body                  TEXT         NOT NULL,
    source_lang           VARCHAR(5)   NOT NULL DEFAULT 'ko',
    source_url            VARCHAR(1000),
    attachment_media_ids  VARCHAR(1000),                 -- 쉼표 구분 media id 목록
    term                  VARCHAR(20)  NOT NULL,         -- 적용 학기, 예: 2027-1
    effective_from        DATE         NOT NULL,
    expires_at            DATE         NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    approved_by           VARCHAR(255),
    approved_at           TIMESTAMPTZ,
    expiry_warned_at      TIMESTAMPTZ,
    version               INTEGER      NOT NULL DEFAULT 1,
    created_by            BIGINT       NOT NULL REFERENCES member(id),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_office_notice_dates CHECK (expires_at > effective_from)
);

-- 담당자 목록(상태·분류 필터), 학생 조회(APPROVED + 기간), 만료 스케줄러(APPROVED + expires_at)
CREATE INDEX idx_office_notice_office_status ON office_notice (office_id, status, expires_at);
CREATE INDEX idx_office_notice_status_expires ON office_notice (status, expires_at);

CREATE TABLE office_notice_translation (
    id              BIGSERIAL    PRIMARY KEY,
    notice_id       BIGINT       NOT NULL REFERENCES office_notice(id) ON DELETE CASCADE,
    lang            VARCHAR(5)   NOT NULL,
    title           VARCHAR(300) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,              -- MACHINE | REVIEWED
    source_version  INTEGER      NOT NULL,
    generated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_office_notice_translation UNIQUE (notice_id, lang)
);
