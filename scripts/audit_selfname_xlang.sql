-- ============================================================================
-- 전 언어 자기명(self-name) 크로스언어 감사 — READ-ONLY 자동 triage
--   목적: university_report_translation 각 (report, lang)의 body 자기명이
--         그 리포트 대학의 검증 정본명(global_university_names)과 다른지 탐지.
--   핵심 아이디어(고정밀): body가 지칭하는 학교명 토큰을 전체 대학 카탈로그에
--         정규화 매칭 → "다른 실존 대학"에 매칭되면 SWAP 자동 확정, 아니면
--         (음차 변이/파손) UNRESOLVED로 분리. → 수동 확정 대상만 좁힘.
--   안전: 순수 SELECT + TEMP. 데이터 변경 없음. 자동 수정 아님(검토 리스트).
--
--   verdict:
--     SWAP        — body 토큰이 '다른' 대학의 검증명과 정규화 일치(고신뢰)
--     UNRESOLVED  — 자기 정본명도 없고 다른 대학과도 매칭 안 됨(음차변이/파손, 수동)
--   대상 언어: ja, zh (CJK gun-core 방식). Latin(en 등)은 하단 별도 뷰.
--     ※ de/fr/vi는 대학명 자체를 번역/서술형으로 바꿔(zero-anchor 26/55/52건)
--       오탐이 과다해 제외. 그 언어들은 en/ko 대조 수동 검토가 현실적.
--
-- 2026-07-25 최초 실행 기준선: SWAP 20건 발굴 → 진짜 12건 교정, 오탐 8건은
--   아래 3개 필터로 억제 → 재실행 시 SWAP 1건(r364 마스터 FK 오링크, 별도 사안).
--   UNRESOLVED 169건(대부분 음차변이=같은 학교), en anchor-missing 8건(약자/악센트).
--   오탐 억제 필터: ①토큰↔자기정본 접두 포함관계 ②라틴 폴백명 사용 ③최장 토큰 우선.
-- ============================================================================
\pset pager off
\set ON_ERROR_STOP on

-- 번체→간체 정규화 (institution 빈출 ~90쌍). 미포함 문자는 그대로 통과.
CREATE OR REPLACE FUNCTION pg_temp.ts_norm(s text) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT translate($1,
    '學國華東師範醫藝術農濟經財貿灣臺廣廈復觀圖書電機開關陽龍鳳島麗爾倫頓蘭韓齊澤薩羅盧鐵錦銀鐘專屬寧慶語讀萬亞區廳陸興漢場進運遠達邊門問隊際雙頭風養馬體點齒從眾會愛應業樂車長館範圓學藝聯',
    '学国华东师范医艺术农济经财贸湾台广厦复观图书电机开关阳龙凤岛丽尔伦顿兰韩齐泽萨罗卢铁锦银钟专属宁庆语读万亚区厅陆兴汉场进运远达边门问队际双头风养马体点齿从众会爱应业乐车长馆范圆学艺联');
$$;

-- 대학명 코어 추출: 접두(国立 등)·접미(大学校 등) 제거
CREATE OR REPLACE FUNCTION pg_temp.name_core(s text) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT regexp_replace(
           regexp_replace(coalesce($1,''), '^(国立|國立|私立|公立|州立|市立|国家)', ''),
           '(大学校|大學校|大学院|大學院|大学|大學|学院|學院|学園|學園|学园|大学分校|分校)$', '');
$$;

-- 전체 대학 카탈로그: 언어별 정규화 코어 (다른 대학 매칭용 룩업)
CREATE TEMP VIEW univ_core AS
SELECT gn.global_univ_id, gn.lang,
       pg_temp.ts_norm(pg_temp.name_core(gn.name)) AS core_norm,
       gu.name_en, gu.country_code
FROM global_university_names gn
JOIN global_universities gu ON gu.id = gn.global_univ_id
WHERE gn.lang IN ('ja','zh')
  AND length(regexp_replace(pg_temp.ts_norm(pg_temp.name_core(gn.name)),'[^一-鿿]','','g')) >= 2;

-- 리포트 자기 대학의 (lang) 코어
CREATE TEMP VIEW own_core AS
SELECT r.id AS report_id, uc.lang, uc.core_norm AS own_core, uc.name_en AS own_name_en, uc.country_code AS own_country
FROM university_report r
JOIN univ_core uc ON uc.global_univ_id = r.global_univ_id;

