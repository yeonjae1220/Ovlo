-- ============================================================================
-- V59: 국립공주대학교 도메인 교정 (kyongju.ac.kr → kongju.ac.kr)
-- ----------------------------------------------------------------------------
-- 마스터의 'Kongju National University'(KR) 행이 원본 데이터셋 때부터 경주대(Kyongju) 도메인
-- kyongju.ac.kr 을 갖고 있었다. 그래서 공주대 학생(kongju.ac.kr)은 학교 메일로 인증할 수 없었고,
-- kyongju.ac.kr 메일은 공주대로 잘못 연결됐다. ROR(0373nm262)·Wikidata(Q482673)는 공주대가 맞다.
-- 공식 홈페이지: https://www.kongju.ac.kr (2026-11-28 확인).
-- 2026-11-28 운영 실측: 이 행을 참조하는 회원·인증·리포트·국제처 0, kongju.ac.kr 을 쓰는 다른 행 없음.
-- V40 의 고려대 도메인 교정과 같은 방식: 학교 식별(name_en)과 바꾸기 전 값(domain)을 함께 맞춘다.
-- 빈 환경(테스트 DB)에서는 no-op. 되돌리기: domain='kyongju.ac.kr', website='http://www.kyongju.ac.kr'.
-- ============================================================================

UPDATE global_universities
SET domain = 'kongju.ac.kr', website = 'https://www.kongju.ac.kr'
WHERE country_code = 'KR'
  AND name_en = 'Kongju National University'
  AND lower(domain) = 'kyongju.ac.kr'
  AND NOT EXISTS (SELECT 1 FROM global_universities x WHERE lower(x.domain) = 'kongju.ac.kr');
