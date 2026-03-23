-- ============================================================
-- Ovlo 교환학생 정보 테이블 (Flyway Migration)
-- ============================================================
-- 사용 전 버전 번호 확인 필요:
--   SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;
-- → 현재 최신 버전 +1 로 파일명 변경하세요.
--   예) 현재 V5 이면 → V6__create_global_university_tables.sql
-- ============================================================

-- ── 1. 전세계 대학 기본 정보 ─────────────────────────────────
CREATE TABLE IF NOT EXISTS global_universities (
    id              BIGSERIAL    PRIMARY KEY,
    name_en         VARCHAR(500) NOT NULL,
    country         VARCHAR(100),               -- 한국어 국가명
    country_en      VARCHAR(100),               -- 영문 국가명
    country_code    CHAR(2),                    -- ISO 3166-1 alpha-2
    city            VARCHAR(200),               -- 도시 or 주/도
    website         VARCHAR(500),
    domain          VARCHAR(200),
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gu_country_code ON global_universities(country_code);
CREATE INDEX IF NOT EXISTS idx_gu_name_en      ON global_universities(name_en);

COMMENT ON TABLE  global_universities            IS '전세계 대학 기본 목록 (ycd/universities, ~9600개)';
COMMENT ON COLUMN global_universities.country    IS '한국어 국가명 (주요 국가만 번역, 나머지는 영문)';
COMMENT ON COLUMN global_universities.domain     IS '대학 도메인 (예: waseda.jp)';


-- ── 2. 교환학생 파견 대학 상세 정보 ─────────────────────────────
--    (파이프라인이 수집한 78개 대학 YouTube 분석 데이터)
CREATE TABLE IF NOT EXISTS exchange_universities (
    id              BIGSERIAL    PRIMARY KEY,
    global_univ_id  BIGINT       REFERENCES global_universities(id) ON DELETE SET NULL,
    name_ko         VARCHAR(300),               -- 한국어 대학명
    name_en         VARCHAR(500) NOT NULL UNIQUE,  -- ON CONFLICT 용
    country         VARCHAR(100),               -- 한국어 국가명
    city            VARCHAR(200),
    website         VARCHAR(500),
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_eu_global_id ON exchange_universities(global_univ_id);
CREATE INDEX IF NOT EXISTS idx_eu_name_en   ON exchange_universities(name_en);

COMMENT ON TABLE  exchange_universities              IS '교환학생 파견 가능 대학 목록 (78개 분석 완료)';
COMMENT ON COLUMN exchange_universities.global_univ_id IS 'global_universities FK (이름 매칭 시 연결)';


-- ── 3. 교환학생 영상 분석 데이터 ────────────────────────────────
CREATE TABLE IF NOT EXISTS exchange_video_reviews (
    id              BIGSERIAL    PRIMARY KEY,
    university_id   BIGINT       REFERENCES exchange_universities(id) ON DELETE SET NULL,  -- NULL 허용: 대학 매핑 안된 영상도 보존
    youtube_url     VARCHAR(500) NOT NULL UNIQUE,
    title           VARCHAR(500),
    channel         VARCHAR(200),
    published_at    TIMESTAMPTZ,
    source_lang     CHAR(5)      DEFAULT 'en',
    quality_score   NUMERIC(4,2),

    -- LLM 분석 핵심 필드 (exchange_info JSON에서 추출)
    country         VARCHAR(100),
    city            VARCHAR(200),
    overall_rating  INTEGER,                     -- 1~5
    difficulty      INTEGER,                     -- 1~5
    workload        INTEGER,                     -- 1~5
    recommend       BOOLEAN,
    overall_tone    VARCHAR(50),                 -- 긍정적/중립/부정적
    excitement_level INTEGER,                   -- 1~5

    -- 비용 정보
    cost_total      VARCHAR(100),               -- "월 약 100~150만원"
    cost_rent       VARCHAR(100),
    cost_food       VARCHAR(100),
    cost_transport  VARCHAR(100),
    cost_currency   VARCHAR(50),

    -- 비자 정보
    visa_type       VARCHAR(100),
    visa_cost       VARCHAR(100),
    visa_duration   VARCHAR(100),
    visa_processing_days VARCHAR(100),

    -- 주거
    dorm_available  BOOLEAN,
    dorm_type       VARCHAR(200),
    dorm_price      VARCHAR(100),

    -- 지원 조건
    gpa_requirement VARCHAR(100),
    language_req    VARCHAR(200),
    deadline_info   VARCHAR(300),

    -- 원본 JSON (전체 데이터 보존)
    exchange_info   JSONB,
    summary         TEXT,
    tags            TEXT[],

    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evr_university_id  ON exchange_video_reviews(university_id);
CREATE INDEX IF NOT EXISTS idx_evr_country        ON exchange_video_reviews(country);
CREATE INDEX IF NOT EXISTS idx_evr_overall_rating ON exchange_video_reviews(overall_rating);
CREATE INDEX IF NOT EXISTS idx_evr_recommend      ON exchange_video_reviews(recommend);
CREATE INDEX IF NOT EXISTS idx_evr_exchange_info  ON exchange_video_reviews USING gin(exchange_info);

COMMENT ON TABLE  exchange_video_reviews             IS 'YouTube 교환학생 후기 영상 LLM 분석 데이터';
COMMENT ON COLUMN exchange_video_reviews.exchange_info IS '파이프라인 LLM 분석 전체 결과 (JSONB)';


-- ── 4. updated_at 자동 갱신 트리거 ──────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_gu_updated_at') THEN
        CREATE TRIGGER trg_gu_updated_at
            BEFORE UPDATE ON global_universities
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_eu_updated_at') THEN
        CREATE TRIGGER trg_eu_updated_at
            BEFORE UPDATE ON exchange_universities
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_evr_updated_at') THEN
        CREATE TRIGGER trg_evr_updated_at
            BEFORE UPDATE ON exchange_video_reviews
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;


-- ── 5. "null" 문자열 → NULL 정제 함수 ──────────────────────────
-- import 후 실행: exchange_video_reviews의 null 문자열 더미 데이터 제거
CREATE OR REPLACE FUNCTION nullify_null_strings()
RETURNS void AS $$
BEGIN
    -- VARCHAR 컬럼들: 'null' 문자열 → NULL
    UPDATE exchange_video_reviews SET
        country             = NULLIF(LOWER(country),              'null'),
        city                = NULLIF(LOWER(city),                 'null'),
        overall_tone        = NULLIF(LOWER(overall_tone),         'null'),
        cost_total          = NULLIF(LOWER(cost_total),           'null'),
        cost_rent           = NULLIF(LOWER(cost_rent),            'null'),
        cost_food           = NULLIF(LOWER(cost_food),            'null'),
        cost_transport      = NULLIF(LOWER(cost_transport),       'null'),
        cost_currency       = NULLIF(LOWER(cost_currency),        'null'),
        visa_type           = NULLIF(LOWER(visa_type),            'null'),
        visa_cost           = NULLIF(LOWER(visa_cost),            'null'),
        visa_duration       = NULLIF(LOWER(visa_duration),        'null'),
        visa_processing_days= NULLIF(LOWER(visa_processing_days), 'null'),
        dorm_type           = NULLIF(LOWER(dorm_type),            'null'),
        dorm_price          = NULLIF(LOWER(dorm_price),           'null'),
        gpa_requirement     = NULLIF(LOWER(gpa_requirement),      'null'),
        language_req        = NULLIF(LOWER(language_req),         'null'),
        deadline_info       = NULLIF(LOWER(deadline_info),        'null')
    WHERE
        LOWER(country) = 'null' OR LOWER(city) = 'null' OR
        LOWER(cost_total) = 'null' OR LOWER(visa_type) = 'null' OR
        LOWER(dorm_type) = 'null' OR LOWER(gpa_requirement) = 'null';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION nullify_null_strings() IS
    'import 후 1회 실행: LLM이 null 대신 "null" 문자열로 채운 더미 데이터 제거';
