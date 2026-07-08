-- V30: exotic_in_cjk 게이트를 '존재 여부' 블런트 체크 → 인접 규칙으로 정밀화
-- ----------------------------------------------------------------------------
-- 배경: V29의 exotic_in_cjk는 ja/zh 텍스트 어디든 키릴/태국/데바나가리/아랍 문자가
-- 있으면 결함으로 봤다. 그러나 실사(23건) 결과 대부분은 결함이 아니라 **의도된
-- 이중언어 설명**이었다:
--   - "学生ビザ（учебный виза）" — 번역 뒤 현지어 원문 병기(비자 신청에 실제
--     필요한 현지어 용어 안내, 유용한 정보)
--   - "「ขอบคุณค่ะ」（ありがとう）" — 현지어 기초 회화 학습 섹션(의도된 콘텐츠)
-- 두 경우 모두 전각 괄호/따옴표가 경계가 되어 가나/한자와 직접 붙지 않는다.
-- 반면 진짜 결함(레ーゲンスбурク=Regensburg 자기 대학명, チリクраб=Chili Crab
-- 등)은 괄호 없이 가나/라틴에 직접 들러붙어 있었다.
-- ko의 han_in_ko(V29)·ja의 hangul_in_ja·zh의 foreign_in_zh와 동일한
-- adjacency 설계로 통일한다.
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
    IF p_lang = 'ko' AND t ~ '[ぁ-ゟ゠-ヿ]'                 THEN def := array_append(def,'kana_in_ko'); END IF;
    IF p_lang = 'ko' AND t ~ '[Ѐ-ӿก-๛ऀ-ॿ؀-ۿ]'            THEN def := array_append(def,'exotic_in_ko'); END IF;
    IF p_lang = 'ko' AND (t ~ '[가-힣][一-鿿]' OR t ~ '[一-鿿][가-힣]')
                                                            THEN def := array_append(def,'han_in_ko'); END IF;
    IF p_lang = 'ja' AND (t ~ '[가-힣][ぁ-ゟ゠-ヿ一-鿿]' OR t ~ '[ぁ-ゟ゠-ヿ一-鿿][가-힣]')
                                                            THEN def := array_append(def,'hangul_in_ja'); END IF;
    -- exotic_in_cjk: 가나/한자/한글이 키릴/태국/데바나가리/아랍에 '직접' 붙은 경우만
    -- (괄호·따옴표로 분리된 이중언어 설명/회화 학습 섹션은 정상이라 제외)
    IF p_lang IN ('ja','zh')
       AND (t ~ '[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z][Ѐ-ӿก-๛ऀ-ॿ؀-ۿ]' OR t ~ '[Ѐ-ӿก-๛ऀ-ॿ؀-ۿ][가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z]')
                                                            THEN def := array_append(def,'exotic_in_cjk'); END IF;
    IF p_lang = 'zh' AND (t ~ '[가-힣ぁ-ゟ゠-ヿ][一-鿿]' OR t ~ '[一-鿿][가-힣ぁ-ゟ゠-ヿ]')
                                                            THEN def := array_append(def,'foreign_in_zh'); END IF;
    IF p_lang IN ('en','de','fr','es','vi')
       AND (t ~ '[A-Za-z][가-힣ぁ-ゟ゠-ヿ一-鿿Ѐ-ӿ]' OR t ~ '[가-힣ぁ-ゟ゠-ヿ一-鿿Ѐ-ӿ][A-Za-z]')
                                                            THEN def := array_append(def,'script_mix_in_latin'); END IF;
    IF p_lang NOT IN ('ja','zh') AND t LIKE '%万%'          THEN def := array_append(def,'currency_wan'); END IF;

    RETURN def;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION detect_report_translation_defects IS
  '리포트 번역 행의 스크립트섞임/통화누출 결함코드 배열 반환. (V30: exotic_in_cjk를 인접 규칙으로 정밀화 — 정상 이중언어 병기 오탐 제거)';

DELETE FROM report_quality_issue;
INSERT INTO report_quality_issue(report_id, lang, defects, detected_at)
SELECT report_id, lang, d, NOW()
FROM (
    SELECT report_id, lang,
           detect_report_translation_defects(lang, title, summary, body, content::text) AS d
    FROM university_report_translation
) x
WHERE array_length(d, 1) IS NOT NULL;