-- body에서 뽑은 자기명 후보 토큰
--   ⚠️ 자기명 판정은 "문서의 첫 대학 언급"으로 좁힌다(is_first).
--   상단 N자/빈도 방식은 "附近的墨尔本大学"(인근 대학 언급) 같은 정당한 타교
--   언급을 자기명으로 오인해 오탐을 만든다(r812 Monash 실측).
CREATE TEMP VIEW body_token AS
WITH raw AS (
  -- ⚠️ 大学 직전의 CJK 런을 최장(greedy)으로 캡처해야 한다. 접미 수식어(州立·工業 등)를
  --    옵션 접두로만 처리하면 "犹他州立大学"에서 "犹他"만 잡혀 Utah로 오매칭된다(r1058 실측).
  SELECT t.report_id, t.lang, t.body, m[1] AS tok_raw
  FROM university_report_translation t
  CROSS JOIN LATERAL regexp_matches(t.body,
    '([一-鿿]{2,10})(?:大学校|大學校|大学|大學|学院|學院)', 'g') m
  WHERE t.lang IN ('ja','zh')
),
pos AS (  -- 각 토큰의 body 내 첫 등장 문자위치(strpos) — 순서 판정의 근거
  SELECT report_id, lang, tok_raw,
         pg_temp.ts_norm(pg_temp.name_core(tok_raw)) AS tok_norm,
         strpos(body, tok_raw) AS at
  FROM raw
  WHERE length(regexp_replace(pg_temp.ts_norm(pg_temp.name_core(tok_raw)),'[^一-鿿]','','g')) >= 2
)
SELECT DISTINCT p.report_id, p.lang, p.tok_norm, p.tok_raw, p.at,
       -- 자기명 = 문서에서 가장 먼저 등장한 대학 언급(제목/첫 문장).
       -- 동률(같은 위치에서 시작)일 땐 가장 긴 토큰이 진짜 이름 — "犹他大学"과
       -- "犹他州立大学"이 같은 at을 가지면 후자를 자기명으로 본다(r1058).
       (p.at = min(p.at) OVER (PARTITION BY p.report_id, p.lang)
        AND length(p.tok_raw) = max(length(p.tok_raw)) OVER (
              PARTITION BY p.report_id, p.lang, p.at)) AS is_selfname
FROM pos p;

-- 제네릭 코어(대학명 아님) 제외 리스트
CREATE TEMP VIEW generic_core AS
SELECT unnest(ARRAY['科技','技术','技術','工科','工业','工業','理工','科学','科學','医科','师范','師範',
                    '国家','外国语','女子','美国','韩国','中国','日本','综合','城市','海洋']) AS g;

-- ============================================================================
-- 결과 1: SWAP 확정 — body 자기명이 '다른' 대학 검증명과 정규화 일치
-- ============================================================================
\echo '################  SWAP (high-confidence: body names a DIFFERENT real university)  ################'
SELECT DISTINCT
  bt.report_id, bt.lang,
  oc.own_name_en, oc.own_country,
  bt.tok_raw       AS body_selfname,
  m.name_en        AS matched_other,
  m.country_code   AS matched_country
FROM body_token bt
JOIN own_core oc ON oc.report_id = bt.report_id AND oc.lang = bt.lang
JOIN univ_core m ON m.lang = bt.lang AND m.core_norm = bt.tok_norm
                 AND m.global_univ_id <> (SELECT global_univ_id FROM university_report WHERE id = bt.report_id)
WHERE bt.is_selfname
  AND bt.tok_norm <> oc.own_core                          -- 자기 정본과 다름
  AND position(oc.own_core IN pg_temp.ts_norm(
        (SELECT body FROM university_report_translation t2 WHERE t2.report_id=bt.report_id AND t2.lang=bt.lang))) = 0  -- body에 자기 정본 아예 없음
  AND bt.tok_norm NOT IN (SELECT g FROM generic_core)
  -- 오탐 억제 1: body 토큰이 자기 정본 코어의 접두부(또는 반대)면 같은 학교의 축약형
  --   예) 利物浦(Liverpool) vs 利物浦约翰摩尔斯(LJMU), 牛津 vs 牛津布鲁克斯, 格拉斯哥 vs 格拉斯哥卡利多尼亚
  AND position(bt.tok_norm IN oc.own_core) = 0
  AND position(oc.own_core IN bt.tok_norm) = 0
  -- 오탐 억제 2: body가 name_en 라틴 표기를 쓰면 자기명 정상(라틴 폴백)
  --   예) "Youngsan大学", "Kookmin大学"
  AND NOT EXISTS (
    SELECT 1 FROM (
      SELECT lower(w) w FROM regexp_split_to_table(regexp_replace(oc.own_name_en,'[^A-Za-z ]',' ','g'),'\s+') w
      WHERE length(w) >= 4 AND lower(w) NOT IN ('university','college','institute','national','state','technology','technical','science','sciences')
    ) e
    WHERE strpos(lower((SELECT body FROM university_report_translation t3 WHERE t3.report_id=bt.report_id AND t3.lang=bt.lang)), e.w) > 0)
ORDER BY bt.report_id, bt.lang;

-- ============================================================================
-- 결과 2: UNRESOLVED — 자기 정본도 없고 다른 대학과도 매칭 안 됨(음차변이/파손)
--   (RESOLVED SWAP에 안 잡힌 것 중, 자기 정본이 body에 전혀 없는 리포트/언어)
-- ============================================================================
\echo ''
\echo '################  UNRESOLVED (self-name absent; translit-variant or garble — manual review)  ################'
SELECT oc.report_id, oc.lang, oc.own_name_en, oc.own_country,
  COALESCE((SELECT string_agg(DISTINCT bt.tok_raw, ', ')
            FROM body_token bt WHERE bt.report_id=oc.report_id AND bt.lang=oc.lang AND bt.is_selfname),'-') AS body_selfnames
