-- ============================================================================
-- V53: 영상 링크의 출처와 최신성 추적
-- ----------------------------------------------------------------------------
-- exchange_video_reviews 를 '유튜브 영상 링크'(제목·채널·게시일만) 저장에 다시 쓴다.
-- 옛 파이프라인의 LLM 추출 컬럼은 비워 둔다. 링크는 주기적으로 원본에 다시 물어
-- 제목·채널명을 갱신하고, 삭제·비공개된 영상은 지우지 않고 숨긴다.
--   source            어디서 왔는지 ('channel:<채널 ID>' | 'search'), 옛 행은 NULL
--   last_checked_at   원본에 마지막으로 확인을 시도한 시각 — 재확인 대기열의 정렬 기준
--   unavailable_since 원본에서 사라진 것을 처음 본 시각. NULL 이 아니면 조회에서 숨긴다
-- 되돌리기: 세 컬럼과 인덱스를 DROP 한다(데이터 손실 없음 — 기존 컬럼은 건드리지 않는다).
-- ============================================================================
ALTER TABLE exchange_video_reviews
    ADD COLUMN IF NOT EXISTS source            VARCHAR(60),
    ADD COLUMN IF NOT EXISTS last_checked_at   TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS unavailable_since TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_evr_last_checked
    ON exchange_video_reviews (last_checked_at NULLS FIRST, id);

COMMENT ON COLUMN exchange_video_reviews.source            IS '링크 출처: channel:<채널 ID> | search | NULL(옛 파이프라인)';
COMMENT ON COLUMN exchange_video_reviews.last_checked_at   IS '원본(oEmbed)에 마지막으로 확인을 시도한 시각';
COMMENT ON COLUMN exchange_video_reviews.unavailable_since IS '원본에서 사라진 것을 처음 본 시각 — NULL 이 아니면 숨김';
