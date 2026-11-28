-- ============================================================================
-- V61: 운영 검증용 테스트 학교 숨김 ([TEST ONLY] … 20261112, id 20307)
-- ----------------------------------------------------------------------------
-- 2026-11-12 운영 검증 때 만든 학교 행이 본교 검색·통합 카탈로그에 실제 학교처럼 노출됐다.
-- 2026-11-28 운영 실측: 이 행을 참조하는 회원 12(테스트 계정 ovlo-test.invalid 2·example.invalid 탈퇴 10),
-- 인증 11, 국제처 1, 장터 글 1 이 있어 DELETE 는 외래키로 막힌다. 그래서 V42 의 inactive_reason 으로 숨긴다.
-- 숨겨도 행과 참조는 그대로 남는다. 학교 식별(id·name_en·domain)을 함께 맞춰 다른 행은 건드리지 않는다.
-- 빈 환경(테스트 DB)에서는 no-op. 되돌리기: inactive_reason = NULL.
-- ============================================================================

UPDATE global_universities
SET inactive_reason = 'TEST_FIXTURE'
WHERE id = 20307
  AND name_en LIKE '[TEST ONLY]%'
  AND lower(domain) = 'ovlo-test.invalid'
  AND inactive_reason IS NULL;
