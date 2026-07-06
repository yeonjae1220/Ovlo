-- ============================================================================
-- (A) ko 번역본 스크립트 파편 결정적 교정 — DRY-RUN (트랜잭션 후 ROLLBACK, 변경 미확정)
--   고신뢰 항목만: 오리엔테이션 계열 + 유명 고유명사 + 일본어 문법 누출 + 万원.
--   replace() 는 부분문자열 전체 치환이라, 조사 붙은 변형(…에/…을/…의)은 기반형만 넣어도 커버됨.
--   애매한 고유명사(카세ム·소ム·센야ン 등)는 사전에서 제외 → 잔여 가나 카운트로 REVIEW 규모 확인.
--   실제 적용은 맨 끝 ROLLBACK → COMMIT 으로 바꿔 실행.
-- ============================================================================
\pset pager off
BEGIN;

CREATE TEMP TABLE fix(bad text, good text);
INSERT INTO fix(bad, good) VALUES
  -- 오리엔테이션 계열(기반형)
  ('オリエンテーション','오리엔테이션'),
  ('オリ엔테이션','오리엔테이션'),
  ('オリエン테이션','오리엔테이션'),
  ('オリ엔테이ション','오리엔테이션'),
  ('オリ엔테ーション','오리엔테이션'),
  ('オリ엔テーション','오리엔테이션'),
  ('オリエンテ이션','오리엔테이션'),
  ('オリエンテイション','오리엔테이션'),
  ('オリエンテイ션','오리엔테이션'),
  -- 전부 일본어인 외래어
  ('スポンジケーキ','스펀지케이크'),
  ('サラダ','샐러드'),
  ('카츠도ん','카츠동'),
  ('스시ざんまい','스시잔마이'),
  ('야끼とり','야키토리'),
  -- 유명 고유명사(음차 파손)
  ('코ロン비아','콜롬비아'),
  ('스톡홀ム','스톡홀름'),
  ('ミュン헨','뮌헨'),
  ('ドレス덴','드레스덴'),
  ('オー클랜드','오클랜드'),
  ('비クト리아','빅토리아'),
  ('모ント고메리','몽고메리'),
  ('루이지アナ','루이지애나'),
  ('데リー','델리'),
  ('모ント펠리에','몽펠리에'),
  ('에디ン버그','에든버러'),
  ('아ユ타야','아유타야'),
  ('후마유ン','후마윤'),
  ('파르미ジャ노','파르미자노'),
  ('맵ル','메이플'),
  ('시ックス','식스'),
  ('사ンド턴','샌드턴'),
  ('나ショ널','내셔널'),
  ('마크투ム','마크툼'),
  ('파エリア','파에야'),
  ('타마レス','타말레스'),
  ('프로スペクト','프로스펙트'),
  -- 일본어 문법 통째 누출
  ('만나ることができます','만날 수 있습니다'),
  ('가르치られます','가르쳐집니다'),
  -- 통화 단위(万=만): ko 에서 万 은 항상 누출
  ('万원','만원');

-- 적용 전: ko 행 중 가나 잔존 행수
SELECT 'BEFORE: 가나 잔존 ko 행' AS metric,
       count(*) AS rows
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[ぁ-ゟ゠-ヿ]';

-- 사전 적용(ko 한정, 4개 필드)
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT bad, good FROM fix LOOP
    UPDATE university_report_translation SET
      title   = replace(title, r.bad, r.good),
      summary = replace(summary, r.bad, r.good),
      body    = replace(body, r.bad, r.good),
      content = replace(content::text, r.bad, r.good)::jsonb
    WHERE lang='ko'
      AND (title LIKE '%'||r.bad||'%' OR summary LIKE '%'||r.bad||'%'
        OR body LIKE '%'||r.bad||'%' OR content::text LIKE '%'||r.bad||'%');
  END LOOP;
END $$;

-- 적용 후: 가나 잔존 ko 행수 (= 남은 REVIEW 규모)
SELECT 'AFTER: 가나 잔존 ko 행' AS metric,
       count(*) AS rows
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[ぁ-ゟ゠-ヿ]';

-- 잔존 파편 목록(REVIEW 대상 = 사전 미포함 애매 항목)
SELECT report_id,
       substring(concat_ws(' ',title,coalesce(summary,''),body,content::text)
                 from '[가-힣A-Za-z0-9]*[ぁ-ゟ゠-ヿ]+[가-힣A-Za-z0-9]*') AS remaining_fragment
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[ぁ-ゟ゠-ヿ]'
ORDER BY report_id;

-- 교정 성공 샘플(오리엔테이션 정상화 확인)
SELECT report_id, substring(body from '.{0,6}오리엔테이션.{0,8}') AS fixed_sample
FROM university_report_translation
WHERE lang='ko' AND body LIKE '%오리엔테이션%'
ORDER BY report_id LIMIT 6;

ROLLBACK;  -- DRY-RUN: 변경 폐기. 실제 적용 시 COMMIT 으로 교체.
