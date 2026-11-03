-- ============================================================================
-- V40: 한국 대학 마스터 보강 — 한글명 결손 76건, 중복 행 31건, 검색 별칭, 고려대 도메인
-- ----------------------------------------------------------------------------
-- 배경(2026-11-03): 한국 대학 239행 중 76행에 한글 이름이 전혀 없어 "서울기독대"처럼
-- 모국어로 검색이 안 됐고, 옛 로마자 표기 행(Cheju/Chonju/Taegu…)이 현행 행과 나란히
-- 검색·카탈로그에 두 번 나왔다. 행은 도메인으로 식별한다(id 는 환경마다 다를 수 있음).
-- 빈 환경(테스트 DB)에서는 모두 no-op. 재실행 안전:
--   * 한글명: 없으면 넣고, 있어도 한글이 아닌(라틴 문자) 값이면 덮어쓴다(source=manual).
--   * 중복: 이미 canonical_id 가 있으면 건드리지 않는다. 대표 행은 canonical_id 가 NULL 인 행만.
--   * 별칭: UNIQUE(global_univ_id, alias) 충돌은 무시.
-- 숨기는 31행은 2026-11-03 운영 실측에서 후기 0·리포트 0·회원 0 이었다(교환대학 행만 1개씩).
-- 되돌리기: canonical_id = NULL, DELETE FROM global_university_names WHERE source='manual'
--   AND lang='ko' AND ... (이 파일의 도메인 목록), DELETE FROM global_university_aliases.
-- ============================================================================

