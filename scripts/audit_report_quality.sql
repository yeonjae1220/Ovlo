-- ============================================================================
-- 대학 리포트 콘텐츠 품질 감사 (READ-ONLY)
-- ----------------------------------------------------------------------------
-- 목적: university_report_translation 전체를 훑어 2종 결함을 기계적으로 탐지한다.
--   부류 A (엔티티 혼동): title이 링크된 대학이 아닌 '다른 대학명'을 담거나,
--                         자기 대학명을 담지 않음. (예: 부경대 리포트인데 title="부산대학교")
--   부류 B (미번역 파편): 번역 언어에 나오면 안 되는 문자 체계가 섞임.
--                         (예: ko 번역에 가타카나 'オリエンテーション' 잔존)
--
-- 특징: 순수 SELECT 만 사용(데이터 변경 없음). 운영 DB에 안전하게 실행 가능.
-- 실행: scripts/run_report_audit.sh 참고 (k8s postgres 파드에서 psql -f)
--
-- 문자 범위 참고:
--   Hangul 음절 [가-힣], Jamo [ㄱ-ㅣ] / Hiragana [ぁ-ゟ] / Katakana [゠-ヿ]
--   CJK Han(한자/漢字) [一-鿿]
-- 주의: ko 리포트에 일본/중국 대학의 '한자 고유명'이 등장하는 건 정상이므로,
--       ko에 대해선 한자(Han)를 결함으로 보지 않고 '가나(kana)'만 결함으로 본다.
-- ============================================================================

\pset pager off
\pset format aligned

-- 여러 문장이 공유하도록 TEMP VIEW 로 정의한다(세션 한정·실데이터 불변, 순수 read-only).
-- 감사 대상 텍스트 = title + summary + body + content(JSONB→text) 를 한 덩어리로.
-- content 를 text 로 캐스팅하면 visa/housing 등 중첩 섹션까지 구조 무관하게 훑는다.
CREATE TEMP VIEW trans AS
    SELECT
        t.report_id,
        t.lang,
        t.title,
        t.summary,
        t.body,
        t.content::text                                    AS content_txt,
        concat_ws(' ', t.title, coalesce(t.summary,''), t.body, t.content::text) AS txt
    FROM university_report_translation t;

-- ── 알려진 한국어 대학명 사전 (부류 A 대조용) ────────────────────────────────
-- exchange_universities.name_ko(78개 분석 대학) ∪ global_universities.local_name(현지어명 중 한글)
CREATE TEMP VIEW univ_names AS
    SELECT DISTINCT name_ko AS nm
    FROM exchange_universities
    WHERE name_ko ~ '[가-힣]' AND char_length(name_ko) >= 3
    UNION
    SELECT DISTINCT local_name
    FROM global_universities
    WHERE local_name ~ '[가-힣]' AND char_length(local_name) >= 3;

-- ── 리포트별 '자기 대학'의 한국어명 (exchange 우선, 없으면 global.local_name) ──
CREATE TEMP VIEW report_self AS
    SELECT
        r.id AS report_id,
        COALESCE(eu.name_ko, gu.local_name) AS self_ko
    FROM university_report r
    LEFT JOIN exchange_universities eu ON eu.id = r.exchange_univ_id
    LEFT JOIN global_universities   gu ON gu.id = r.global_univ_id;

-- ════════════════════════════════════════════════════════════════════════════
-- ① 요약: 결함 건수 집계
-- ════════════════════════════════════════════════════════════════════════════
SELECT '── ① 요약 (defect counts) ──' AS section;

SELECT
    'B: 미번역 파편 (script leak)' AS defect_class,
    COUNT(*)                       AS rows
FROM trans
WHERE (lang = 'ko' AND txt ~ '[ぁ-ゟ゠-ヿ]')                        -- ko 에 가나
   OR (lang = 'ja' AND txt ~ '[가-힣]')                            -- ja 에 한글
   OR (lang = 'zh' AND txt ~ '[가-힣ぁ-ゟ゠-ヿ]')                   -- zh 에 한글/가나
   OR (lang IN ('en','fr','de','es','vi') AND txt ~ '[가-힣ぁ-ゟ゠-ヿ一-鿿]') -- 라틴 언어에 한글/가나/한자
UNION ALL
SELECT
    'A1: title 에 타 대학명 혼입',
    COUNT(*)
FROM trans t
JOIN report_self rs ON rs.report_id = t.report_id
WHERE t.lang = 'ko' AND rs.self_ko IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM univ_names un
      WHERE t.title LIKE '%' || un.nm || '%'
        AND un.nm <> rs.self_ko
        AND rs.self_ko NOT LIKE '%' || un.nm || '%'  -- self 가 타명을 포함하는 부분문자열 관계 제외
  )
UNION ALL
SELECT
    'A2: title 에 자기 대학명 누락',
    COUNT(*)
FROM trans t
JOIN report_self rs ON rs.report_id = t.report_id
WHERE t.lang = 'ko' AND rs.self_ko IS NOT NULL
  AND t.title NOT LIKE '%' || rs.self_ko || '%'