FROM own_core oc
WHERE position(oc.own_core IN pg_temp.ts_norm(
        (SELECT body FROM university_report_translation t2 WHERE t2.report_id=oc.report_id AND t2.lang=oc.lang))) = 0
  -- SWAP로 이미 확정된 (report,lang) 제외
  AND NOT EXISTS (
    SELECT 1 FROM body_token bt
    JOIN univ_core m ON m.lang=bt.lang AND m.core_norm=bt.tok_norm
                     AND m.global_univ_id <> (SELECT global_univ_id FROM university_report WHERE id=bt.report_id)
    WHERE bt.report_id=oc.report_id AND bt.lang=oc.lang AND bt.is_selfname
      AND bt.tok_norm <> oc.own_core AND bt.tok_norm NOT IN (SELECT g FROM generic_core))
  -- name_en 라틴 토큰이 body에 있으면 라틴폴백(정상) → 제외
  AND NOT EXISTS (
    SELECT 1 FROM (
      SELECT lower(w) w FROM regexp_split_to_table(regexp_replace(oc.own_name_en,'[^A-Za-z ]',' ','g'),'\s+') w
      WHERE length(w)>=4 AND lower(w) NOT IN ('university','college','institute','national','state','technology','technical','science','sciences')
    ) e WHERE strpos(lower((SELECT body FROM university_report_translation t3 WHERE t3.report_id=oc.report_id AND t3.lang=oc.lang)), e.w)>0)
ORDER BY oc.report_id, oc.lang;

-- ============================================================================
-- 결과 3: Latin(en) 앵커 미검출 — en body에 name_en 유의어가 하나도 없음
--   (대부분 약자/악센트 오탐; 참고용. de/fr/vi는 이름 번역으로 오탐 과다라 제외)
-- ============================================================================
\echo ''
\echo '################  en anchor-missing (mostly abbreviation FPs; reference only)  ################'
WITH enw AS (
  SELECT r.id AS report_id, COALESCE(gu.name_en, eu.name_en) AS name_en, lower(w) AS w
  FROM university_report r
  LEFT JOIN global_universities gu ON gu.id=r.global_univ_id
  LEFT JOIN exchange_universities eu ON eu.id=r.exchange_univ_id,
  LATERAL regexp_split_to_table(regexp_replace(COALESCE(gu.name_en,eu.name_en),'[^A-Za-z ]',' ','g'),'\s+') w
  WHERE length(w)>=4 AND lower(w) NOT IN ('university','college','institute','national','state','technology','technical','science','sciences')
)
SELECT t.report_id, max(e.name_en) AS name_en
FROM university_report_translation t
JOIN enw e ON e.report_id = t.report_id
WHERE t.lang='en'
GROUP BY t.report_id
HAVING count(*) FILTER (WHERE strpos(lower(t.body), e.w) > 0) = 0   -- 앵커 단어가 하나도 없음
ORDER BY t.report_id;

-- ============================================================================
-- 결과 4: 요약 카운트
-- ============================================================================
\echo ''
\echo '################  SUMMARY  ################'
SELECT 'ja/zh (report,lang) pairs with gun name' AS metric, count(*)::text AS value FROM own_core
UNION ALL
SELECT 'SWAP (auto-confirmed)', count(*)::text FROM (
  SELECT DISTINCT bt.report_id, bt.lang FROM body_token bt
  JOIN own_core oc ON oc.report_id=bt.report_id AND oc.lang=bt.lang
  JOIN univ_core m ON m.lang=bt.lang AND m.core_norm=bt.tok_norm
                   AND m.global_univ_id <> (SELECT global_univ_id FROM university_report WHERE id=bt.report_id)
  WHERE bt.is_selfname AND bt.tok_norm <> oc.own_core
    AND bt.tok_norm NOT IN (SELECT g FROM generic_core)
    AND position(oc.own_core IN pg_temp.ts_norm((SELECT body FROM university_report_translation t2 WHERE t2.report_id=bt.report_id AND t2.lang=bt.lang)))=0
    AND position(bt.tok_norm IN oc.own_core)=0 AND position(oc.own_core IN bt.tok_norm)=0
    AND NOT EXISTS (SELECT 1 FROM (
      SELECT lower(w) w FROM regexp_split_to_table(regexp_replace(oc.own_name_en,'[^A-Za-z ]',' ','g'),'\s+') w
      WHERE length(w)>=4 AND lower(w) NOT IN ('university','college','institute','national','state','technology','technical','science','sciences')
    ) e WHERE strpos(lower((SELECT body FROM university_report_translation t3 WHERE t3.report_id=bt.report_id AND t3.lang=bt.lang)), e.w)>0)
) s;
