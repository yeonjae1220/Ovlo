-- ============================================================================
-- V31: global_universities 외부 식별자 (ROR · Wikidata · GeoNames)
-- ----------------------------------------------------------------------------
-- 이름 검색 대신 안정 ID 로 보강·재보강·중복 탐지를 하기 위한 컬럼.
-- 값은 scripts/enrich_ror_wikidata.py 가 만든 apply.sql 로 채운다(마이그레이션은 컬럼만).
--   ror_id       : ROR 짧은 ID (예: 02d439m40) — https://ror.org/<id>
--   wikidata_qid : ROR external_ids 에서 온 QID (예: Q178972)
--   geonames_id  : ROR 위치의 GeoNames 도시 ID (도시명 정규화·다국어 도시명용)
--   ror_status   : active | inactive (폐교·통폐합 표시용)
--   coord_source : latitude/longitude 출처 (manual | wikidata). NULL = 기존 값(출처 미기록)
-- 같은 대학이 마스터에 중복으로 있으면 같은 ror_id 를 가질 수 있어 UNIQUE 는 걸지 않는다.
-- ============================================================================

ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS ror_id       VARCHAR(20);
ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS wikidata_qid VARCHAR(20);
ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS geonames_id  INTEGER;
ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS ror_status   VARCHAR(20);
ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS coord_source VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_gu_ror_id       ON global_universities (ror_id);
CREATE INDEX IF NOT EXISTS idx_gu_wikidata_qid ON global_universities (wikidata_qid);
