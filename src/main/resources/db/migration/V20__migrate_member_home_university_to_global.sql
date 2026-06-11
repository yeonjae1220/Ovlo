-- V20: 멤버 본교 id를 global_universities 기준으로 정합화
-- ============================================================================
-- 배경(Phase 0 진단):
--   - 실유저(id 2,5)는 이미 global id(5647=경북대)를 사용 중 → 조치 불필요
--   - board.university_id / member_university_experience: 0건 → 마이그레이션 대상 없음
--   - dev seed 멤버(dev@dev.com)만 home_university_id=1 (university(40) UCLA 의도였으나
--     global id 1 = 무관한 그리스 대학) → UCLA의 global id(1003)로 정정
-- ============================================================================

UPDATE member
SET home_university_id = (
    SELECT id FROM global_universities
    WHERE name_en = 'University of California, Los Angeles'
    ORDER BY id LIMIT 1
)
WHERE email = 'dev@dev.com'
  AND home_university_id = 1;
