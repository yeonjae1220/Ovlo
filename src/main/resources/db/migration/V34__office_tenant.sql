-- V34: 대학 국제처 테넌트 (office) 와 담당자 (office_staff)
-- ============================================================================
-- docs/intl-office-pilot-design.md §4.1·§4.2, docs/adr/0005-office-tenant-authorization.md
-- - 대학(global_universities) 1곳에 Office 1개 (UNIQUE global_univ_id)
-- - 담당자는 일반 member. (office_id, member_id) 당 기록 1개 — REMOVED 뒤 재초대는 같은 행을 INVITED 로 되돌린다
-- - 삭제 대신 상태(SUSPENDED / REMOVED)만 쓴다
-- ============================================================================

CREATE TABLE office (
    id              BIGSERIAL    PRIMARY KEY,
    global_univ_id  BIGINT       NOT NULL REFERENCES global_universities(id),
    name            VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL,              -- PILOT | ACTIVE | SUSPENDED
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_office_university UNIQUE (global_univ_id)
);

CREATE TABLE office_staff (
    id           BIGSERIAL    PRIMARY KEY,
    office_id    BIGINT       NOT NULL REFERENCES office(id),
    member_id    BIGINT       NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    role         VARCHAR(20)  NOT NULL,                 -- OFFICE_ADMIN | OFFICE_STAFF
    status       VARCHAR(20)  NOT NULL,                 -- INVITED | ACTIVE | REMOVED
    invited_by   VARCHAR(255) NOT NULL,                 -- 플랫폼 관리자 이메일 또는 초대한 담당자 member id
    invited_at   TIMESTAMPTZ  NOT NULL,
    accepted_at  TIMESTAMPTZ,
    removed_at   TIMESTAMPTZ,

    CONSTRAINT uq_office_staff_member UNIQUE (office_id, member_id)
);

-- 로그인·재발급마다 "이 회원의 활동 소속" 을 찾는다 (LoadOfficeMembershipPort)
CREATE INDEX idx_office_staff_member_status ON office_staff (member_id, status);
CREATE INDEX idx_office_staff_office ON office_staff (office_id);
