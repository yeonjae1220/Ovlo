-- ============================================================================
-- V58: 한국 대학 마스터 — 폐교 5개교 숨김, 통합 4개교를 현행 학교로 연결
-- ----------------------------------------------------------------------------
-- 배경(2026-11-28): V40 이후에도 노출 중인 한국 대학 209행 중 9행이 이미 문을 닫았거나
-- 다른 학교로 통합된 학교였다. 9행 모두 2026-11-28 운영 실측에서 회원·리포트·후기·인증·
-- 국제처·일정·신청·마켓 참조가 0 이었다(교환대학 행만 1개씩).
--
--   1. 폐교, 후계 학교 없음 → inactive_reason = 'CLOSED' (V42)
--        서남대(2018-02 폐쇄명령) · 한중대(2018-02 폐쇄명령) · 대구외국어대(2018-02 폐쇄명령)
--        한려대(2022-02 법인 파산 후 폐교) · 한국국제대(2023 파산 선고 후 폐교)
--   2. 통합, 후계 학교 행이 마스터에 있음 → canonical_id (V32)
--        밀양대 → 부산대(2006) · 국립강릉원주대 → 강원대(2026-03-01 통합 출범)
--   3. 통합, 후계 학교 행이 마스터에 없음 → 후계 행을 새로 만들고 canonical_id
--        충주대 → 한국교통대(2012, ut.ac.kr) · 동명정보대 → 동명대(2006, tu.ac.kr)
--      V40 은 이 두 옛 행에 후계 학교 이름을 별칭으로 걸어 대신 쓰게 했다. 그 상태로는
--      현재 재학생 메일(ut.ac.kr·tu.ac.kr)로 학교 인증이 안 되고, 옛 이름이 화면에 나온다.
--      옛 행은 지우지 않고 숨긴다 — 옛 도메인 메일은 findByDomainIgnoreCase 가 대표 행으로 돌린다.
--
-- canonical_id 는 한 단계만 따라간다(검색·도메인 역조회 모두). 그래서 강릉원주대를 숨길 때
-- 강릉원주대를 대표로 삼던 행(강릉대, V40)도 강원대로 다시 잇는다.
-- 행은 도메인으로 식별한다(id 는 환경마다 다를 수 있음). 빈 환경(테스트 DB)에서는 모두 no-op.
-- 되돌리기: 1번 행 inactive_reason = NULL · 2·3번 행 canonical_id = NULL ·
--   kangnung.ac.kr 행 canonical_id = gwnu.ac.kr 행 · ut.ac.kr/tu.ac.kr 행과 그 이름·별칭 삭제.
-- ============================================================================

-- ── 1. 폐교 5개교 ────────────────────────────────────────────────────────────
UPDATE global_universities
SET inactive_reason = 'CLOSED'
WHERE country_code = 'KR'
  AND inactive_reason IS NULL
  AND canonical_id IS NULL
  AND (lower(domain), name_en) IN (
        ('seonam.ac.kr',   'Seonam University'),
        ('hanzhong.ac.kr', 'Hanzhong University'),
        ('dufs.ac.kr',     'Daegu University of Foreign Studies'),
        ('hanlyo.ac.kr',   'Hanlyo University'),
        ('iuk.ac.kr',      'International University of Korea'));

-- ── 2. 후계 학교 행 추가 (옛 행이 있는 환경에서만) ──────────────────────────────
WITH succ(old_domain, name_en, domain, website) AS (VALUES
    ('chungju.ac.kr', 'Korea National University of Transportation', 'ut.ac.kr', 'https://www.ut.ac.kr'),
    ('tit.ac.kr',     'Tongmyong University',                        'tu.ac.kr', 'https://www.tu.ac.kr')
)
INSERT INTO global_universities (name_en, country, country_en, country_code, website, domain)
SELECT s.name_en, o.country, o.country_en, 'KR', s.website, s.domain
FROM succ s
JOIN global_universities o ON lower(o.domain) = s.old_domain AND o.country_code = 'KR'
-- name_en UNIQUE(uq_global_univ_name_en)는 운영에만 있고 마이그레이션에는 없어 ON CONFLICT 대신 NOT EXISTS.
WHERE NOT EXISTS (SELECT 1 FROM global_universities x
                  WHERE lower(x.domain) = s.domain OR x.name_en = s.name_en);

WITH ko(domain, name) AS (VALUES
    ('ut.ac.kr', '국립한국교통대학교'),
    ('tu.ac.kr', '동명대학교')
)
INSERT INTO global_university_names (global_univ_id, lang, name, source)
SELECT g.id, 'ko', ko.name, 'manual'
FROM ko JOIN global_universities g ON lower(g.domain) = ko.domain AND g.country_code = 'KR'
ON CONFLICT (global_univ_id, lang) DO NOTHING;

-- V40 이 옛 행에 붙인 후계 학교 별칭을 후계 행으로 옮긴다.
DELETE FROM global_university_aliases a
USING global_universities o
WHERE a.global_univ_id = o.id AND o.country_code = 'KR' AND a.source = 'manual'
  AND (lower(o.domain), a.alias) IN (
        ('chungju.ac.kr', '한국교통대학교'), ('chungju.ac.kr', '한국교통대'),
        ('chungju.ac.kr', '교통대'), ('chungju.ac.kr', 'Korea National University of Transportation'),
        ('tit.ac.kr', '동명대학교'), ('tit.ac.kr', '동명대'), ('tit.ac.kr', 'Tongmyong University'));

WITH al(domain, alias) AS (VALUES
    ('ut.ac.kr', '한국교통대학교'), ('ut.ac.kr', '한국교통대'), ('ut.ac.kr', '교통대'),
    ('tu.ac.kr', '동명대')
)
INSERT INTO global_university_aliases (global_univ_id, alias, source)
SELECT g.id, al.alias, 'manual'
FROM al JOIN global_universities g ON lower(g.domain) = al.domain AND g.country_code = 'KR'
ON CONFLICT (global_univ_id, alias) DO NOTHING;

-- ── 3. 통합 4개교 → 대표 행 ──────────────────────────────────────────────────
WITH merged(hidden_domain, canonical_domain) AS (VALUES
    ('miryang.ac.kr', 'pusan.ac.kr'),     -- 밀양대 → 부산대(2006)
    ('gwnu.ac.kr',    'kangwon.ac.kr'),   -- 국립강릉원주대 → 강원대(2026-03-01)
    ('chungju.ac.kr', 'ut.ac.kr'),        -- 충주대 → 한국교통대(2012)
    ('tit.ac.kr',     'tu.ac.kr')         -- 동명정보대 → 동명대(2006)
)
UPDATE global_universities h
SET canonical_id = c.id
FROM merged m
JOIN global_universities c ON lower(c.domain) = m.canonical_domain AND c.country_code = 'KR'
                          AND c.canonical_id IS NULL AND c.inactive_reason IS NULL
WHERE lower(h.domain) = m.hidden_domain AND h.country_code = 'KR'
  AND h.canonical_id IS NULL AND h.id <> c.id;

-- 숨긴 행을 대표로 삼던 행(강릉대 → 강릉원주대)을 그 대표의 대표로 다시 잇는다.
UPDATE global_universities h
SET canonical_id = mid.canonical_id
FROM global_universities mid
WHERE h.canonical_id = mid.id
  AND mid.canonical_id IS NOT NULL
  AND h.country_code = 'KR'
  AND lower(mid.domain) IN ('miryang.ac.kr', 'gwnu.ac.kr', 'chungju.ac.kr', 'tit.ac.kr');