-- ── 1. 한글명 결손 76건 (domain → 한글명) ─────────────────────────────────────
WITH ko(domain, name) AS (VALUES
    ('acts.ac.kr',          '아세아연합신학대학교'),
    ('cha.ac.kr',           '차의과학대학교'),
    ('cataegu.ac.kr',       '대구가톨릭대학교'),
    ('cheju.ac.kr',         '제주대학교'),
    ('cheju-e.ac.kr',       '제주교육대학교'),
    ('cje.ac.kr',           '청주교육대학교'),
    ('chinju.ac.kr',        '진주산업대학교'),
    ('howon.ac.kr',         '호원대학교'),
    ('chongju.ac.kr',       '청주대학교'),
    ('chonju-e.ac.kr',      '전주교육대학교'),
    ('cnue-e.ac.kr',        '춘천교육대학교'),
    ('chungju.ac.kr',       '충주대학교'),
    ('daebul.ac.kr',        '대불대학교'),
    ('dcatholic.ac.kr',     '대전가톨릭대학교'),
    ('daejeon.ac.kr',       '대전신학대학교'),
    ('daeshin.ac.kr',       '대신대학교'),
    ('dongeui.ac.kr',       '동의대학교'),
    ('dytc.ac.kr',          '동양미래대학교'),
    ('gnue.ac.kr',          '광주교육대학교'),
    ('htus.ac.kr',          '호남신학대학교'),
    ('inchon-e.ac.kr',      '인천교육대학교'),
    ('inchon.ac.kr',        '인천대학교'),
    ('icu.ac.kr',           '한국정보통신대학교'),
    ('jeju.ac.kr',          '제주국제대학교'),
    ('jesus.ac.kr',         '예수대학교'),
    ('sangha.ac.kr',        '중앙승가대학교'),
    ('jwu.ac.kr',           '중원대학교'),
    ('kcu.ac.kr',           'KC대학교'),
    ('kangnung.ac.kr',      '강릉대학교'),
    ('kaya.ac.kr',          '가야대학교'),
    ('kkot.ac.kr',          '꽃동네대학교'),
    ('kongju-e.ac.kr',      '공주교육대학교'),
    ('kau.ac.kr',           '한국항공대학교'),
    ('kafna.ac.kr',         '국군간호사관학교'),
    ('kmaritime.ac.kr',     '한국해양대학교'),
    ('knupe.ac.kr',         '한국체육대학교'),
    ('bible.ac.kr',         '한국성서대학교'),
    ('kwandong.ac.kr',      '관동대학교'),
    ('kwangju.ac.kr',       '광주대학교'),
    ('kwu.ac.kr',           '광주여자대학교'),
    ('kwangshin.ac.kr',     '광신대학교'),
    ('kduniv.ac.kr',        '경동대학교'),
    ('kyunghee.ac.kr',      '경희대학교'),
    ('kyungil.ac.kr',       '경일대학교'),
    ('kyungwon.ac.kr',      '경원대학교'),
    ('ikw.ac.kr',           '경운대학교'),
    ('mtu.ac.kr',           '감리교신학대학교'),
    ('nambu.ac.kr',         '남부대학교'),
    ('paichai.ac.kr',       '배재대학교'),
    ('puts.ac.kr',          '장로회신학대학교'),
    ('pusan-e.ac.kr',       '부산교육대학교'),
    ('pufs.ac.kr',          '부산외국어대학교'),
    ('pwc.ac.kr',           '부산여자대학교'),
    ('ptuniv.ac.kr',        '평택대학교'),
    ('samchok.ac.kr',       '삼척대학교'),
    ('sangji.ac.kr',        '상지대학교'),
    ('sangmyung.ac.kr',     '상명대학교'),
    ('semyung.ac.kr',       '세명대학교'),
    ('seonam.ac.kr',        '서남대학교'),
    ('scu.ac.kr',           '서울기독대학교'),
    ('uos.ac.kr',           '서울시립대학교'),
    ('hytu.ac.kr',          '서울한영대학교'),
    ('snut.ac.kr',          '서울산업대학교'),
    ('shinhan.ac.kr',       '신한대학교'),
    ('songwon.ac.kr',       '송원대학교'),
    ('skku.ac.kr',          '성균관대학교'),
    ('sunmoon.ac.kr',       '선문대학교'),
    ('suwoncatholic.ac.kr', '수원가톨릭대학교'),
    ('taegu-e.ac.kr',       '대구교육대학교'),
    ('taegu.ac.kr',         '대구대학교'),
    ('taejae.ac.kr',        '태재대학교'),
    ('tnut.ac.kr',          '대전산업대학교'),
    ('taejon.ac.kr',        '대전대학교'),
    ('tit.ac.kr',           '동명정보대학교'),
    ('u1.ac.kr',            '유원대학교'),
    ('yosu.ac.kr',          '여수대학교')
)
INSERT INTO global_university_names (global_univ_id, lang, name, source)
SELECT g.id, 'ko', ko.name, 'manual'
FROM ko JOIN global_universities g ON g.domain = ko.domain AND g.country_code = 'KR'
ON CONFLICT (global_univ_id, lang) DO UPDATE
    SET name = EXCLUDED.name, source = 'manual'
    WHERE global_university_names.name !~ '[가-힣]';   -- 라틴 문자뿐인 ko 라벨만 교체

-- ── 2. 고려대학교 도메인 교정 ──────────────────────────────────────────────────
-- 마스터의 'Korea University'(KR, 고려대학교) 행이 일본 朝鮮大学校 도메인(korea-u.ac.jp)을
-- 갖고 있어 고려대 학생(korea.ac.kr)이 학교 메일로 인증할 수 없었다.
UPDATE global_universities
SET domain = 'korea.ac.kr', website = 'https://www.korea.ac.kr'
WHERE country_code = 'KR' AND name_en = 'Korea University' AND domain = 'korea-u.ac.jp'
  AND NOT EXISTS (SELECT 1 FROM global_universities x WHERE x.domain = 'korea.ac.kr');

