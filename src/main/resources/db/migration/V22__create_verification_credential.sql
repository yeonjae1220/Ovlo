-- V22: 학생 인증 자격 테이블
-- ============================================================================
-- 멤버가 특정 대학(global_universities)의 학생 이메일을 인증한 자격을 보관.
-- 본교/파견 무관 — university_id는 인증 대상 대학.
-- ============================================================================

CREATE TABLE verification_credential (
    id             BIGSERIAL    PRIMARY KEY,
    member_id      BIGINT       NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    type           VARCHAR(30)  NOT NULL,
    university_id  BIGINT       NOT NULL REFERENCES global_universities(id),
    verified_email VARCHAR(255) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    verified_at    TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- 멤버당 타입별 활성 자격 1개 (재인증 시 대체)
    CONSTRAINT uq_vc_member_type UNIQUE (member_id, type)
);

CREATE INDEX idx_vc_member ON verification_credential (member_id);

-- 학교 이메일 유일성: 활성(VERIFIED) 자격 기준으로 동일 이메일 중복 인증 차단
CREATE UNIQUE INDEX uq_vc_verified_email_active
    ON verification_credential (lower(verified_email))
    WHERE status = 'VERIFIED';
