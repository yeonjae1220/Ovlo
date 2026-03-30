-- V13: 웹 크롤러 수집 교환학생 프로그램 정보 테이블
-- 공식 대학 웹사이트에서 추출한 프로그램 정보 (GPA, 마감일, 비자, 기숙사 등)

CREATE TABLE IF NOT EXISTS web_exchange_programs (
    id                      BIGSERIAL PRIMARY KEY,
    global_univ_id          BIGINT REFERENCES global_universities(id),
    name_en                 VARCHAR(500),
    country                 VARCHAR(100),
    source_url              VARCHAR(1000),
    crawled_at              TIMESTAMPTZ,

    -- 핵심 프로그램 요건
    gpa_requirement         VARCHAR(200),
    language_requirement    VARCHAR(300),
    duration                VARCHAR(200),
    application_deadline    VARCHAR(300),
    nomination_deadline     VARCHAR(300),
    quota_per_semester      VARCHAR(100),
    tuition_policy          VARCHAR(300),

    -- 지원 서류 / 연락처
    available_programs      TEXT[],
    required_documents      TEXT[],
    contact_info            VARCHAR(500),

    -- 비자
    visa_type               VARCHAR(200),
    visa_processing_days    VARCHAR(100),
    visa_cost               VARCHAR(200),
    visa_required_docs      TEXT[],

    -- 기숙사 / 주거
    dorm_available          BOOLEAN,
    dorm_price_per_month    VARCHAR(200),
    dorm_type               VARCHAR(200),
    offcampus_price         VARCHAR(200),
    offcampus_areas         TEXT[],

    -- 비용
    estimated_monthly_cost  VARCHAR(200),
    cost_currency           VARCHAR(50),
    scholarship_info        TEXT,

    -- 전체 50개 필드 원본 보존 (JSONB)
    exchange_info           JSONB,
    summary                 TEXT,

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE (global_univ_id, source_url)
);

CREATE INDEX IF NOT EXISTS idx_wep_global_univ_id
    ON web_exchange_programs(global_univ_id);

CREATE INDEX IF NOT EXISTS idx_wep_country
    ON web_exchange_programs(country);

CREATE INDEX IF NOT EXISTS idx_wep_exchange_info
    ON web_exchange_programs USING GIN(exchange_info);

-- updated_at 자동 갱신 트리거
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_wep_updated_at'
    ) THEN
        CREATE TRIGGER trg_wep_updated_at
            BEFORE UPDATE ON web_exchange_programs
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;
