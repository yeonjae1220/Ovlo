-- ============================================================================
-- 본문/제목/요약/content 스크립트 파편 추출 (READ-ONLY, CSV export)
--   출력: report_id, lang, field, fragment, occurrences_here, high_confidence
--   high_confidence = ko(가나는 무조건 결함) OR 한 토큰이 2종 이상 문자체계 혼합
--                     (예: 아姆斯特丹=한글+한자) → 진짜 결함.
--   low(=false)     = 비-ko 에서 단일 체계 토큰(예: en 본문의 '(한인 학생회)' 병기)
--                     → 정상 병기(오탐) 가능성 → 육안 확인 대상.
--   COPY ... TO STDOUT 으로 스트리밍(psql -f, 클라이언트로 CSV 수신).
-- ============================================================================
COPY (
  WITH pf AS (
    SELECT report_id, lang, 'title'   AS field, title          AS txt FROM university_report_translation
    UNION ALL SELECT report_id, lang, 'summary', summary               FROM university_report_translation
    UNION ALL SELECT report_id, lang, 'body',    body                  FROM university_report_translation
    UNION ALL SELECT report_id, lang, 'content', content::text         FROM university_report_translation
  ),
  pat AS (
    SELECT report_id, lang, field, txt,
      CASE lang
        WHEN 'ko' THEN '[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*[ぁ-ゟ゠-ヿ]+[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*'
        WHEN 'ja' THEN '[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*[가-힣]+[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*'
        WHEN 'zh' THEN '[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*[가-힣ぁ-ゟ゠-ヿ]+[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*'
        ELSE           '[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*[가-힣ぁ-ゟ゠-ヿ一-鿿]+[가-힣ぁ-ゟ゠-ヿ一-鿿A-Za-z0-9]*'
      END AS re
    FROM pf WHERE txt IS NOT NULL AND txt <> ''
  ),
  hits AS (
    SELECT report_id, lang, field,
           unnest(regexp_matches(txt, re, 'g')) AS fragment
    FROM pat
  ),
  agg AS (
    SELECT report_id, lang, field, fragment, count(*) AS n
    FROM hits
    WHERE btrim(fragment) <> '' AND char_length(btrim(fragment)) >= 2
    GROUP BY report_id, lang, field, fragment
  )
  SELECT
    report_id, lang, field, fragment, n AS occurrences_here,
    (lang = 'ko'
      OR ( (fragment ~ '[A-Za-z0-9]')::int
         + (fragment ~ '[가-힣]')::int
         + (fragment ~ '[ぁ-ゟ゠-ヿ]')::int
         + (fragment ~ '[一-鿿]')::int ) >= 2
    ) AS high_confidence
  FROM agg
  ORDER BY high_confidence DESC, lang, report_id, field
) TO STDOUT WITH CSV HEADER;
