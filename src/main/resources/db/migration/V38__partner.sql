-- V38: 정착 제휴 (partner) 와 클릭 집계 (partner_click), Office 의 제휴 노출 설정
-- ============================================================================
-- docs/intl-office-pilot-design.md §4.6·§6.3. 제휴는 플랫폼 공통, 노출은 대학(Office)이 분류 단위로 켠다.
-- 클릭은 append-only 집계 원천이며 외부에 넘기지 않는다.
-- ============================================================================

ALTER TABLE office ADD COLUMN enabled_partner_categories VARCHAR(200);   -- 쉼표 구분, 기본 NULL(전부 꺼짐)

CREATE TABLE partner (
    id           BIGSERIAL     PRIMARY KEY,
    category     VARCHAR(20)   NOT NULL,            -- SIM | INSURANCE | HOUSING | PICKUP
    name         VARCHAR(200)  NOT NULL,
    description  VARCHAR(1000),
    target_url   VARCHAR(1000) NOT NULL,
    enabled      BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order   INTEGER       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE partner_click (
    id           BIGSERIAL   PRIMARY KEY,
    partner_id   BIGINT      NOT NULL REFERENCES partner(id),
    office_id    BIGINT      NOT NULL REFERENCES office(id),
    member_id    BIGINT      REFERENCES member(id) ON DELETE SET NULL,
    clicked_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_partner_click_office_time ON partner_click (office_id, clicked_at DESC);
