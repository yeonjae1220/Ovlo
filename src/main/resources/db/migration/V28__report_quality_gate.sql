-- V28: 리포트 번역 품질 게이트 (재발방지)
-- ----------------------------------------------------------------------------
-- 목적: 외부 파이프라인이 university_report_translation 에 주입하는 콘텐츠의
--       '스크립트 섞임 / 통화단위 누출'을 행 단위로 탐지한다.
--   ① detect_report_translation_defects(): 결함코드 배열 반환 → 파이프라인이
--      주입 전 호출해 실패 행을 reject/재생성(능동 게이트)하는 데 사용.
--   ② report_quality_issue + 트리거: 주입을 막지 않고 결함을 격리 테이블에 기록
--      (수동 모니터 — 파이프라인 미연동 상태에서도 새 오염을 즉시 가시화).
-- 엔티티 오기(title↔대학명 불일치)는 마스터 조인이 필요해 배치감사(audit_report_quality.sql)로 분리.
-- ============================================================================

CREATE OR REPLACE FUNCTION detect_report_translation_defects(
    p_lang    VARCHAR,
    p_title   TEXT,
    p_summary TEXT,
    p_body    TEXT,
    p_content TEXT
) RETURNS TEXT[] AS $$
DECLARE
    t   TEXT := concat_ws(' ', p_title, coalesce(p_summary,''), coalesce(p_body,''), coalesce(p_content,''));
    def TEXT[] := ARRAY[]::TEXT[];
BEGIN
    -- 스크립트 섞임: 언어별 '나오면 안 되는' 문자 체계
    IF p_lang = 'ko' AND t ~ '[ぁ-ゟ゠-ヿ]'                 THEN def := array_append(def,'kana_in_ko'); END IF;
    IF p_lang = 'ko' AND t ~ '[Ѐ-ӿก-๛ऀ-ॿ؀-ۿ]'            THEN def := array_append(def,'exotic_in_ko'); END IF;
    -- ja: 한글 인접(정상 병기 아닌 내부혼합) — 한글이 가나/한자에 직접 붙음
    IF p_lang = 'ja' AND (t ~ '[가-힣][ぁ-ゟ゠-ヿ一-鿿]' OR t ~ '[ぁ-ゟ゠-ヿ一-鿿][가-힣]')
                                                            THEN def := array_append(def,'hangul_in_ja'); END IF;
    -- zh: 한글/가나 인접
    IF p_lang = 'zh' AND (t ~ '[가-힣ぁ-ゟ゠-ヿ][一-鿿]' OR t ~ '[一-鿿][가-힣ぁ-ゟ゠-ヿ]')
                                                            THEN def := array_append(def,'foreign_in_zh'); END IF;
    -- 라틴 언어: CJK/키릴이 라틴에 직접 붙음(내부혼합) — 괄호 병기는 공백/괄호로 분리돼 제외됨
    IF p_lang IN ('en','de','fr','es','vi')
       AND (t ~ '[A-Za-z][가-힣ぁ-ゟ゠-ヿ一-鿿Ѐ-ӿ]' OR t ~ '[가-힣ぁ-ゟ゠-ヿ一-鿿Ѐ-ӿ][A-Za-z]')
                                                            THEN def := array_append(def,'script_mix_in_latin'); END IF;
    -- 통화단위 누출: 万(중국 만) 은 ko/라틴 에서 누출 (ja/zh 는 정상 숫자표기)
    IF p_lang NOT IN ('ja','zh') AND t LIKE '%万%'          THEN def := array_append(def,'currency_wan'); END IF;

    RETURN def;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION detect_report_translation_defects IS
  '리포트 번역 행의 스크립트섞임/통화누출 결함코드 배열 반환. 파이프라인 주입 전 게이트로 호출.';

-- 결함 격리/모니터 테이블
CREATE TABLE IF NOT EXISTS report_quality_issue (
    report_id   BIGINT      NOT NULL,
    lang        VARCHAR(5)  NOT NULL,
    defects     TEXT[]      NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (report_id, lang)
);

COMMENT ON TABLE report_quality_issue IS '품질 게이트가 탐지한 결함 행(수동 모니터; 주입은 막지 않음)';

-- 트리거: INSERT/UPDATE 시 결함이면 격리테이블 기록, 아니면 해당 행 기록 제거
CREATE OR REPLACE FUNCTION trg_report_quality_check() RETURNS TRIGGER AS $$
DECLARE
    d TEXT[];
BEGIN
    d := detect_report_translation_defects(NEW.lang, NEW.title, NEW.summary, NEW.body, NEW.content::text);
    IF array_length(d, 1) IS NULL THEN
        DELETE FROM report_quality_issue WHERE report_id = NEW.report_id AND lang = NEW.lang;
    ELSE
        INSERT INTO report_quality_issue(report_id, lang, defects, detected_at)
        VALUES (NEW.report_id, NEW.lang, d, NOW())
        ON CONFLICT (report_id, lang) DO UPDATE SET defects = EXCLUDED.defects, detected_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_report_quality ON university_report_translation;
CREATE TRIGGER trg_report_quality
    AFTER INSERT OR UPDATE OF title, summary, body, content ON university_report_translation
    FOR EACH ROW EXECUTE FUNCTION trg_report_quality_check();