-- ── 3. 중복 행 숨김 31건 (옛 표기·통합 전 학교 → 현행 대표 행) ────────────────────
WITH dup(hidden_domain, canonical_domain) AS (VALUES
    ('cheju.ac.kr',     'jejunu.ac.kr'),     -- 제주대 옛 도메인
    ('cheju-e.ac.kr',   'jejunu.ac.kr'),     -- 제주교대 → 제주대 통합(2008)
    ('chinju.ac.kr',    'gsnu.ac.kr'),       -- 진주산업대 → 경남과기대 → 경상국립대(2021)
    ('gntech.ac.kr',    'gsnu.ac.kr'),       -- 경남과학기술대 → 경상국립대 통합(2021)
    ('chongju.ac.kr',   'cju.ac.kr'),        -- 청주대 옛 표기
    ('chonju-e.ac.kr',  'jnue.ac.kr'),       -- 전주교대 옛 표기
    ('cnue-e.ac.kr',    'cnue.ac.kr'),       -- 춘천교대 옛 표기
    ('daebul.ac.kr',    'sehan.ac.kr'),      -- 대불대 → 세한대 개명(2012)
    ('inchon-e.ac.kr',  'ginue.ac.kr'),      -- 인천교대 → 경인교대 통합(2003)
    ('inchon.ac.kr',    'inu.ac.kr'),        -- 인천대 옛 표기
    ('icu.ac.kr',       'kaist.ac.kr'),      -- 한국정보통신대 → KAIST 통합(2009)
    ('kangnung.ac.kr',  'gwnu.ac.kr'),       -- 강릉대 → 강릉원주대(2009)
    ('kongju-e.ac.kr',  'gjue.ac.kr'),       -- 공주교대 옛 표기
    ('kmaritime.ac.kr', 'kmou.ac.kr'),       -- 한국해양대 옛 표기
    ('knupe.ac.kr',     'knsu.ac.kr'),       -- 한국체육대 옛 표기
    ('kwandong.ac.kr',  'cku.ac.kr'),        -- 관동대 → 가톨릭관동대(2014)
    ('kyunghee.ac.kr',  'khu.ac.kr'),        -- 경희대 옛 표기
    ('kyungwon.ac.kr',  'gachon.ac.kr'),     -- 경원대 → 가천대 통합(2012)
    ('paichai.ac.kr',   'pcu.ac.kr'),        -- 배재대 옛 표기
    ('pusan-e.ac.kr',   'bnue.ac.kr'),       -- 부산교대 옛 표기
    ('pufs.ac.kr',      'bufs.ac.kr'),       -- 부산외대 옛 표기
    ('ptuniv.ac.kr',    'ptu.ac.kr'),        -- 평택대 옛 표기
    ('samchok.ac.kr',   'kangwon.ac.kr'),    -- 삼척대 → 강원대 통합(2006)
    ('sangmyung.ac.kr', 'smu.ac.kr'),        -- 상명대 옛 표기
    ('snut.ac.kr',      'seoultech.ac.kr'),  -- 서울산업대 → 서울과기대(2010)
    ('skku.ac.kr',      'skku.edu'),         -- 성균관대 두 도메인
    ('taegu-e.ac.kr',   'dnue.ac.kr'),       -- 대구교대 옛 표기
    ('taegu.ac.kr',     'daegu.ac.kr'),      -- 대구대 옛 표기
    ('tnut.ac.kr',      'hanbat.ac.kr'),     -- 대전산업대 → 한밭대(2001)
    ('taejon.ac.kr',    'dju.ac.kr'),        -- 대전대 옛 표기
    ('yosu.ac.kr',      'chonnam.ac.kr')     -- 여수대 → 전남대 통합(2006)
)
UPDATE global_universities h
SET canonical_id = c.id
FROM dup
JOIN global_universities c ON c.domain = dup.canonical_domain AND c.country_code = 'KR' AND c.canonical_id IS NULL
WHERE h.domain = dup.hidden_domain AND h.country_code = 'KR'
  AND h.canonical_id IS NULL AND h.id <> c.id;

