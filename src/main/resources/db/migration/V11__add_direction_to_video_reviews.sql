-- ── direction 컬럼 추가 ────────────────────────────────────────────
-- OUTBOUND : 이 대학에서 나가는 교환학생 리뷰
-- INBOUND  : 이 대학으로 오는 교환학생 리뷰
-- UNKNOWN  : 방향 미분류 (기본값)
ALTER TABLE exchange_video_reviews
    ADD COLUMN IF NOT EXISTS direction VARCHAR(20) DEFAULT 'UNKNOWN';

-- exchangeInfo JSONB에 direction 키가 있으면 자동 추출
UPDATE exchange_video_reviews
SET direction = UPPER(exchange_info->>'direction')
WHERE exchange_info->>'direction' IS NOT NULL
  AND UPPER(exchange_info->>'direction') IN ('OUTBOUND', 'INBOUND');

CREATE INDEX IF NOT EXISTS idx_evr_direction ON exchange_video_reviews(direction);
