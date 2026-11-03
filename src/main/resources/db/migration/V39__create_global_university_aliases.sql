-- ============================================================================
-- V39: 대학 별칭(검색용) 테이블
-- ----------------------------------------------------------------------------
-- global_university_names 는 (대학, 언어)당 이름 하나라 "연대"·"부산외대"·"서울과기대" 같은
-- 약어, 옛 이름, 다른 영문 표기를 담을 수 없다. 별칭은 표시용이 아니라 검색 매칭 전용이다
-- (본교 선택 검색·통합 카탈로그 검색이 ILIKE 로 본다). 한 대학에 여러 개, 언어 구분 없음.
-- source: manual(사람이 넣음) | 추후 자동 보강 시 별도 값.
-- ============================================================================

CREATE TABLE IF NOT EXISTS global_university_aliases (
    id             BIGSERIAL    PRIMARY KEY,
    global_univ_id BIGINT       NOT NULL REFERENCES global_universities(id) ON DELETE CASCADE,
    alias          TEXT         NOT NULL,
    source         VARCHAR(20)  NOT NULL DEFAULT 'manual',
    CONSTRAINT uq_gua_univ_alias UNIQUE (global_univ_id, alias),
    CONSTRAINT chk_gua_alias_not_blank CHECK (length(btrim(alias)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_gua_global_univ_id ON global_university_aliases(global_univ_id);

COMMENT ON TABLE  global_university_aliases IS '대학 별칭(약어·옛 이름·다른 표기) — 검색 매칭 전용, 표시에는 쓰지 않음';
COMMENT ON COLUMN global_university_aliases.source IS 'manual(사람이 넣음)';
