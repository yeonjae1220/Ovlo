-- ============================================================================
-- (A확장) ko 스크립트 파편 결정적 교정 — 68항목 (원본 39 + 문맥교정 29)
--   맨 끝 명령이 ROLLBACK 이면 dry-run, COMMIT 이면 실제 적용.
--   백업: scripts/ko_translations_backup_20260705.csv (사전 생성).
--   미포함(불확실 6 / 미상 5)은 별도 REVIEW — 사람 확인 후 2차 패스.
-- ============================================================================
\pset pager off
BEGIN;

CREATE TEMP TABLE fix(bad text, good text);
INSERT INTO fix(bad, good) VALUES
  -- ── 원본 39 (고신뢰) ──────────────────────────────────────────
  ('オリエンテーション','오리엔테이션'),('オリ엔테이션','오리엔테이션'),
  ('オリエン테이션','오리엔테이션'),('オリ엔테이ション','오리엔테이션'),
  ('オリ엔테ーション','오리엔테이션'),('オリ엔テーション','오리엔테이션'),
  ('オリエンテ이션','오리엔테이션'),('オリエンテイション','오리엔테이션'),
  ('オリエンテイ션','오리엔테이션'),
  ('スポンジケーキ','스펀지케이크'),('サラダ','샐러드'),
  ('카츠도ん','카츠동'),('스시ざんまい','스시잔마이'),('야끼とり','야키토리'),
  ('코ロン비아','콜롬비아'),('스톡홀ム','스톡홀름'),('ミュン헨','뮌헨'),
  ('ドレス덴','드레스덴'),('オー클랜드','오클랜드'),('비クト리아','빅토리아'),
  ('모ント고메리','몽고메리'),('루이지アナ','루이지애나'),('데リー','델리'),
  ('모ント펠리에','몽펠리에'),('에디ン버그','에든버러'),('아ユ타야','아유타야'),
  ('후마유ン','후마윤'),('파르미ジャ노','파르미자노'),('맵ル','메이플'),
  ('시ックス','식스'),('사ンド턴','샌드턴'),('나ショ널','내셔널'),
  ('마크투ム','마크툼'),('파エリア','파에야'),('타마レス','타말레스'),
  ('프로スペクト','프로스펙트'),
  ('만나ることができます','만날 수 있습니다'),('가르치られます','가르쳐집니다'),
  ('万원','만원'),
  -- ── 문맥교정 29 (2026-07-05, 주변 문맥/영어병기로 식별) ──────────
  ('스트로우와เฟル','스트룹와플'),     -- stroopwafel (암스테르담)
  ('타ンド리','탄두리'),               -- Tandoori
  ('린кольン','링컨'),                 -- Lincoln Memorial (Cyrillic 혼입)
  ('펜ギ스','펭귄스'),                 -- Penguins (피츠버그)
  ('비ーン스','빈스'),('비ーン','빈'), -- (red) beans  ※긴 것 먼저
  ('채타고ン','치타공'),               -- Chittagong (다카 인근)
  ('페이트ム','페이티엠'),             -- Paytm (인도 결제)
  ('포ン테','폰테'),                   -- Ponte Vecchio
  ('모ント트레블랑','몽트랑블랑'),     -- Mont-Tremblant
  ('살라ム','살람'),                   -- Salam Park (병기 확인)
  ('솔레ント','솔렌트'),               -- Solent University
  ('쿠ン','쿤'),                       -- Kuntsevskaya (모스크바)
  ('소리ャнка','솔랸카'),             -- Solyanka (러시아 수프, Cyrillic 혼입)
  ('그리피ント운','그리핀타운'),       -- Griffintown (몬트리올)
  ('하라ム','하람'),                   -- Al Haram
  ('클라ム','클램'),                   -- clam chowder
  ('티ム 쇼 쇼','침사추이'),           -- Tsim Sha Tsui (병기 확인)
  ('소토 야ム','소토 아얌'),           -- Soto Ayam
  ('코ン카엔','콘깬'),                 -- Khon Kaen
  ('토마 유ム','똠얌'),               -- Tom Yum
  ('맥시ム','맥심'),                   -- Maxim (라이드 앱)
  ('실레ント로','칠렌토'),             -- Cilento 국립공원 (나폴리)
  ('오보ん','오봉'),                   -- Obon (일본 명절)
  ('소ム 터','솜땀'),('소마 타ム','솜땀'), -- Som Tum (병기 확인)
  ('라이ム','라임'),                   -- Laim (뮌헨 지역)
  ('에마ム','에맘');                   -- Emam Khomeini (테헤란)

-- 적용 전 가나 잔존 ko 행수
SELECT 'BEFORE' AS phase, count(*) AS kana_rows
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[ぁ-ゟ゠-ヿ]';

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

-- 적용 후 가나 잔존 (= 남은 REVIEW)
SELECT 'AFTER' AS phase, count(*) AS kana_rows
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[ぁ-ゟ゠-ヿ]';

-- 남은 파편 목록
SELECT report_id,
       substring(concat_ws(' ',title,coalesce(summary,''),body,content::text)
                 from '[가-힣A-Za-z0-9]*[ぁ-ゟ゠-ヿ]+[가-힣A-Za-z0-9]*') AS remaining
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[ぁ-ゟ゠-ヿ]'
ORDER BY report_id;

COMMIT;  -- 실제 적용 (2026-07-05). 되돌리려면 scripts/ko_translations_backup_20260705.csv 사용.
