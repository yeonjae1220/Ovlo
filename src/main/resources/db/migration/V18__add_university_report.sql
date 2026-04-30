-- V18: 대학별 종합 분석 보고서 테이블 추가
-- 여러 YouTube 영상 후기 + 공식 프로그램 정보를 집계한 단일 게시글 형태의 종합 가이드

CREATE TABLE university_report (
    id                  BIGSERIAL PRIMARY KEY,
    global_univ_id      BIGINT REFERENCES global_universities(id) ON DELETE SET NULL,
    exchange_univ_id    BIGINT REFERENCES exchange_universities(id) ON DELETE SET NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT | PUBLISHED
    source_video_count  INTEGER      NOT NULL DEFAULT 0,
    source_web_count    INTEGER      NOT NULL DEFAULT 0,
    source_video_ids    BIGINT[]     NOT NULL DEFAULT '{}',
    avg_rating          NUMERIC(3,2),
    recommend_ratio     NUMERIC(3,2),
    avg_difficulty      NUMERIC(3,2),
    avg_cost_monthly    VARCHAR(100),
    cost_currency       VARCHAR(20),
    aggregate_stats     JSONB,
    supported_langs     TEXT[]       NOT NULL DEFAULT '{ko}',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_university_report_global UNIQUE (global_univ_id)
);

CREATE TABLE university_report_translation (
    report_id   BIGINT       NOT NULL REFERENCES university_report(id) ON DELETE CASCADE,
    lang        VARCHAR(5)   NOT NULL,
    title       TEXT         NOT NULL,
    summary     TEXT,
    body        TEXT         NOT NULL,
    content     JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (report_id, lang)
);

CREATE INDEX idx_ur_global_univ   ON university_report(global_univ_id);
CREATE INDEX idx_ur_exchange_univ ON university_report(exchange_univ_id);
CREATE INDEX idx_ur_status        ON university_report(status);
CREATE INDEX idx_urt_lang         ON university_report_translation(lang);
CREATE INDEX idx_urt_report_lang  ON university_report_translation(report_id, lang);

CREATE TRIGGER trg_ur_updated_at
    BEFORE UPDATE ON university_report
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_urt_updated_at
    BEFORE UPDATE ON university_report_translation
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
