-- R__seed_universities.sql (tombstone / no-op)
-- ============================================================================
-- 구 university(40) 테이블은 global_universities(10,150)로 일원화되며 V21에서 DROP됨.
-- 이 repeatable 시드는 더 이상 데이터를 적재하지 않는다.
-- 파일을 완전히 삭제하면 Flyway가 이미 적용된 repeatable을 'missing'으로 보고
-- validateOnMigrate 실패하므로, no-op 본문으로 유지하여 안전하게 무력화한다.
-- ============================================================================
DO $$ BEGIN END $$;