-- ── 4. 검색 별칭 (약어·옛 이름·다른 표기) ────────────────────────────────────────
WITH al(domain, alias) AS (VALUES
    ('yonsei.ac.kr',     '연대'),
    ('korea.ac.kr',      '고대'),
    ('skku.edu',         '성대'),
    ('cau.ac.kr',        '중대'),
    ('hufs.ac.kr',       '한국외대'),   ('hufs.ac.kr',  '외대'),
    ('bufs.ac.kr',       '부산외대'),
    ('dufs.ac.kr',       '대구외대'),
    ('uos.ac.kr',        '시립대'),     ('uos.ac.kr',   '서울시립대'),  ('uos.ac.kr', 'University of Seoul'),
    ('seoultech.ac.kr',  '서울과기대'), ('seoultech.ac.kr', '과기대'), ('seoultech.ac.kr', '서울산업대'),
    ('seoul-e.ac.kr',    '서울교대'),
    ('bnue.ac.kr',       '부산교대'),
    ('ginue.ac.kr',      '경인교대'),   ('ginue.ac.kr', '인천교대'),
    ('dnue.ac.kr',       '대구교대'),
    ('gnue.ac.kr',       '광주교대'),
    ('jnue.ac.kr',       '전주교대'),
    ('chinju-e.ac.kr',   '진주교대'),
    ('cje.ac.kr',        '청주교대'),
    ('cnue.ac.kr',       '춘천교대'),
    ('gjue.ac.kr',       '공주교대'),
    ('ewha.ac.kr',       '이대'),
    ('sookmyung.ac.kr',  '숙대'),
    ('hongik.ac.kr',     '홍대'),
    ('konkuk.ac.kr',     '건대'),
    ('dongguk.ac.kr',    '동대'),
    ('kaist.ac.kr',      '카이스트'),
    ('postech.ac.kr',    '포항공대'),
    ('gist.ac.kr',       '지스트'),
    ('dgist.ac.kr',      '디지스트'),
    ('unist.ac.kr',      '유니스트'),
    ('gsnu.ac.kr',       '경상대'),     ('gsnu.ac.kr',  '경남과기대'), ('gsnu.ac.kr', '진주산업대'),
    ('kumoh.ac.kr',      '금오공대'),
    ('chungju.ac.kr',    '한국교통대학교'), ('chungju.ac.kr', '한국교통대'), ('chungju.ac.kr', '교통대'),
    ('chungju.ac.kr',    'Korea National University of Transportation'),
    ('koreatech.ac.kr',  '한기대'),     ('koreatech.ac.kr', '코리아텍'),
    ('knua.ac.kr',       '한예종'),
    ('knsu.ac.kr',       '한체대'),
    ('knou.ac.kr',       '방통대'),     ('knou.ac.kr',  '방송대'),
    ('hknu.ac.kr',       '한경대'),
    ('andong.ac.kr',     '안동대학교'), ('andong.ac.kr', '안동대'),
    ('cataegu.ac.kr',    '대가대'),     ('cataegu.ac.kr', '대구효성가톨릭대학교'), ('cataegu.ac.kr', 'Daegu Catholic University'),
    ('stu.ac.kr',        '서울신대'),
    ('puts.ac.kr',       '장신대'),
    ('mtu.ac.kr',        '감신대'),
    ('kbtus.ac.kr',      '침신대'),
    ('kma.ac.kr',        '육사'),       ('kma.ac.kr',   '육군사관학교'),
    ('navy.ac.kr',       '해사'),       ('navy.ac.kr',  '해군사관학교'),
    ('afa.ac.kr',        '공사'),       ('afa.ac.kr',   '공군사관학교'),
    ('tit.ac.kr',        '동명대학교'), ('tit.ac.kr',   '동명대'), ('tit.ac.kr', 'Tongmyong University'),
    ('u1.ac.kr',         '영동대학교'), ('u1.ac.kr',    '영동대'),
    ('hytu.ac.kr',       '한영신학대학교'), ('hytu.ac.kr', '한영대'),
    ('cha.ac.kr',        '차의과대'),   ('cha.ac.kr',   '차의과학대'),
    ('kcu.ac.kr',        '그리스도대학교')
)
INSERT INTO global_university_aliases (global_univ_id, alias, source)
SELECT g.id, al.alias, 'manual'
FROM al JOIN global_universities g ON g.domain = al.domain AND g.country_code = 'KR'
ON CONFLICT (global_univ_id, alias) DO NOTHING;
