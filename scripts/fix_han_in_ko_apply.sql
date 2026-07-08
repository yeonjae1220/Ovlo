-- ============================================================================
-- ko 리포트에 혼입된 한자(漢字) 고빈도 공통 패턴 교정 (2026-07-08)
-- ----------------------------------------------------------------------------
-- 배경: V28 게이트는 ko 필드에서 한자를 결함 대상에서 제외했음(일본/중국 대학
--       한자 고유명은 정상이라는 전제). 이 전제가 방 종류·일반명사 어휘 단위
--       오염(예: "단人间"="단인실")까지 통과시켜, 728개 중 415개(57%) 리포트에
--       한자 조각이 혼입된 채 방치됐다(사용자가 부경대 346에서 최초 발견).
-- 범위: "고빈도 공통 패턴"(33개, 방종류·일반명사/동사)만 교정.
--       개별 리포트 1~2회 등장 외국 지명 음차 파손(메尔버른 등)과 중국
--       지명/음식/앱명(长春·汤圆·支付宝 등)은 별도 패스로 미룸(개별 검증 필요,
--       GLOBAL-PIT-063).
-- 안전장치: UPDATE 전 백업 테이블. 전부 리터럴 문자열 치환(정규식 미사용).
--          replace()는 패턴 없으면 no-op이라 재실행해도 안전(idempotent).
-- ============================================================================

\pset pager off

DROP TABLE IF EXISTS _bak_han_in_ko_20260708;
CREATE TABLE _bak_han_in_ko_20260708 AS
SELECT report_id, lang, title, summary, body, content
FROM university_report_translation
WHERE lang = 'ko';

CREATE OR REPLACE FUNCTION _fix_han_in_ko(t TEXT) RETURNS TEXT AS $$
    SELECT replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(t, '자동提款기', '현금인출기'), '문화人類학', '문화인류학'), '손工艺品', '수공예품'), '사交', '사교'), '생선脍', '생선회'), '학생寮', '학생 기숙사'), '주택中介', '주택 중개'), '商学院', '경영대학'), '견习', '견습'), '명细서', '명세서'), '土著', '원주민'), '文艺复兴', '르네상스'), '沃尔玛', '월마트'), '소금渍', '소금절임'), '学生簽證', '학생 비자'), '出入境管理局', '출입국관리소'), '剑桥', '케임브리지'), '清华大学', '칭화대학교'), '自炊', '자취'), '자炊', '자취'), '케레姆', '카림'), '케어姆', '카림'), '수饺', '만두'), '장보기的理想적인', '장보기에 이상적인'), '장보기的理想입니다', '장보기에 이상적입니다'), '단人间실', '단인실'), '실人间', '2인실'), '단人间', '단인실'), '이人间', '2인실'), '삼人间', '3인실'), '쌍人间', '2인실'), '장江 강', '양쯔강'), '장江', '양쯔강');
$$ LANGUAGE SQL IMMUTABLE;

UPDATE university_report_translation t
SET
    title   = _fix_han_in_ko(t.title),
    summary = CASE WHEN t.summary IS NULL THEN NULL ELSE _fix_han_in_ko(t.summary) END,
    body    = _fix_han_in_ko(t.body),
    content = _fix_han_in_ko(t.content::text)::jsonb
WHERE t.lang = 'ko';

DROP FUNCTION _fix_han_in_ko(TEXT);

-- ── 결과 확인 ────────────────────────────────────────────────────────────────
SELECT
    count(*) FILTER (WHERE concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '人间') AS remaining_renjian,
    count(*) FILTER (WHERE concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '提款기') AS remaining_atm,
    count(*) FILTER (WHERE concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '寮') AS remaining_dorm,
    count(*) FILTER (WHERE concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '清华大学|剑桥') AS remaining_univ
FROM university_report_translation
WHERE lang = 'ko';
