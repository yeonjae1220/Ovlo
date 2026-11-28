-- ============================================================================
-- V62: global_universities.name_en UNIQUE 제약을 마이그레이션으로 고정 (스키마 드리프트 해소)
-- ----------------------------------------------------------------------------
-- 운영 DB 에는 uq_global_univ_name_en(UNIQUE (name_en))이 있지만 어떤 마이그레이션도 만들지 않아
-- 마이그레이션만으로 만든 DB(Testcontainers·새 환경)에는 없었다. 그래서 V58 의 ON CONFLICT (name_en)이
-- 빈 DB 에서 실패했다(LLM-Wiki PIT-374). 2026-11-28 운영 실측: 10,156행·name_en 10,156종·NULL 0.
-- 운영처럼 제약이 이미 있으면 no-op. 중복 name_en 이 있는 DB 에서는 ADD CONSTRAINT 가 실패해
-- 배포가 멈추므로 조용히 건너뛰지 않는다. 되돌리기: DROP CONSTRAINT uq_global_univ_name_en.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'global_universities'::regclass
          AND conname = 'uq_global_univ_name_en'
    ) THEN
        ALTER TABLE global_universities
            ADD CONSTRAINT uq_global_univ_name_en UNIQUE (name_en);
    END IF;
END
$$;