UNION ALL
-- A3: 마스터 한글명이 없어도 동작하는 '리포트 내부 정합성' 검사.
--     ko title 에서 뽑은 대학명 어근(stem)이 자기 body 에 등장하지 않으면 의심.
--     ⚠️ 약신호: title 접미사 형식 다양성/ body 표기 차이로 오탐 가능 → 육안 확인 필요.
--     (전체 리포트의 89%가 self_ko NULL 이라, 부경대(부산대 오기) 같은 케이스는 A1/A2 가 아닌 A3 로만 잡힘)
SELECT
    'A3: title 대학명이 body 에 없음(내부 불일치)',
    COUNT(*)
FROM trans t
CROSS JOIN LATERAL (
    SELECT regexp_replace(
        regexp_replace(t.title,
            '\s*(교환\s*학생\s*가이드|교환학생\s*가이드|학업\s*안내서|학업\s*가이드|안내서|가이드)\s*$', '', 'gi'),
        '(여자대학교|대학교|대학|대)\s*[:：]?\s*$', '') AS stem
) x
WHERE t.lang = 'ko'
  AND x.stem ~ '[가-힣]'
  AND char_length(x.stem) BETWEEN 2 AND 20
  AND t.body NOT LIKE '%' || x.stem || '%';

-- ════════════════════════════════════════════════════════════════════════════
-- ② 부류 B 상세: 어느 필드에 어떤 문자 체계가 샜는지
-- ════════════════════════════════════════════════════════════════════════════
SELECT '── ② 부류 B 상세 (script leak detail) ──' AS section;

SELECT
    t.report_id,
    t.lang,
    -- 어느 필드에서 발견됐는지
    CASE
        WHEN t.title       ~ leak.re THEN 'title'
        WHEN t.summary     ~ leak.re THEN 'summary'
        WHEN t.body        ~ leak.re THEN 'body'
        WHEN t.content_txt ~ leak.re THEN 'content'
    END AS field,
    -- 샌 조각 첫 매치 주변 발췌
    substring(t.txt from '.{0,15}' || leak.re || '.{0,15}') AS sample
FROM trans t
CROSS JOIN LATERAL (
    SELECT CASE
        WHEN t.lang = 'ko'                          THEN '[ぁ-ゟ゠-ヿ]'
        WHEN t.lang = 'ja'                          THEN '[가-힣]'
        WHEN t.lang = 'zh'                          THEN '[가-힣ぁ-ゟ゠-ヿ]'
        WHEN t.lang IN ('en','fr','de','es','vi')   THEN '[가-힣ぁ-ゟ゠-ヿ一-鿿]'
        ELSE NULL
    END AS re
) leak
WHERE leak.re IS NOT NULL AND t.txt ~ leak.re
ORDER BY t.report_id, t.lang;

-- ════════════════════════════════════════════════════════════════════════════
-- ③ 부류 A 상세: title 엔티티 혼동
-- ════════════════════════════════════════════════════════════════════════════
SELECT '── ③ 부류 A 상세 (entity mismatch detail) ──' AS section;

-- A1: title 에 링크된 대학이 아닌 다른 대학명이 들어감 (강한 신호)
SELECT
    'A1' AS kind,
    t.report_id,
    rs.self_ko            AS linked_univ,
    un.nm                 AS foreign_univ_in_title,
    t.title
FROM trans t
JOIN report_self rs ON rs.report_id = t.report_id
JOIN univ_names un
  ON t.title LIKE '%' || un.nm || '%'
 AND un.nm <> rs.self_ko
 AND rs.self_ko NOT LIKE '%' || un.nm || '%'
WHERE t.lang = 'ko' AND rs.self_ko IS NOT NULL

UNION ALL

-- A2: 자기 대학명이 title 에 아예 없음 (약한 신호 — 오탐 가능, 육안 확인 필요)
SELECT
    'A2' AS kind,
    t.report_id,
    rs.self_ko           AS linked_univ,
    NULL                 AS foreign_univ_in_title,
    t.title
FROM trans t
JOIN report_self rs ON rs.report_id = t.report_id
WHERE t.lang = 'ko' AND rs.self_ko IS NOT NULL
  AND t.title NOT LIKE '%' || rs.self_ko || '%'
ORDER BY kind, report_id;

-- A3: 리포트 내부 정합성 (title 대학명 어근이 body 에 없음). self_ko 불필요 → 89% 미링크 리포트도 커버.
SELECT
    'A3' AS kind,
    t.report_id,
    x.stem                                              AS title_stem,
    substring(t.body from '[가-힣]{2,}(?:여자대학교|대학교|대학|대)') AS first_univ_in_body,
    left(t.title, 40)                                   AS title
FROM trans t
CROSS JOIN LATERAL (
    SELECT regexp_replace(
        regexp_replace(t.title,
            '\s*(교환\s*학생\s*가이드|교환학생\s*가이드|학업\s*안내서|학업\s*가이드|안내서|가이드)\s*$', '', 'gi'),
        '(여자대학교|대학교|대학|대)\s*[:：]?\s*$', '') AS stem
) x
WHERE t.lang = 'ko'
  AND x.stem ~ '[가-힣]'
  AND char_length(x.stem) BETWEEN 2 AND 20
  AND t.body NOT LIKE '%' || x.stem || '%'
ORDER BY t.report_id;
