-- V36: 학생 문의 (office_inquiry) 와 피드백 (office_inquiry_feedback)
-- ============================================================================
-- docs/intl-office-pilot-design.md §4.4, ADR-0006. append-only — outcome 만 ESCALATED 로 바뀐다(S4).
-- 후보·인용 id 는 쉼표 구분 문자열(테스트 프로필 H2 호환).
-- ============================================================================

CREATE TABLE office_inquiry (
    id                    BIGSERIAL    PRIMARY KEY,
    office_id             BIGINT       NOT NULL REFERENCES office(id),
    member_id             BIGINT       NOT NULL REFERENCES member(id),
    lang                  VARCHAR(5)   NOT NULL,
    question              VARCHAR(1000) NOT NULL,
    outcome               VARCHAR(20)  NOT NULL,          -- ANSWERED | NO_GROUNDS | ESCALATED
    answer_text           TEXT,
    cited_notice_ids      VARCHAR(500),
    candidate_notice_ids  VARCHAR(500),
    asked_at              TIMESTAMPTZ  NOT NULL,
    latency_ms            BIGINT       NOT NULL DEFAULT 0,
    model_id              VARCHAR(100)
);

-- 담당자 로그(기간·결과), Office 일일 상한
CREATE INDEX idx_office_inquiry_office_asked ON office_inquiry (office_id, asked_at DESC);
CREATE INDEX idx_office_inquiry_member ON office_inquiry (member_id, asked_at DESC);

CREATE TABLE office_inquiry_feedback (
    inquiry_id   BIGINT       PRIMARY KEY REFERENCES office_inquiry(id) ON DELETE CASCADE,
    helpful      BOOLEAN      NOT NULL,
    reason       VARCHAR(300),
    created_at   TIMESTAMPTZ  NOT NULL
);
