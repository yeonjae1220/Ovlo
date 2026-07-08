-- ============================================================================
-- ja/zh 리포트 exotic script(키릴/태국/아랍) 혼입 23건 처리 (2026-07-08)
-- ----------------------------------------------------------------------------
-- V29 exotic_in_cjk 게이트는 '어디든 존재하면 결함'인 블런트 체크였음.
-- 23건 실사 결과 대부분은 결함이 아니라 **의도된 이중언어 설명**이었다:
--   - "学生ビザ（учебный виза）" 처럼 일본어/중국어 번역 뒤 현지어 원문 병기
--     (비자 서류에 실제로 필요한 현지어 용어 안내 — 유용한 정보, 결함 아님)
--   - "「ขอบคุณค่ะ」（ありがとう）" 처럼 현지어 기초 회화 학습 섹션(의도된 콘텐츠)
-- 진짜 결함(괄호 등 경계 없이 가나/한자/라틴에 직접 들러붙은 것)만 11건:
--   자기 대학명 오기(레겐스부르크·윈저·로욜라, ko의 부경대 사례와 동일 패턴 —
--     윈저는 제목/본문에서 서로 다른 두 표기(ウインズ/ウィンズ)로 각각 오염돼
--     둘 다 등록 필요했음), 요리명/지명 손상(치리크랩·카페·케방사안·시니강·
--     코라망갈라·차리치노), 자유직업 플랫폼명(Workana, 중국어 리포트에
--     한글 '워크'까지 섞여 있었음).
-- 안전장치: UPDATE 전 백업. 전부 리터럴 문자열 치환(정규식 미사용). 적용 전
-- dry-run으로 엄격 인접 규칙 기준 잔존 0건 확인 후 실행.
-- ============================================================================

\pset pager off

DROP TABLE IF EXISTS _bak_exotic_cjk_20260708;
CREATE TABLE _bak_exotic_cjk_20260708 AS
SELECT report_id, lang, title, summary, body, content
FROM university_report_translation
WHERE lang IN ('ja','zh');

CREATE OR REPLACE FUNCTION _fix_exotic_ja(t TEXT) RETURNS TEXT AS $$
    SELECT replace(replace(replace(replace(replace(replace(replace(replace(replace(t, 'ローカーブوروー', 'ローカルバス'), 'レーゲンスбурク', 'レーゲンスブルク'), 'チリクраб', 'チリクラブ'), 'ウインズور', 'ウィンザー'), 'ウィンズور', 'ウィンザー'), 'ロイولا', 'ロヨラ'), 'ケバڠサナ', 'ケバンサナ'), 'Caффè', 'Caffè'), 'シンイڠ', 'シニガン');
$$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION _fix_exotic_zh(t TEXT) RETURNS TEXT AS $$
    SELECT replace(replace(replace(t, '科拉马ڠ加拉', '科拉曼加拉'), '茨ари欣诺', '察里津诺'), '워크انا', '沃卡纳');
$$ LANGUAGE SQL IMMUTABLE;

UPDATE university_report_translation t
SET
    title   = _fix_exotic_ja(t.title),
    summary = CASE WHEN t.summary IS NULL THEN NULL ELSE _fix_exotic_ja(t.summary) END,
    body    = _fix_exotic_ja(t.body),
    content = _fix_exotic_ja(t.content::text)::jsonb
WHERE t.lang = 'ja';

UPDATE university_report_translation t
SET
    title   = _fix_exotic_zh(t.title),
    summary = CASE WHEN t.summary IS NULL THEN NULL ELSE _fix_exotic_zh(t.summary) END,
    body    = _fix_exotic_zh(t.body),
    content = _fix_exotic_zh(t.content::text)::jsonb
WHERE t.lang = 'zh';

DROP FUNCTION _fix_exotic_ja(TEXT);
DROP FUNCTION _fix_exotic_zh(TEXT);
