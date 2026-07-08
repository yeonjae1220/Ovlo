-- V29: 리포트 품질 게이트 보강 — ko 한자 인접 탐지 + ja/zh exotic script 탐지
-- ----------------------------------------------------------------------------
-- 배경: V28은 ko 필드에서 한자(漢字)를 결함 대상에서 통째로 제외했다
--       ("일본/중국 대학 한자 고유명은 정상"이라는 전제). 그러나 이 전제가
--       방 종류·일반명사 같은 어휘 단위 오염(예: "단人间"="단인실")까지
--       통과시켜, 728개 중 415개(57%) 리포트가 한자 혼입 상태로 방치됐다
--       (부경대 346 리포트에서 사용자가 최초 발견, scripts/fix_han_in_ko_apply.sql
--       로 고빈도 공통 패턴 교정 완료, 잔여 140개는 지명/음식/앱명 롱테일).
-- 이번 보강:
--   ① ko: 한글에 한자가 '직접'(공백/괄호 등 경계 없이) 붙은 경우만 결함으로 본다.
--      "리츠메이칸대학(立命館大學)" 같은 정상 병기는 괄호가 경계가 되어
--      adjacency 매치가 안 되므로 오탐하지 않는다.
--   ② ja/zh: exotic script(키릴/태국/데바나가리/아랍) 인접 탐지 추가
--      (라틴 언어에는 이미 있었으나 ja/zh 에는 누락돼 있었음).
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
    -- ko: 한자가 한글에 직접 붙은 경우(정상 병기는 괄호/공백이 경계가 되어 제외됨)
    IF p_lang = 'ko' AND (t ~ '[가-힣][一-鿿]' OR t ~ '[一-鿿][가-힣]')
                                                            THEN def := array_append(def,'han_in_ko'); END IF;
    -- ja: 한글 인접(정상 병기 아닌 내부혼합) — 한글이 가나/한자에 직접 붙음
    IF p_lang = 'ja' AND (t ~ '[가-힣][ぁ-ゟ゠-ヿ一-鿿]' OR t ~ '[ぁ-ゟ゠-ヿ一-鿿][가-힣]')
                                                            THEN def := array_append(def,'hangul_in_ja'); END IF;
    -- ja/zh: exotic script(키릴/태국/데바나가리/아랍) 인접 — 라틴 언어에만 있던 체크 확장
    IF p_lang IN ('ja','zh') AND t ~ '[Ѐ-ӿก-๛ऀ-ॿ؀-ۿ]'      THEN def := array_append(def,'exotic_in_cjk'); END IF;
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
  '리포트 번역 행의 스크립트섞임/통화누출 결함코드 배열 반환. 파이프라인 주입 전 게이트로 호출. (V29: ko 한자인접 + ja/zh exotic 추가)';

-- 기존 데이터 재평가(트리거는 INSERT/UPDATE 시에만 발동하므로 과거 행은 수동 백필 필요)
DELETE FROM report_quality_issue;
INSERT INTO report_quality_issue(report_id, lang, defects, detected_at)
SELECT report_id, lang, d, NOW()
FROM (
    SELECT report_id, lang,
           detect_report_translation_defects(lang, title, summary, body, content::text) AS d
    FROM university_report_translation
) x
WHERE array_length(d, 1) IS NOT NULL;
