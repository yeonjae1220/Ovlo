-- ============================================================================
-- V32: global_universities 중복 행 숨김용 canonical_id
-- ----------------------------------------------------------------------------
-- 같은 학교가 마스터에 두 줄 이상 있을 때(옛 이름 행·학생 메일 도메인 행 등 — 같은 ror_id)
-- 대표가 아닌 행에 대표 행 id 를 넣는다. 그 행은 본교 검색·통합 카탈로그·리포트 목록에서 빠지고,
-- 그 행의 domain 으로 학교 이메일을 역조회하면 대표 행으로 해석된다.
-- 행을 지우지 않는 이유: domain 이 한 행에 하나뿐이라 지우면 그 도메인 메일로 인증이 안 된다.
-- 되돌리기는 canonical_id = NULL. 값은 마이그레이션이 아니라 운영 SQL 로 채운다(데이터 검토 필요).
-- 대표 행은 스스로 canonical_id 가 NULL 이어야 한다(한 단계만 따라간다) — 운영 SQL 이 확인한다.
-- ============================================================================

ALTER TABLE global_universities
    ADD COLUMN IF NOT EXISTS canonical_id BIGINT REFERENCES global_universities (id);

ALTER TABLE global_universities
    ADD CONSTRAINT chk_gu_canonical_not_self CHECK (canonical_id IS NULL OR canonical_id <> id);

CREATE INDEX IF NOT EXISTS idx_gu_canonical_id
    ON global_universities (canonical_id) WHERE canonical_id IS NOT NULL;
