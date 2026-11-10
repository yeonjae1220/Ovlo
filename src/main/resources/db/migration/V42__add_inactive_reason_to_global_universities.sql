-- ============================================================================
-- V42: global_universities 폐교·인가취소 행 숨김용 inactive_reason
-- ----------------------------------------------------------------------------
-- canonical_id(V32)는 "같은 학교의 중복 행 → 대표 행"이라 이어 줄 학교가 있을 때만 쓸 수 있다.
-- 인가가 취소됐거나 문을 닫았고 후계 학교가 없는 행은 숨길 방법이 없었다(2026-11-10:
-- Ittihad University·American College of Dubai, UAE CAA 등록부 "Licensure Revoked").
-- inactive_reason 이 NULL 이 아니면 본교 검색·통합 카탈로그·교환대학 목록·리포트 목록에서 빠진다.
-- 행을 지우지 않는 이유는 V32 와 같다: domain 으로 학교 이메일을 역조회하는 경로(졸업생 인증)를 남긴다.
-- 값은 짧은 사유 코드(LICENSURE_REVOKED, CLOSED 등). 되돌리기: inactive_reason = NULL.
-- 행은 도메인과 영문명으로 식별한다(id 는 환경마다 다를 수 있음). 빈 환경(테스트 DB)에서는 no-op.
-- ============================================================================

ALTER TABLE global_universities
    ADD COLUMN IF NOT EXISTS inactive_reason VARCHAR(50);

-- 두 학교 모두 2026-11-10 운영 실측: 회원·신청·국제처·일정 참조 0, 리포트는 이미 DRAFT.
-- 운영 기대값 2행(같은 조건 SELECT 로 확인). 인덱스는 두지 않는다 — 조회는 모두 IS NULL 쪽이고 해당 행이 몇 개뿐이다.
UPDATE global_universities
SET inactive_reason = 'LICENSURE_REVOKED'
WHERE inactive_reason IS NULL
  AND canonical_id IS NULL
  AND country_code = 'AE'
  AND (lower(domain), name_en) IN (('ittihad.ac.ae', 'Ittihad University'),
                                   ('acd.ac.ae', 'American College Of Dubai'));
