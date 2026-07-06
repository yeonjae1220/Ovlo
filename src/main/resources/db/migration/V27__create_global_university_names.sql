-- V27: 대학 다국어명 저장소 (다국어 검색 + 리포트 제목 언어별 재생성 기반)
-- 데이터는 외부 보강(Wikidata QID→언어별 라벨)으로 적재. scripts/load_university_names.sh 참조.
-- (스키마만 정의 — 리포트 본문과 마찬가지로 데이터는 파이프라인/스크립트가 채운다.)

CREATE TABLE IF NOT EXISTS global_university_names (
    global_univ_id BIGINT       NOT NULL REFERENCES global_universities(id) ON DELETE CASCADE,
    lang           VARCHAR(5)   NOT NULL,          -- 'ko','en','ja','zh','de','fr','es','vi'
    name           TEXT         NOT NULL,
    source         VARCHAR(20)  NOT NULL DEFAULT 'wikidata',  -- wikidata|wikidata_check|exchange|manual
    qid            VARCHAR(20),                     -- Wikidata QID (감사/재보강용)
    PRIMARY KEY (global_univ_id, lang)
);

CREATE INDEX IF NOT EXISTS idx_gun_lang ON global_university_names(lang);

COMMENT ON TABLE  global_university_names IS '대학 다국어명 (Wikidata 라벨 + exchange.name_ko 보강)';
COMMENT ON COLUMN global_university_names.source IS 'wikidata|wikidata_check(이름매칭 의심)|exchange|manual';
